package airline.domain.passenger;

/** 승객 유형. 검색 화면의 인원 선택과 승객 정보 입력 화면에서 사용한다. */
public enum PassengerType {
    ADULT("성인", "국내선 만 13세 이상 / 국제선 만 12세 이상"),
    CHILD("소아", "만 2세 이상 ~ 성인 기준 미만 (운임 75%)"),
    INFANT("유아", "만 2세 미만, 좌석 없이 보호자와 탑승");

    private final String displayName;
    private final String ageGuide;

    PassengerType(String displayName, String ageGuide) {
        this.displayName = displayName;
        this.ageGuide = ageGuide;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 인원 선택 옆에 보여줄 나이 안내 */
    public String getAgeGuide() {
        return ageGuide;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
