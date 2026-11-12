package airline.exception;

/** 인원 구성, 이름, 나이, 여권, 보호자 규칙을 어겼을 때 */
public class InvalidPassengerException extends AirlineException {

    public InvalidPassengerException(String message) {
        super(message);
    }

    @Override
    public String getTitle() {
        return "탑승객 정보 확인";
    }
}
