package airline.exception;

/** 예약 마감 시간이 지난 항공권을 예약하려는 경우 */
public class BookingClosedException extends AirlineException {

    public BookingClosedException(String message) {
        super(message);
    }

    @Override
    public String getTitle() {
        return "예약 마감";
    }
}
