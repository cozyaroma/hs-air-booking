package airline.domain.booking;

/**
 * 여정의 구간. 편도는 가는 편만, 왕복은 가는 편과 오는 편이 있다.
 * 좌석·수하물·체크인은 구간마다 따로 관리한다.
 */
public enum SegmentType {
    OUTBOUND("가는 편"),
    INBOUND("오는 편");

    private final String displayName;

    SegmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
