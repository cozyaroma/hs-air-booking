package airline.policy;

import java.time.Duration;
import java.util.List;

/**
 * 취소 수수료 규정.
 * 국내선과 국제선은 수수료 구간이 달라서 각자 구현 클래스를 가진다.
 * Booking은 어떤 규정인지 몰라도 getFeeRate()만 호출하면 된다.
 */
public interface CancellationPolicy {

    /**
     * 출발까지 남은 시간에 따른 수수료율 (0.0 ~ 1.0). 운임에만 적용한다.
     * @param timeBeforeDeparture 0보다 커야 한다 (출발 후에는 취소 자체가 불가)
     */
    double getFeeRate(Duration timeBeforeDeparture);

    /** 규정 이름 (화면 표시용) */
    String getName();

    /** 구간별 안내 문구 (화면 표시용) */
    List<String> describeRules();
}
