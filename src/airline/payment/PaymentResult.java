package airline.payment;

import java.time.LocalDateTime;

/** 결제 승인 결과 (변경되지 않는 값 객체). 가려진 결제 정보만 담는다. */
public record PaymentResult(String approvalNumber, String methodName, String maskedInfo,
                            long amount, LocalDateTime paidAt) {
}
