package airline.payment;

import airline.exception.PaymentFailedException;

import java.time.LocalDateTime;

/**
 * 결제 수단. 실제 결제는 하지 않는 모의 결제다.
 * BookingService는 이 인터페이스에만 의존하므로 결제 수단이 늘어나도 서비스 코드는 그대로다.
 *
 * 보안 원칙: CVC·카드 비밀번호 같은 값은 객체를 만들 때 형식만 확인하고 필드로 저장하지 않는다.
 *          카드번호·계좌번호는 가려진(마스킹) 값만 보관한다.
 */
public interface PaymentMethod {

    /** 화면·영수증 표시용 이름. 예) "신용카드 (신한카드, 일시불)" */
    String getDisplayName();

    /** 가려진 결제 정보. 예) "**** **** **** 1111", "국민은행 **********4567" */
    String getMaskedInfo();

    PaymentResult pay(long amount, LocalDateTime now) throws PaymentFailedException;

    /** 취소 시 환불 방법 안내 */
    String getRefundGuide();
}
