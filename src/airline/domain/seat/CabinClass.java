package airline.domain.seat;

/** 좌석 등급. 등급별 운임 배율을 함께 가진다. */
public enum CabinClass {
    ECONOMY("일반석", 1.0),
    BUSINESS("비즈니스석", 2.6);

    private final String displayName;
    private final double fareMultiplier;

    CabinClass(String displayName, double fareMultiplier) {
        this.displayName = displayName;
        this.fareMultiplier = fareMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 일반석 성인 운임 대비 배율 */
    public double getFareMultiplier() {
        return fareMultiplier;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
