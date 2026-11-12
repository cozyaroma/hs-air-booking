package airline.domain.flight;

import airline.policy.BaggagePolicy;
import airline.policy.CancellationPolicy;
import airline.policy.DomesticBaggagePolicy;
import airline.policy.DomesticCancellationPolicy;

import java.time.LocalDateTime;

/**
 * 국내선 운항편.
 * 나이 기준과 유아 운임은 대한항공 안내를 참고했고(소아 만 13세 미만, 유아 무료, 보호자 만 13세 이상),
 * 세금·수수료 금액은 과제용 예시 값이다.
 */
public class DomesticFlight extends Flight {

    /** 국내선 성인 기준 나이 (만 13세 이상). 나이 계산기(AgeCalculator)도 이 값을 쓴다. */
    public static final int ADULT_MIN_AGE = 13;
    public static final int GUARDIAN_MIN_AGE = 13;

    private static final CancellationPolicy CANCELLATION_POLICY = new DomesticCancellationPolicy();
    private static final BaggagePolicy BAGGAGE_POLICY = new DomesticBaggagePolicy();

    public DomesticFlight(String flightNumber, Route route, LocalDateTime departureTime,
                          AircraftType aircraft, long adultEconomyFare) {
        super(flightNumber, route, departureTime, aircraft, adultEconomyFare);
        if (!route.isDomestic()) {
            throw new IllegalArgumentException("국내선은 국내 공항끼리만 운항합니다: " + route);
        }
    }

    @Override
    public String getTypeName() {
        return "국내선";
    }

    @Override
    public int getAdultMinAge() {
        return ADULT_MIN_AGE;
    }

    @Override
    public int getGuardianMinAge() {
        return GUARDIAN_MIN_AGE;
    }

    @Override
    public double getInfantFareRate() {
        return 0.0;
    }

    @Override
    protected long getAirportTax() {
        return 4_000;
    }

    @Override
    public int getCheckInOpenHours() {
        return 24;
    }

    @Override
    public int getCheckInCloseMinutes() {
        return 30;
    }

    @Override
    public boolean requiresPassport() {
        return false;
    }

    @Override
    public CancellationPolicy getCancellationPolicy() {
        return CANCELLATION_POLICY;
    }

    @Override
    public BaggagePolicy getBaggagePolicy() {
        return BAGGAGE_POLICY;
    }
}
