package airline.domain.seat;

import airline.domain.passenger.Passenger;
import airline.exception.SeatRestrictionException;
import airline.exception.SeatUnavailableException;

import java.time.LocalDate;

/**
 * 항공기 좌석 하나를 나타내는 추상 클래스.
 *
 * [추상성] 모든 좌석이 공유하는 상태(위치, 예약 여부)와 동작(예약, 해제)은 여기서 구현하고,
 *         좌석마다 다른 부분(등급, 추가 요금, 착석 제한)만 하위 클래스가 정한다.
 * [은닉성] 예약 여부(reserved)는 private이며 reserve()/release()로만 바뀐다.
 *         밖에서 직접 바꿀 수 없으므로 "이미 예약된 좌석을 또 예약"하는 일이 생기지 않는다.
 */
public abstract class Seat {

    private final int row;
    private final char column;
    private boolean reserved;

    protected Seat(int row, char column) {
        if (row < 1) {
            throw new IllegalArgumentException("좌석 열 번호는 1 이상이어야 합니다: " + row);
        }
        if (column < 'A' || column > 'F') {
            throw new IllegalArgumentException("좌석 위치는 A~F 중 하나여야 합니다: " + column);
        }
        this.row = row;
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public char getColumn() {
        return column;
    }

    /** 예: "12A" */
    public String getCode() {
        return row + String.valueOf(column);
    }

    public boolean isAvailable() {
        return !reserved;
    }

    public void reserve() throws SeatUnavailableException {
        if (reserved) {
            throw new SeatUnavailableException(getCode() + " 좌석은 이미 예약되었습니다. 다른 좌석을 선택하세요.");
        }
        reserved = true;
    }

    public void release() {
        reserved = false;
    }

    /** 이 좌석이 속한 등급 */
    public abstract CabinClass getCabin();

    /** 좌석 지정에 드는 추가 요금. 기본은 0원이고, 유료 좌석이 오버라이딩한다. */
    public long getExtraFee() {
        return 0;
    }

    /** 화면에 표시할 좌석 설명 */
    public String getDescription() {
        return getCabin().getDisplayName();
    }

    /**
     * 이 승객이 이 좌석에 앉을 수 있는지 검사한다.
     * 기본 규칙은 "좌석을 점유하는 승객이면 누구나 가능"이고,
     * 비상구석처럼 조건이 더 있는 좌석은 오버라이딩해서 규칙을 추가한다.
     */
    public void checkOccupant(Passenger passenger, LocalDate travelDate) throws SeatRestrictionException {
        if (!passenger.occupiesSeat()) {
            throw new SeatRestrictionException(
                    passenger.getType().getDisplayName() + " 승객은 좌석을 배정받지 않고 보호자와 함께 앉습니다.");
        }
    }

    @Override
    public String toString() {
        return getCode();
    }
}
