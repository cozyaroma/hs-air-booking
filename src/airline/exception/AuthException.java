package airline.exception;

/** 회원가입·로그인 규칙 위반 (아이디 형식, 비밀번호 불일치, 중복 아이디 등) */
public class AuthException extends AirlineException {

    public AuthException(String message) {
        super(message);
    }

    @Override
    public String getTitle() {
        return "로그인 확인";
    }
}
