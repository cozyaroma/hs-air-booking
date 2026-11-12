package airline.domain.flight;

import java.time.Duration;
import java.util.Objects;

/**
 * 노선: 출발 공항과 도착 공항의 쌍.
 * 같은 노선(김포→제주)에 날짜·시간별 운항편(Flight)이 여러 개 생긴다.
 * 영화관 예매에서 "영화"와 "상영회차"를 나눈 것과 같은 구조다.
 */
public final class Route {

    private final Airport origin;
    private final Airport destination;
    private final Duration flightTime;
    private final long standardFare;     // 일반석 성인 기준 운임
    private final long fuelSurcharge;    // 유류할증료 (1인)

    public Route(Airport origin, Airport destination, int flightMinutes, long standardFare, long fuelSurcharge) {
        Objects.requireNonNull(origin);
        Objects.requireNonNull(destination);
        if (origin == destination) {
            throw new IllegalArgumentException("출발지와 도착지가 같은 노선은 만들 수 없습니다.");
        }
        if (flightMinutes <= 0 || standardFare <= 0 || fuelSurcharge < 0) {
            throw new IllegalArgumentException("노선 정보가 올바르지 않습니다.");
        }
        this.origin = origin;
        this.destination = destination;
        this.flightTime = Duration.ofMinutes(flightMinutes);
        this.standardFare = standardFare;
        this.fuelSurcharge = fuelSurcharge;
    }

    public boolean isDomestic() {
        return origin.isInKorea() && destination.isInKorea();
    }

    public Airport getOrigin() {
        return origin;
    }

    public Airport getDestination() {
        return destination;
    }

    public Duration getFlightTime() {
        return flightTime;
    }

    public long getStandardFare() {
        return standardFare;
    }

    public long getFuelSurcharge() {
        return fuelSurcharge;
    }

    @Override
    public String toString() {
        return origin.getCityName() + " → " + destination.getCityName();
    }
}
