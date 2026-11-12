package airline.domain.passenger;

import airline.domain.flight.Flight;
import airline.exception.InvalidPassengerException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

/**
 * 탑승객 추상 클래스. 성인·소아·유아가 이 클래스를 상속한다.
 *
 * [추상성] 운임 비율, 나이 규칙, 좌석 점유 여부처럼 유형마다 다른 동작은 하위 클래스가 정한다.
 * [은닉성] 모든 필드는 final이고 생성자에서 검증한다. 잘못된 이름이나 생년월일을 가진
 *         승객 객체는 아예 만들어질 수 없다.
 * [다형성] FareCalculator는 승객 유형을 if문으로 구분하지 않고 getFareRate()만 호출한다.
 */
public abstract class Passenger {

    /** 여권 영문 이름 형식: 대문자 알파벳, 단어 사이 공백 허용 */
    private static final Pattern ENGLISH_NAME = Pattern.compile("[A-Z]+( [A-Z]+)*");
    /** 한국 여권번호 형식: 영문 1자리 + 숫자 3자리 + (영문 또는 숫자) 1자리 + 숫자 4자리. 예) M12345678, M123A4567 */
    private static final Pattern PASSPORT = Pattern.compile("[A-Z][0-9]{3}[A-Z0-9][0-9]{4}");

    private final String lastName;
    private final String firstName;
    private final Gender gender;
    private final LocalDate birthDate;
    private final String passportNumber;   // 국내선은 없어도 된다

    protected Passenger(String lastName, String firstName, Gender gender, LocalDate birthDate, String passportNumber)
            throws InvalidPassengerException {
        this.lastName = normalizeName(lastName, "영문 성");
        this.firstName = normalizeName(firstName, "영문 이름");
        if (gender == null) {
            throw new InvalidPassengerException("성별을 선택하세요. (" + this.lastName + "/" + this.firstName + ")");
        }
        this.gender = gender;
        if (birthDate == null) {
            throw new InvalidPassengerException("생년월일을 입력하세요.");
        }
        this.birthDate = birthDate;
        this.passportNumber = (passportNumber == null || passportNumber.isBlank())
                ? null
                : passportNumber.trim().toUpperCase();
    }

    private static String normalizeName(String raw, String label) throws InvalidPassengerException {
        if (raw == null || raw.isBlank()) {
            throw new InvalidPassengerException(label + "을(를) 입력하세요.");
        }
        String value = raw.trim().toUpperCase().replaceAll("\\s+", " ");
        if (!ENGLISH_NAME.matcher(value).matches()) {
            throw new InvalidPassengerException(label + "은(는) 여권과 같은 영문 알파벳으로 입력하세요: " + raw);
        }
        return value;
    }

    // ===== 하위 클래스가 반드시 구현해야 하는 규칙 =====

    public abstract PassengerType getType();

    /** 성인 운임 대비 비율 (성인 1.0, 소아 0.75, 유아는 노선에 따라 다름) */
    public abstract double getFareRate(Flight flight);

    /** 탑승일 기준 나이가 이 유형에 맞는지 검사한다. 국내선과 국제선은 나이 기준이 다르다. */
    public abstract void validateAge(Flight flight) throws InvalidPassengerException;

    // ===== 기본 동작 (필요한 하위 클래스만 오버라이딩) =====

    /** 좌석을 하나 차지하는지. 유아는 보호자 무릎에 앉으므로 false로 오버라이딩한다. */
    public boolean occupiesSeat() {
        return true;
    }

    /** 공항이용료·유류할증료를 내는지 */
    public boolean paysTax() {
        return true;
    }

    /** 유아를 동반하는지 (비상구석 제한에 사용). 성인만 오버라이딩한다. */
    public boolean isAccompanyingInfant() {
        return false;
    }

    /** 여권번호 형식 검사 (화면에서 입력 즉시 확인할 때도 사용 가능) */
    public static boolean isValidPassportFormat(String value) {
        return value != null && PASSPORT.matcher(value.trim().toUpperCase()).matches();
    }

    /** 국제선이면 여권번호가 있어야 하고 형식이 맞아야 한다. */
    public void validateDocuments(Flight flight) throws InvalidPassengerException {
        if (!flight.requiresPassport()) {
            return;
        }
        if (passportNumber == null) {
            throw new InvalidPassengerException(getFullName() + " 승객의 여권번호를 입력하세요. (국제선은 여권 정보가 필요합니다)");
        }
        if (!PASSPORT.matcher(passportNumber).matches()) {
            throw new InvalidPassengerException(
                    "여권번호 형식이 올바르지 않습니다: " + passportNumber + " (예: M12345678 또는 M123A4567)");
        }
    }

    // ===== 공통 기능 =====

    /** 그 날짜 기준 만 나이 */
    public int ageOn(LocalDate date) {
        return AgeCalculator.ageOn(birthDate, date);
    }

    public long daysOldOn(LocalDate date) {
        return ChronoUnit.DAYS.between(birthDate, date);
    }

    /** 항공권에 찍히는 형식: 성/이름 */
    public String getFullName() {
        return lastName + "/" + firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    /** 나이 오류 메시지를 만드는 공통 도우미 */
    protected InvalidPassengerException ageMismatch(Flight flight, int age, String rule) {
        return new InvalidPassengerException(String.format(
                "%s 승객은 탑승일(%s) 기준 만 %d세입니다. %s %s 기준은 %s입니다.",
                getFullName(), flight.getDepartureDate(), age,
                flight.getTypeName(), getType().getDisplayName(), rule));
    }

    @Override
    public String toString() {
        return getType().getDisplayName() + " " + getFullName();
    }
}
