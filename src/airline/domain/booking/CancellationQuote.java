package airline.domain.booking;

import java.time.Duration;
import java.util.List;

/**
 * 취소 견적: 구간마다 출발까지 남은 시간이 달라서 수수료를 구간별로 계산한 뒤 더한다.
 * 수수료는 운임에만 매기고, 세금·좌석 요금·수하물 요금은 모두 돌려준다.
 */
public record CancellationQuote(List<SegmentFee> segmentFees, long totalPaid) {

    /** 구간 하나의 수수료 */
    public record SegmentFee(SegmentType segment, String flightNumber, Duration timeBeforeDeparture,
                             double feeRate, long fare, long fee) {

        public int feeRatePercent() {
            return (int) Math.round(feeRate * 100);
        }
    }

    public CancellationQuote {
        segmentFees = List.copyOf(segmentFees);
    }

    public long totalFee() {
        return segmentFees.stream().mapToLong(SegmentFee::fee).sum();
    }

    public long refundAmount() {
        return totalPaid - totalFee();
    }
}
