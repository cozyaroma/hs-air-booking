package airline.domain.flight;

/** 취항 공항. 국내 공항끼리의 노선은 국내선, 그 외는 국제선이 된다. */
public enum Airport {
    GMP("서울/김포", true),
    ICN("서울/인천", true),
    CJU("제주", true),
    PUS("부산/김해", true),
    NRT("도쿄/나리타", false),
    BKK("방콕/수완나품", false);

    private final String cityName;
    private final boolean inKorea;

    Airport(String cityName, boolean inKorea) {
        this.cityName = cityName;
        this.inKorea = inKorea;
    }

    public String getCode() {
        return name();
    }

    public String getCityName() {
        return cityName;
    }

    public boolean isInKorea() {
        return inKorea;
    }

    @Override
    public String toString() {
        return cityName + " (" + name() + ")";
    }
}
