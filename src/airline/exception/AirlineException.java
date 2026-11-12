package airline.exception;

/**
 * 프로그램의 모든 업무 규칙 위반(로그인, 검색, 승객, 좌석, 결제, 예약)의 부모 예외.
 *
 * 체크 예외(Exception 상속)로 만들어 서비스 메서드를 호출하는 쪽이 반드시 처리하도록 했다.
 * 화면은 AirlineException 하나만 잡아도 모든 하위 예외를 처리할 수 있고(다형성),
 * 각 하위 예외가 getTitle()을 오버라이딩해서 알림창 제목이 자동으로 달라진다.
 */
public class AirlineException extends Exception {

    public AirlineException(String message) {
        super(message);
    }

    /** 오류 알림창에 표시할 제목 */
    public String getTitle() {
        return "확인 필요";
    }
}
