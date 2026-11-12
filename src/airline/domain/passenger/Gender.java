package airline.domain.passenger;

/** 승객 성별 (여권 표기 M/F) */
public enum Gender {
    MALE("남성", "M"),
    FEMALE("여성", "F");

    private final String displayName;
    private final String passportCode;

    Gender(String displayName, String passportCode) {
        this.displayName = displayName;
        this.passportCode = passportCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 여권·탑승권 표기 (M 또는 F) */
    public String getPassportCode() {
        return passportCode;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
