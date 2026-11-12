package airline.payment;

import airline.exception.InvalidPaymentInfoException;
import airline.exception.PaymentFailedException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 신용카드 결제 (모의).
 *
 * 국내 카드 결제에서 실제로 입력하는 값: 카드번호, 유효기간, CVC, 카드 비밀번호 앞 2자리, 생년월일 6자리.
 * 생성자에서 형식을 모두 검사하고, 저장은 가려진 카드번호와 유효기간·할부 정보만 한다.
 * (CVC·비밀번호·생년월일은 필드로 남기지 않는다)
 */
public class CreditCardPayment extends AbstractPayment {

    public static final List<String> CARD_COMPANIES = List.of(
            "신한카드", "KB국민카드", "삼성카드", "현대카드", "롯데카드", "하나카드", "우리카드", "BC카드", "NH농협카드");
    public static final long MIN_INSTALLMENT_AMOUNT = 50_000;
    public static final int MAX_INSTALLMENT_MONTHS = 12;

    private final String cardCompany;
    private final String maskedNumber;
    private final YearMonth expiry;
    private final int installmentMonths;   // 0 = 일시불

    /**
     * @param cardNumber      15~16자리 (띄어쓰기·하이픈 허용)
     * @param expiry          MM/YY
     * @param cvc             카드 뒷면 3자리 (아멕스는 4자리)
     * @param ownerBirth6     카드 소유자 생년월일 6자리 (YYMMDD)
     * @param passwordFirst2  카드 비밀번호 앞 2자리
     * @param installmentMonths 0(일시불) 또는 2~12
     */
    public CreditCardPayment(String cardCompany, String cardNumber, String expiry, String cvc,
                             String ownerBirth6, String passwordFirst2, int installmentMonths)
            throws InvalidPaymentInfoException {
        if (cardCompany == null || cardCompany.isBlank()) {
            throw new InvalidPaymentInfoException("카드사를 선택하세요.");
        }
        String digits = PaymentFormats.requireDigits(cardNumber, 15, 16, "카드번호");
        if (!PaymentFormats.isLuhnValid(digits)) {
            throw new InvalidPaymentInfoException("카드번호가 올바르지 않습니다. 번호를 다시 확인하세요.\n(시연용 테스트 번호: 4111 1111 1111 1111)");
        }
        this.expiry = PaymentFormats.parseExpiry(expiry);
        PaymentFormats.requireDigits(cvc, 3, 4, "CVC");
        String birth = PaymentFormats.requireDigits(ownerBirth6, 6, 6, "생년월일 6자리");
        int month = Integer.parseInt(birth.substring(2, 4));
        int day = Integer.parseInt(birth.substring(4, 6));
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            throw new InvalidPaymentInfoException("생년월일 6자리(YYMMDD)가 올바르지 않습니다.");
        }
        PaymentFormats.requireDigits(passwordFirst2, 2, 2, "카드 비밀번호 앞 2자리");
        if (installmentMonths < 0 || installmentMonths == 1 || installmentMonths > MAX_INSTALLMENT_MONTHS) {
            throw new InvalidPaymentInfoException("할부 개월 수는 일시불 또는 2~12개월입니다.");
        }
        this.cardCompany = cardCompany;
        this.maskedNumber = PaymentFormats.maskCardNumber(digits);
        this.installmentMonths = installmentMonths;
    }

    @Override
    protected void validate(long amount, LocalDateTime now) throws PaymentFailedException {
        if (expiry.isBefore(YearMonth.from(now))) {
            throw new PaymentFailedException("유효기간이 지난 카드입니다. (유효기간 " + expiry + ")");
        }
        if (installmentMonths > 0 && amount < MIN_INSTALLMENT_AMOUNT) {
            throw new PaymentFailedException("5만원 미만은 할부로 결제할 수 없습니다. 일시불을 선택하세요.");
        }
    }

    @Override
    protected String approvalPrefix() {
        return "CC";
    }

    @Override
    public String getDisplayName() {
        String plan = installmentMonths == 0 ? "일시불" : installmentMonths + "개월 할부";
        return "신용카드 (" + cardCompany + ", " + plan + ")";
    }

    @Override
    public String getMaskedInfo() {
        return maskedNumber;
    }

    @Override
    public String getRefundGuide() {
        return "카드 승인 취소 (카드사에 따라 영업일 기준 3~7일 소요)";
    }

    public int getInstallmentMonths() {
        return installmentMonths;
    }
}
