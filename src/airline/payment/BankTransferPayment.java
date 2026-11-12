package airline.payment;

import airline.exception.InvalidPaymentInfoException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 실시간 계좌이체 (모의). 은행, 계좌번호, 예금주를 받는다.
 * 계좌번호는 가려진 값만 저장한다.
 */
public class BankTransferPayment extends AbstractPayment {

    public static final List<String> BANKS = List.of(
            "KB국민은행", "신한은행", "우리은행", "하나은행", "NH농협은행", "IBK기업은행", "카카오뱅크", "토스뱅크");

    private final String bankName;
    private final String maskedAccount;
    private final String holderName;

    /**
     * @param accountNumber 10~14자리 (하이픈 허용)
     * @param holderName    예금주 이름 (2~20자)
     */
    public BankTransferPayment(String bankName, String accountNumber, String holderName)
            throws InvalidPaymentInfoException {
        if (bankName == null || bankName.isBlank()) {
            throw new InvalidPaymentInfoException("은행을 선택하세요.");
        }
        String digits = PaymentFormats.requireDigits(accountNumber, 10, 14, "계좌번호");
        if (holderName == null || holderName.isBlank()) {
            throw new InvalidPaymentInfoException("예금주 이름을 입력하세요.");
        }
        String holder = holderName.trim();
        if (holder.length() < 2 || holder.length() > 20) {
            throw new InvalidPaymentInfoException("예금주 이름은 2~20자로 입력하세요.");
        }
        this.bankName = bankName;
        this.maskedAccount = PaymentFormats.maskAccountNumber(digits);
        this.holderName = holder;
    }

    @Override
    protected void validate(long amount, LocalDateTime now) {
        // 계좌이체는 추가 제한 없음
    }

    @Override
    protected String approvalPrefix() {
        return "BT";
    }

    @Override
    public String getDisplayName() {
        return "실시간 계좌이체 (" + bankName + ")";
    }

    @Override
    public String getMaskedInfo() {
        return bankName + " " + maskedAccount + " (예금주 " + holderName + ")";
    }

    @Override
    public String getRefundGuide() {
        return "결제한 " + bankName + " 계좌로 환불 (영업일 기준 1~3일 소요)";
    }
}
