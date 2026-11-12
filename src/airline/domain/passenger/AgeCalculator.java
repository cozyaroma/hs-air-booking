package airline.domain.passenger;

import airline.domain.flight.DomesticFlight;
import airline.domain.flight.InternationalFlight;
import airline.exception.InvalidPassengerException;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * 만 나이 계산기 (화면의 "나이 계산기"와 승객 검증이 같은 규칙을 쓰도록 한 곳에 모았다).
 *
 * 만 나이 = 기준일에 생일이 지났으면 (연도 차이), 안 지났으면 (연도 차이 - 1)
 * 항공권은 "탑승일 기준" 만 나이로 성인/소아/유아를 나눈다.
 *   국내선: 성인 만 13세 이상, 소아 만 2세~12세, 유아 만 2세 미만
 *   국제선: 성인 만 12세 이상, 소아 만 2세~11세, 유아 만 2세 미만
 */
public final class AgeCalculator {

    private AgeCalculator() {
    }

    /** 계산 결과: 만 나이와 그 나이에 맞는 승객 유형 */
    public record AgeResult(LocalDate birthDate, LocalDate baseDate, int age, long daysOld,
                            PassengerType domesticType, PassengerType internationalType) {

        /** 노선 종류에 맞는 승객 유형 */
        public PassengerType typeFor(boolean domestic) {
            return domestic ? domesticType : internationalType;
        }

        /** 유아인데 생후 7일이 안 되어 탑승할 수 없는 경우 */
        public boolean isTooYoungToFly() {
            return daysOld < InfantPassenger.MIN_DAYS_OLD;
        }

        /** 예) "탑승일 2026-11-20 기준 만 12세 → 국내선 소아 / 국제선 성인" */
        public String describe() {
            String text = String.format("%s 기준 만 %d세 → 국내선 %s / 국제선 %s",
                    baseDate, age, domesticType.getDisplayName(), internationalType.getDisplayName());
            if (isTooYoungToFly()) {
                text += " (생후 " + InfantPassenger.MIN_DAYS_OLD + "일 이후부터 탑승 가능)";
            }
            return text;
        }
    }

    /** 기준일의 만 나이 */
    public static int ageOn(LocalDate birthDate, LocalDate baseDate) {
        return Period.between(birthDate, baseDate).getYears();
    }

    /**
     * 생년월일 8자리(예: 19950315, 1995-03-15, 1995.03.15)를 날짜로 바꾼다.
     * 숫자 외 문자는 무시하고, 숫자가 정확히 8개여야 한다.
     */
    public static LocalDate parseBirthDate(String text) throws InvalidPassengerException {
        String digits = text == null ? "" : text.replaceAll("[^0-9]", "");
        if (digits.length() != 8) {
            throw new InvalidPassengerException("생년월일은 8자리 숫자로 입력하세요. (예: 19950315)");
        }
        try {
            return LocalDate.of(Integer.parseInt(digits.substring(0, 4)),
                    Integer.parseInt(digits.substring(4, 6)),
                    Integer.parseInt(digits.substring(6, 8)));
        } catch (DateTimeException e) {
            throw new InvalidPassengerException("존재하지 않는 날짜입니다: " + text);
        }
    }

    /**
     * 생년월일과 기준일(보통 탑승일)로 만 나이와 승객 유형을 계산한다.
     * @throws InvalidPassengerException 생년월일이 기준일보다 뒤인 경우
     */
    public static AgeResult calculate(LocalDate birthDate, LocalDate baseDate) throws InvalidPassengerException {
        if (birthDate.isAfter(baseDate)) {
            throw new InvalidPassengerException("생년월일이 기준일(" + baseDate + ")보다 늦습니다.");
        }
        int age = ageOn(birthDate, baseDate);
        long days = ChronoUnit.DAYS.between(birthDate, baseDate);
        return new AgeResult(birthDate, baseDate, age, days,
                classify(age, DomesticFlight.ADULT_MIN_AGE),
                classify(age, InternationalFlight.ADULT_MIN_AGE));
    }

    /** 8자리 문자열을 바로 계산 (계산기 화면용) */
    public static AgeResult calculate(String birthDigits, LocalDate baseDate) throws InvalidPassengerException {
        return calculate(parseBirthDate(birthDigits), baseDate);
    }

    /** 입력 중에 미리보기로 쓰기 좋은 버전: 형식이 틀리면 빈 값을 돌려준다 */
    public static Optional<AgeResult> tryCalculate(String birthDigits, LocalDate baseDate) {
        try {
            return Optional.of(calculate(birthDigits, baseDate));
        } catch (InvalidPassengerException e) {
            return Optional.empty();
        }
    }

    private static PassengerType classify(int age, int adultMinAge) {
        if (age >= adultMinAge) {
            return PassengerType.ADULT;
        }
        if (age >= ChildPassenger.MIN_AGE) {
            return PassengerType.CHILD;
        }
        return PassengerType.INFANT;
    }
}
