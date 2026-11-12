package airline.payment;

import airline.exception.InvalidPaymentInfoException;
import airline.exception.PaymentFailedException;

import java.time.LocalDateTime;
import java.util.List;

/** 간편결제 (모의). 별도 입력 없이 승인되며 1회 결제 한도가 있다 (과제용 예시 값). */
public class SimplePayment extends AbstractPayment {

    public static final List<String> PROVIDERS = List.of("카카오페이", "네이버페이", "토스페이");
    public static final long LIMIT_PER_PAYMENT = 2_000_000;

    private final String provider;

    public SimplePayment(String provider) throws InvalidPaymentInfoException {
        if (provider == null || provider.isBlank()) {
            throw new InvalidPaymentInfoException("간편결제 서비스를 선택하세요.");
        }
        this.provider = provider;
    }

    @Override
    protected void validate(long amount, LocalDateTime now) throws PaymentFailedException {
        if (amount > LIMIT_PER_PAYMENT) {
            throw new PaymentFailedException(String.format(
                    "간편결제 1회 한도(%,d원)를 초과했습니다. 신용카드나 계좌이체를 이용하세요.", LIMIT_PER_PAYMENT));
        }
    }

    @Override
    protected String approvalPrefix() {
        return "SP";
    }

    @Override
    public String getDisplayName() {
        return "간편결제 (" + provider + ")";
    }

    @Override
    public String getMaskedInfo() {
        return provider + " 등록 결제수단";
    }

    @Override
    public String getRefundGuide() {
        return provider + " 결제 수단으로 즉시 환불";
    }
}
