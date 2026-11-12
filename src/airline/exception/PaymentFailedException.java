package airline.exception;

/** 결제 수단의 규칙(할부 최소 금액, 간편결제 한도 등)에 걸린 경우 */
public class PaymentFailedException extends AirlineException {

    public PaymentFailedException(String message) {
        super(message);
    }

    @Override
    public String getTitle() {
        return "결제 실패";
    }
}
