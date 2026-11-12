package airline.domain.booking;

/**
 * 예약 전체의 상태와 허용되는 상태 변화.
 * 예약 확정 → 취소만 가능하다. 체크인은 구간(BookingSegment)마다 따로 기록한다.
 */
public enum BookingStatus {
    CONFIRMED("예약 확정"),
    CANCELLED("취소됨");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public boolean canTransitionTo(BookingStatus next) {
        return this == CONFIRMED && next == CANCELLED;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
