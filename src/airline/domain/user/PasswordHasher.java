package airline.domain.user;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 비밀번호를 그대로 저장하지 않고 "소금(salt) + 해시"로 바꿔 저장하는 도구.
 *
 * 같은 비밀번호라도 사용자마다 salt가 달라서 저장된 값이 다르게 나온다.
 * 자바 기본 라이브러리의 PBKDF2(HmacSHA256)를 사용하므로 외부 라이브러리가 필요 없다.
 * 저장소가 유출되어도 원래 비밀번호를 바로 알 수 없게 하는 것이 목적이다.
 */
public class PasswordHasher {

    private static final int ITERATIONS = 10_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;

    private final SecureRandom random = new SecureRandom();

    /** 저장용 값: salt와 해시 (둘 다 Base64 문자열) */
    public record HashedPassword(String salt, String hash) {
    }

    public HashedPassword hash(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return new HashedPassword(encode(salt), encode(derive(rawPassword, salt)));
    }

    public boolean matches(String rawPassword, HashedPassword stored) {
        if (rawPassword == null || stored == null) {
            return false;
        }
        byte[] expected = Base64.getDecoder().decode(stored.hash());
        byte[] actual = derive(rawPassword, Base64.getDecoder().decode(stored.salt()));
        return MessageDigest.isEqual(expected, actual);   // 비교 시간이 일정한 비교
    }

    private static byte[] derive(String rawPassword, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, ITERATIONS, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("비밀번호 암호화 기능을 사용할 수 없습니다.", e);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
