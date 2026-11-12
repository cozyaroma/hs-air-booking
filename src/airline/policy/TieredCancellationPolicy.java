package airline.policy;

import java.time.Duration;
import java.util.List;

/**
 * "출발 N일 전까지는 몇 %"처럼 구간으로 정해지는 취소 규정의 공통 틀 (템플릿 메서드 패턴).
 * 수수료를 찾는 방법은 여기서 한 번만 구현하고, 하위 클래스는 구간표(tiers)만 제공한다.
 */
public abstract class TieredCancellationPolicy implements CancellationPolicy {

    /**
     * 수수료 구간 하나.
     * @param minTimeBefore 출발까지 이 시간 이상 남았으면 이 구간을 적용
     */
    protected record Tier(Duration minTimeBefore, double feeRate, String description) {
    }

    /** 남은 시간이 긴 구간부터 순서대로. 마지막 구간의 minTimeBefore는 Duration.ZERO여야 한다. */
    protected abstract List<Tier> tiers();

    @Override
    public final double getFeeRate(Duration timeBeforeDeparture) {
        if (timeBeforeDeparture.isNegative() || timeBeforeDeparture.isZero()) {
            throw new IllegalArgumentException("출발 이후에는 취소 수수료를 계산할 수 없습니다.");
        }
        for (Tier tier : tiers()) {
            if (timeBeforeDeparture.compareTo(tier.minTimeBefore()) >= 0) {
                return tier.feeRate();
            }
        }
        throw new IllegalStateException("취소 규정 구간이 잘못 정의되었습니다: " + getName());
    }

    @Override
    public List<String> describeRules() {
        return tiers().stream().map(Tier::description).toList();
    }
}
