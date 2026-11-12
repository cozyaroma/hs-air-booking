package airline.domain.flight;

import airline.domain.seat.BusinessSeat;
import airline.domain.seat.EconomySeat;
import airline.domain.seat.ExitRowSeat;
import airline.domain.seat.Seat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 기종별 좌석 배치.
 * 비즈니스석은 2-2 배열(A, C, D, F), 일반석은 3-3 배열(A~F)이다.
 */
public enum AircraftType {
    A321NEO("에어버스 A321neo", 2, 16, Set.of(10)),
    B737_8("보잉 737-8", 2, 18, Set.of(11));

    public static final char[] ALL_COLUMNS = {'A', 'B', 'C', 'D', 'E', 'F'};
    private static final char[] BUSINESS_COLUMNS = {'A', 'C', 'D', 'F'};

    private final String displayName;
    private final int businessRows;
    private final int economyRows;
    private final Set<Integer> exitRows;

    AircraftType(String displayName, int businessRows, int economyRows, Set<Integer> exitRows) {
        this.displayName = displayName;
        this.businessRows = businessRows;
        this.economyRows = economyRows;
        this.exitRows = exitRows;
    }

    /** 이 기종의 빈 좌석 배치도를 새로 만든다. 운항편마다 별도의 좌석 객체를 가져야 한다. */
    public Map<String, Seat> createSeatMap() {
        Map<String, Seat> seats = new LinkedHashMap<>();
        int row = 1;
        for (; row <= businessRows; row++) {
            for (char column : BUSINESS_COLUMNS) {
                add(seats, new BusinessSeat(row, column));
            }
        }
        int lastRow = businessRows + economyRows;
        for (; row <= lastRow; row++) {
            for (char column : ALL_COLUMNS) {
                add(seats, exitRows.contains(row) ? new ExitRowSeat(row, column) : new EconomySeat(row, column));
            }
        }
        return seats;
    }

    private static void add(Map<String, Seat> seats, Seat seat) {
        seats.put(seat.getCode(), seat);
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTotalRows() {
        return businessRows + economyRows;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
