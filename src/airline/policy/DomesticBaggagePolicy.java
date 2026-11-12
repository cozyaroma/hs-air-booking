package airline.policy;

import airline.domain.seat.CabinClass;

import java.util.Map;

/** 국내선: 무게 기준 (일반석 20kg, 비즈니스석 30kg). 추가 요금은 과제용 예시 값. */
public class DomesticBaggagePolicy implements BaggagePolicy {

    private static final Map<CabinClass, String> FREE_ALLOWANCE = Map.of(
            CabinClass.ECONOMY, "20kg",
            CabinClass.BUSINESS, "30kg");

    @Override
    public String getFreeAllowance(CabinClass cabin) {
        return FREE_ALLOWANCE.get(cabin);
    }

    @Override
    public String getExtraUnitName() {
        return "10kg";
    }

    @Override
    public long getExtraFeePerUnit() {
        return 20_000;
    }

    @Override
    public int getMaxExtraUnits() {
        return 3;
    }
}
