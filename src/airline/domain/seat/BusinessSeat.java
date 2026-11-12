package airline.domain.seat;

/** 비즈니스석 좌석 */
public class BusinessSeat extends Seat {

    public BusinessSeat(int row, char column) {
        super(row, column);
    }

    @Override
    public CabinClass getCabin() {
        return CabinClass.BUSINESS;
    }

    @Override
    public String getDescription() {
        return "비즈니스석 (2-2 배열)";
    }
}
