package airline.domain.flight;

import airline.domain.seat.CabinClass;
import airline.domain.seat.Seat;
import airline.policy.BaggagePolicy;
import airline.policy.CancellationPolicy;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 특정 날짜·시각에 운항하는 비행 1편. 화면에서는 "항공권"이라고 부른다.
 * (예: 11월 13일 07:30 김포→제주 HS1201 = 항공권 1개)
 *
 * [추상성] 국내선과 국제선은 나이 기준, 세금, 체크인 시간, 취소·수하물 규정이 다르다.
 *         공통 부분(좌석 관리, 시각 계산)은 여기서 구현하고 다른 부분은 추상 메서드로 남겼다.
 * [은닉성] 좌석 맵은 private이고 읽기 전용 컬렉션으로만 밖에 보여준다.
 * [다형성] 서비스와 화면은 Flight 타입으로만 다루고, 규정이 필요하면 getCancellationPolicy()처럼 물어본다.
 *         새 종류(예: 전세기)가 생기면 하위 클래스만 추가하면 된다.
 */
public abstract class Flight {

    private final String flightNumber;
    private final Route route;
    private final LocalDateTime departureTime;
    private final AircraftType aircraft;
    private final long adultEconomyFare;
    private final Map<String, Seat> seats;

    protected Flight(String flightNumber, Route route, LocalDateTime departureTime,
                     AircraftType aircraft, long adultEconomyFare) {
        this.flightNumber = Objects.requireNonNull(flightNumber);
        this.route = Objects.requireNonNull(route);
        this.departureTime = Objects.requireNonNull(departureTime);
        this.aircraft = Objects.requireNonNull(aircraft);
        if (adultEconomyFare <= 0) {
            throw new IllegalArgumentException("운임은 0원보다 커야 합니다.");
        }
        this.adultEconomyFare = adultEconomyFare;
        this.seats = aircraft.createSeatMap();
    }

    // ===== 국내선/국제선이 각자 정하는 규칙 =====

    /** "국내선" 또는 "국제선" */
    public abstract String getTypeName();

    /** 성인으로 보는 최소 나이 (국내선 만 13세, 국제선 만 12세) */
    public abstract int getAdultMinAge();

    /** 유아 보호자 최소 나이 (국내선 만 13세, 국제선 만 18세) */
    public abstract int getGuardianMinAge();

    /** 좌석 없는 유아의 운임 비율 (국내선 무료, 국제선 10%) */
    public abstract double getInfantFareRate();

    /** 공항이용료 등 1인당 세금 */
    protected abstract long getAirportTax();

    /** 온라인 체크인이 열리는 시점 (출발 N시간 전) */
    public abstract int getCheckInOpenHours();

    /** 체크인·예약 마감 (출발 N분 전) */
    public abstract int getCheckInCloseMinutes();

    public abstract boolean requiresPassport();

    public abstract CancellationPolicy getCancellationPolicy();

    public abstract BaggagePolicy getBaggagePolicy();

    // ===== 공통 기능 =====

    /** 1인당 세금 합계 = 공항이용료 + 노선별 유류할증료 */
    public long getTaxPerPassenger() {
        return getAirportTax() + route.getFuelSurcharge();
    }

    public LocalDateTime getBookingDeadline() {
        return departureTime.minusMinutes(getCheckInCloseMinutes());
    }

    public boolean isBookable(LocalDateTime now) {
        return now.isBefore(getBookingDeadline());
    }

    public LocalDateTime getCheckInOpenTime() {
        return departureTime.minusHours(getCheckInOpenHours());
    }

    public Optional<Seat> findSeat(String code) {
        return Optional.ofNullable(seats.get(code));
    }

    /** 좌석 목록 (읽기 전용). 좌석 상태는 Seat의 메서드로만 바꿀 수 있다. */
    public Collection<Seat> getSeats() {
        return Collections.unmodifiableCollection(seats.values());
    }

    public int countAvailableSeats(CabinClass cabin) {
        return (int) seats.values().stream()
                .filter(seat -> seat.getCabin() == cabin && seat.isAvailable())
                .count();
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public Route getRoute() {
        return route;
    }

    public Airport getOrigin() {
        return route.getOrigin();
    }

    public Airport getDestination() {
        return route.getDestination();
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public LocalDate getDepartureDate() {
        return departureTime.toLocalDate();
    }

    public LocalDateTime getArrivalTime() {
        return departureTime.plus(route.getFlightTime());
    }

    public Duration getFlightTime() {
        return route.getFlightTime();
    }

    public AircraftType getAircraft() {
        return aircraft;
    }

    public long getAdultEconomyFare() {
        return adultEconomyFare;
    }

    public boolean isDomestic() {
        return route.isDomestic();
    }

    /** 출발 시각이 속한 시간대 (오전/오후 등). 항공권 선택 화면의 시간대 필터에 쓴다. */
    public TimeBand getTimeBand() {
        return TimeBand.of(departureTime.toLocalTime());
    }

    @Override
    public String toString() {
        return flightNumber + " " + route + " " + departureTime;
    }
}
