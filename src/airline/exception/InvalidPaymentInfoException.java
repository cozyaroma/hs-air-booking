package airline.exception;

/** 결제 정보 형식이 잘못된 경우 (카드번호, 유효기간, CVC, 계좌번호 등) */
public class InvalidPaymentInfoException extends AirlineException {

    public InvalidPaymentInfoException(String message) {
        super(message);
    }

    @Override
    public String getTitle() {
        return "결제 정보 확인";
    }
}
