package airline.payment;

import airline.exception.PaymentFailedException;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 결제 절차의 공통 틀 (템플릿 메서드 패턴).
 * "금액 확인 → 수단별 검사 → 승인번호 발급" 순서는 고정하고(final),
 * 수단별 검사(validate)와 승인번호 접두어만 하위 클래스가 정한다.
 */
public abstract class AbstractPayment implements PaymentMethod {

    @Override
    public final PaymentResult pay(long amount, LocalDateTime now) throws PaymentFailedException {
        if (amount <= 0) {
            throw new PaymentFailedException("결제 금액이 올바르지 않습니다: " + amount);
        }
        validate(amount, now);
        String approval = approvalPrefix() + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        return new PaymentResult(approval, getDisplayName(), getMaskedInfo(), amount, now);
    }

    /** 결제 시점의 수단별 검사 (한도, 할부 최소 금액, 카드 유효기간 등) */
    protected abstract void validate(long amount, LocalDateTime now) throws PaymentFailedException;

    /** 승인번호 앞 두 글자 (CC: 카드, BT: 계좌이체, SP: 간편결제) */
    protected abstract String approvalPrefix();
}
