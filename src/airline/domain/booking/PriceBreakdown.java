package airline.domain.booking;

/**
 * 요금 내역 (변경되지 않는 값 객체).
 * 운임만 취소 수수료 계산 대상이라 항목을 나눠서 보관한다.
 */
public record PriceBreakdown(long fare, long tax, long seatFee, long baggageFee) {

    public static final PriceBreakdown ZERO = new PriceBreakdown(0, 0, 0, 0);

    public PriceBreakdown {
        if (fare < 0 || tax < 0 || seatFee < 0 || baggageFee < 0) {
            throw new IllegalArgumentException("요금 항목은 음수일 수 없습니다.");
        }
    }

    public long total() {
        return fare + tax + seatFee + baggageFee;
    }

    public PriceBreakdown plus(PriceBreakdown other) {
        return new PriceBreakdown(fare + other.fare, tax + other.tax,
                seatFee + other.seatFee, baggageFee + other.baggageFee);
    }
}
