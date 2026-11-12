package airline.domain.seat;

/** 일반석 좌석 */
public class EconomySeat extends Seat {

    public EconomySeat(int row, char column) {
        super(row, column);
    }

    @Override
    public CabinClass getCabin() {
        return CabinClass.ECONOMY;
    }
}
