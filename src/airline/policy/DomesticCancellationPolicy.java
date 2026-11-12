package airline.policy;

import java.time.Duration;
import java.util.List;

/** 국내선 취소 규정 (과제용 예시 값) */
public class DomesticCancellationPolicy extends TieredCancellationPolicy {

    private static final List<Tier> TIERS = List.of(
            new Tier(Duration.ofDays(7), 0.0, "출발 7일 전까지: 수수료 없음"),
            new Tier(Duration.ofHours(24), 0.10, "출발 7일 이내 ~ 24시간 전: 운임의 10%"),
            new Tier(Duration.ZERO, 0.20, "출발 24시간 이내: 운임의 20%"));

    @Override
    protected List<Tier> tiers() {
        return TIERS;
    }

    @Override
    public String getName() {
        return "국내선 취소 규정";
    }
}
