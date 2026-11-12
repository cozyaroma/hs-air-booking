package airline.domain.booking;

/** 여정 종류 */
public enum TripType {
    ONE_WAY("편도"),
    ROUND_TRIP("왕복");

    private final String displayName;

    TripType(String displayName) {
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
