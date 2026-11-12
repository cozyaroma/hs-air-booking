package airline.exception;

/** 좌석은 비어 있지만 이 승객이 앉을 수 없는 경우 (비상구석 나이 제한, 다른 등급 좌석 등) */
public class SeatRestrictionException extends AirlineException {

    public SeatRestrictionException(String message) {
        super(message);
    }

    @Override
    public String getTitle() {
        return "좌석 이용 제한";
    }
}
