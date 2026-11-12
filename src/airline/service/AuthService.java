package airline.service;

import airline.domain.user.PasswordHasher;
import airline.domain.user.User;
import airline.exception.AuthException;
import airline.repository.UserRepository;
import airline.util.AppClock;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 회원가입·로그인·로그아웃.
 * 데스크톱 프로그램이라 한 번에 한 명만 로그인한다고 보고 현재 사용자를 여기서 기억한다.
 */
public class AuthService {

    public static final String USERNAME_RULE = "영문 소문자·숫자 4~16자";
    public static final String PASSWORD_RULE = "영문과 숫자를 모두 포함한 8~20자";

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9]{4,16}");
    private static final Pattern PASSWORD_CHARS = Pattern.compile("[A-Za-z0-9!@#$%^&*._-]{8,20}");
    private static final String LOGIN_FAILED = "아이디 또는 비밀번호가 올바르지 않습니다.";

    private final UserRepository userRepository;
    private final PasswordHasher hasher;
    private final AppClock clock;
    private User currentUser;

    public AuthService(UserRepository userRepository, PasswordHasher hasher, AppClock clock) {
        this.userRepository = userRepository;
        this.hasher = hasher;
        this.clock = clock;
    }

    /** 회원가입 후 바로 로그인 상태가 된다. */
    public User signUp(String username, String password, String passwordConfirm) throws AuthException {
        if (password == null || !password.equals(passwordConfirm)) {
            throw new AuthException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        User user = register(username, password);
        currentUser = user;
        return user;
    }

    /** 회원 등록만 한다 (로그인 상태는 바꾸지 않음). 시연용 계정을 만들 때 사용. */
    public User register(String username, String password) throws AuthException {
        String id = normalize(username);
        if (!USERNAME.matcher(id).matches()) {
            throw new AuthException("아이디는 " + USERNAME_RULE + "로 입력하세요.");
        }
        if (userRepository.exists(id)) {
            throw new AuthException("이미 사용 중인 아이디입니다: " + id);
        }
        checkPasswordRule(password);
        User user = new User(id, hasher.hash(password), clock.now());
        userRepository.save(user);
        return user;
    }

    /**
     * 로그인. 아이디가 없을 때와 비밀번호가 틀렸을 때 같은 메시지를 보여줘서
     * 어떤 아이디가 가입되어 있는지 알아낼 수 없게 한다.
     */
    public User login(String username, String password) throws AuthException {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new AuthException("아이디와 비밀번호를 입력하세요.");
        }
        User user = userRepository.findByUsername(normalize(username))
                .filter(found -> found.checkPassword(password, hasher))
                .orElseThrow(() -> new AuthException(LOGIN_FAILED));
        currentUser = user;
        return user;
    }

    public void logout() {
        currentUser = null;
    }

    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /** 회원가입 화면의 "중복 확인" 버튼용 */
    public boolean isUsernameAvailable(String username) {
        String id = normalize(username);
        return USERNAME.matcher(id).matches() && !userRepository.exists(id);
    }

    private static void checkPasswordRule(String password) throws AuthException {
        if (password == null || !PASSWORD_CHARS.matcher(password).matches()
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
            throw new AuthException("비밀번호는 " + PASSWORD_RULE + "로 입력하세요. (특수문자 !@#$%^&*._- 사용 가능)");
        }
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
