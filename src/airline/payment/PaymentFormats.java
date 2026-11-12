package airline.payment;

import airline.exception.InvalidPaymentInfoException;

import java.time.YearMonth;
import java.util.List;

/**
 * 결제 정보 입력값을 다루는 공통 도구 (형식 검사, 마스킹, 보기 좋은 표시).
 * 결제 화면에서 입력 중에 미리 검사할 때도, 결제 클래스가 최종 검사할 때도 같은 규칙을 쓴다.
 *
 * 이 프로그램의 결제는 모두 모의 결제다. 실제 카드 정보를 넣지 않도록 화면에 안내하고,
 * 시연에는 아래 테스트 번호를 쓴다 (형식 검사만 통과하는 번호이며 실제 카드가 아니다).
 */
public final class PaymentFormats {

    /** 시연용 테스트 카드번호 (Luhn 검사를 통과하는 공개 테스트 번호) */
    public static final List<String> TEST_CARD_NUMBERS = List.of("4111111111111111", "5555555555554444");

    private PaymentFormats() {
    }

    /** 숫자만 남긴다. 예) "4111-1111 1111-1111" → "4111111111111111" */
    public static String digitsOnly(String text) {
        return text == null ? "" : text.replaceAll("[^0-9]", "");
    }

    /**
     * 카드번호 체크섬(Luhn 알고리즘) 검사. 실제 카드번호도 이 규칙을 따르므로
     * 숫자를 하나 잘못 쳤을 때 대부분 잡아낼 수 있다.
     */
    public static boolean isLuhnValid(String digits) {
        if (digits == null || digits.isEmpty() || !digits.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (doubleIt) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    /** 4자리씩 띄어 쓴다. 예) "4111 1111 1111 1111" */
    public static String formatCardNumber(String digits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append(' ');
            }
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    /** 뒤 4자리만 보여준다. 예) "**** **** **** 1111" */
    public static String maskCardNumber(String digits) {
        if (digits.length() < 4) {
            return "****";
        }
        String masked = "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
        return formatCardNumber(masked);
    }

    /** 뒤 4자리만 보여준다. 예) "**********4567" */
    public static String maskAccountNumber(String digits) {
        if (digits.length() < 4) {
            return "****";
        }
        return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
    }

    /**
     * 카드 유효기간 "MM/YY" (또는 "MMYY")를 연월로 바꾼다.
     * @throws InvalidPaymentInfoException 형식이 틀리거나 월이 01~12가 아닐 때
     */
    public static YearMonth parseExpiry(String text) throws InvalidPaymentInfoException {
        String digits = digitsOnly(text);
        if (digits.length() != 4) {
            throw new InvalidPaymentInfoException("유효기간은 MM/YY 형식으로 입력하세요. (예: 08/29)");
        }
        int month = Integer.parseInt(digits.substring(0, 2));
        int year = 2000 + Integer.parseInt(digits.substring(2, 4));
        if (month < 1 || month > 12) {
            throw new InvalidPaymentInfoException("유효기간의 월은 01~12 사이여야 합니다.");
        }
        return YearMonth.of(year, month);
    }

    /** 숫자 개수가 min~max인지 검사하고, 숫자만 남긴 값을 돌려준다. */
    static String requireDigits(String text, int min, int max, String label) throws InvalidPaymentInfoException {
        String digits = digitsOnly(text);
        if (text == null || text.isBlank()) {
            throw new InvalidPaymentInfoException(label + "을(를) 입력하세요.");
        }
        if (!text.replaceAll("[\\s-]", "").chars().allMatch(Character::isDigit)) {
            throw new InvalidPaymentInfoException(label + "에는 숫자만 입력하세요.");
        }
        if (digits.length() < min || digits.length() > max) {
            String range = (min == max) ? min + "자리" : min + "~" + max + "자리";
            throw new InvalidPaymentInfoException(label + "은(는) " + range + " 숫자입니다.");
        }
        return digits;
    }
}
