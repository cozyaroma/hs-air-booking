package airline.policy;

import airline.domain.seat.CabinClass;

import java.util.Map;

/** 국제선: 개수 기준 (일반석 23kg 1개, 비즈니스석 32kg 2개). 추가 요금은 과제용 예시 값. */
public class InternationalBaggagePolicy implements BaggagePolicy {

    private static final Map<CabinClass, String> FREE_ALLOWANCE = Map.of(
            CabinClass.ECONOMY, "23kg × 1개",
            CabinClass.BUSINESS, "32kg × 2개");

    @Override
    public String getFreeAllowance(CabinClass cabin) {
        return FREE_ALLOWANCE.get(cabin);
    }

    @Override
    public String getExtraUnitName() {
        return "1개 (23kg)";
    }

    @Override
    public long getExtraFeePerUnit() {
        return 100_000;
    }

    @Override
    public int getMaxExtraUnits() {
        return 2;
    }
}
