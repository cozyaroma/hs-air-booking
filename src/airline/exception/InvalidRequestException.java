package airline.exception;

/** 검색 조건이나 예약 요청이 올바르지 않을 때 (같은 출발·도착지, 좌석 미선택, 수하물 수량 초과 등) */
public class InvalidRequestException extends AirlineException {

    public InvalidRequestException(String message) {
        super(message);
    }

    @Override
    public String getTitle() {
        return "입력 확인";
    }
}
