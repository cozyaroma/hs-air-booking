package airline.domain.flight;

import airline.policy.BaggagePolicy;
import airline.policy.CancellationPolicy;
import airline.policy.InternationalBaggagePolicy;
import airline.policy.InternationalCancellationPolicy;

import java.time.LocalDateTime;

/**
 * 국제선 운항편.
 * 여권 정보가 필요하고, 유아도 운임의 10%를 내며, 체크인은 48시간 전에 열려 1시간 전에 마감된다.
 * 세금·수수료 금액은 과제용 예시 값이다.
 */
public class InternationalFlight extends Flight {

    /** 국제선 성인 기준 나이 (만 12세 이상). 나이 계산기(AgeCalculator)도 이 값을 쓴다. */
    public static final int ADULT_MIN_AGE = 12;
    public static final int GUARDIAN_MIN_AGE = 18;

    private static final CancellationPolicy CANCELLATION_POLICY = new InternationalCancellationPolicy();
    private static final BaggagePolicy BAGGAGE_POLICY = new InternationalBaggagePolicy();

    public InternationalFlight(String flightNumber, Route route, LocalDateTime departureTime,
                               AircraftType aircraft, long adultEconomyFare) {
        super(flightNumber, route, departureTime, aircraft, adultEconomyFare);
        if (route.isDomestic()) {
            throw new IllegalArgumentException("국제선은 해외 공항이 포함되어야 합니다: " + route);
        }
    }

    @Override
    public String getTypeName() {
        return "국제선";
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
        return 0.10;
    }

    @Override
    protected long getAirportTax() {
        return 28_000;
    }

    @Override
    public int getCheckInOpenHours() {
        return 48;
    }

    @Override
    public int getCheckInCloseMinutes() {
        return 60;
    }

    @Override
    public boolean requiresPassport() {
        return true;
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
