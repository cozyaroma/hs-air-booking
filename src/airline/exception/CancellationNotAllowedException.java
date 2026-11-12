package airline.exception;

/** 이미 취소됐거나 체크인을 마쳤거나 출발한 예약을 취소하려는 경우 */
public class CancellationNotAllowedException extends AirlineException {

    public CancellationNotAllowedException(String message) {
        super(message);
    }

    @Override
    public String getTitle() {
        return "취소 불가";
    }
}
