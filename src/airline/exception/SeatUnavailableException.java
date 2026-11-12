package airline.exception;

/** 이미 예약된 좌석을 고르거나 한 좌석을 두 명이 고른 경우 */
public class SeatUnavailableException extends AirlineException {

    public SeatUnavailableException(String message) {
        super(message);
    }

    @Override
    public String getTitle() {
        return "좌석 선택 불가";
    }
}
