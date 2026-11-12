package airline.domain.user;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 회원 한 명.
 *
 * [은닉성] 비밀번호 원문은 어디에도 저장하지 않는다. 해시만 private으로 가지고 있고,
 *         밖에서는 checkPassword()로 "맞는지"만 물어볼 수 있다.
 */
public final class User {

    private final String username;
    private final PasswordHasher.HashedPassword password;
    private final LocalDateTime joinedAt;

    public User(String username, PasswordHasher.HashedPassword password, LocalDateTime joinedAt) {
        this.username = Objects.requireNonNull(username);
        this.password = Objects.requireNonNull(password);
        this.joinedAt = Objects.requireNonNull(joinedAt);
    }

    public boolean checkPassword(String rawPassword, PasswordHasher hasher) {
        return hasher.matches(rawPassword, password);
    }

    /** 로그인 아이디 (소문자로 저장) */
    public String getUsername() {
        return username;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof User other && username.equals(other.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public String toString() {
        return username;
    }
}
