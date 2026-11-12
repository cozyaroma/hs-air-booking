package airline.exception;

/** 체크인 가능 시간이 아니거나 이미 체크인한 경우 */
public class CheckInNotAvailableException extends AirlineException {

    public CheckInNotAvailableException(String message) {
        super(message);
    }

    @Override
    public String getTitle() {
        return "체크인 불가";
    }
}
