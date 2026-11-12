package airline.exception;

/** 예약번호와 성이 일치하는 예약이 없을 때 */
public class BookingNotFoundException extends AirlineException {

    public BookingNotFoundException(String message) {
        super(message);
    }

    @Override
    public String getTitle() {
        return "예약 조회 실패";
    }
}
