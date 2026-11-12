package airline.policy;

import airline.domain.seat.CabinClass;

/**
 * 위탁 수하물 규정.
 * 실제로 국내선은 무게(kg) 기준, 국제선(아시아 노선 다수)은 개수 기준이라
 * 무료 허용량을 표현하는 방식과 추가 요금 단위가 다르다.
 */
public interface BaggagePolicy {

    /** 좌석 등급별 무료 허용량 설명. 예) "20kg", "23kg × 1개" */
    String getFreeAllowance(CabinClass cabin);

    /** 추가 구매 단위 설명. 예) "10kg", "1개 (23kg)" */
    String getExtraUnitName();

    long getExtraFeePerUnit();

    int getMaxExtraUnits();

    /** 추가 수하물 요금. 구현 클래스가 단위 요금만 정하면 계산은 공통으로 처리한다. */
    default long calculateExtraFee(int units) {
        if (units < 0 || units > getMaxExtraUnits()) {
            throw new IllegalArgumentException("추가 수하물은 0~" + getMaxExtraUnits() + " 단위까지 구매할 수 있습니다.");
        }
        return units * getExtraFeePerUnit();
    }
}
