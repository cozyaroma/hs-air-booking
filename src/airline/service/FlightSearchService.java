package airline.service;

import airline.domain.flight.Airport;
import airline.domain.flight.Flight;
import airline.domain.flight.Route;
import airline.domain.seat.CabinClass;
import airline.exception.AirlineException;
import airline.exception.InvalidRequestException;
import airline.repository.FlightRepository;
import airline.util.AppClock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/** 항공권 검색: 출발지·도착지 목록, 날짜 범위, 편도/왕복 검색, 최저가 */
public class FlightSearchService {

    private final FlightRepository flightRepository;
    private final FareCalculator fareCalculator;
    private final AppClock clock;

    public FlightSearchService(FlightRepository flightRepository, FareCalculator fareCalculator, AppClock clock) {
        this.flightRepository = flightRepository;
        this.fareCalculator = fareCalculator;
        this.clock = clock;
    }

    // ===== 검색 화면 선택지 =====

    /** 출발지로 고를 수 있는 공항 (취항 노선이 있는 곳만, 공항 목록 순서) */
    public List<Airport> getOrigins() {
        return flightRepository.findAllRoutes().stream()
                .map(Route::getOrigin)
                .distinct()
                .sorted()
                .toList();
    }

    /** 이 출발지에서 갈 수 있는 도착지만. 출발지를 바꾸면 도착지 목록을 이걸로 다시 채운다. */
    public List<Airport> getDestinations(Airport origin) {
        return flightRepository.findAllRoutes().stream()
                .filter(route -> route.getOrigin() == origin)
                .map(Route::getDestination)
                .distinct()
                .sorted()
                .toList();
    }

    public boolean isServed(Airport origin, Airport destination) {
        return getDestinations(origin).contains(destination);
    }

    /** 달력에서 고를 수 있는 첫날 (오늘) */
    public LocalDate getFirstSearchableDate() {
        return clock.today();
    }

    /** 달력에서 고를 수 있는 마지막 날 (운항 스케줄이 있는 마지막 날). 스케줄이 없으면 오늘. */
    public LocalDate getLastSearchableDate() {
        return flightRepository.findAll().stream()
                .map(Flight::getDepartureDate)
                .max(Comparator.naturalOrder())
                .orElse(clock.today());
    }

    // ===== 검색 =====

    /**
     * 조건에 맞고 지금 예약할 수 있는 항공권을 출발 시각 순으로 돌려준다.
     * 예약 마감이 지났거나 남은 좌석이 인원보다 적은 항공권은 제외한다.
     */
    public SearchResult search(SearchCriteria criteria) throws AirlineException {
        criteria.validate();
        if (criteria.departureDate().isBefore(clock.today())) {
            throw new InvalidRequestException("지난 날짜는 검색할 수 없습니다.");
        }
        if (!isServed(criteria.origin(), criteria.destination())) {
            throw new InvalidRequestException(criteria.origin().getCityName() + " → "
                    + criteria.destination().getCityName() + " 노선은 운항하지 않습니다.");
        }
        List<Flight> outbound = findBookable(criteria.origin(), criteria.destination(),
                criteria.departureDate(), criteria.cabin(), criteria.getSeatedCount());
        List<Flight> inbound = criteria.isRoundTrip()
                ? findBookable(criteria.destination(), criteria.origin(), criteria.returnDate(),
                        criteria.cabin(), criteria.getSeatedCount())
                : List.of();
        return new SearchResult(criteria, outbound, inbound);
    }

    private List<Flight> findBookable(Airport origin, Airport destination, LocalDate date,
                                      CabinClass cabin, int seatsNeeded) {
        LocalDateTime now = clock.now();
        return flightRepository.findByRouteAndDate(origin, destination, date).stream()
                .filter(flight -> flight.isBookable(now))
                .filter(flight -> flight.countAvailableSeats(cabin) >= Math.max(1, seatsNeeded))
                .sorted(Comparator.comparing(Flight::getDepartureTime))
                .toList();
    }

    // ===== 요금 =====

    /** 항공권 목록에 보여줄 성인 1인 요금 (운임 + 세금) */
    public long getAdultPrice(Flight flight, CabinClass cabin) {
        return fareCalculator.adultTotal(flight, cabin);
    }

    /** 그날 예약 가능한 항공권 중 성인 1인 최저가. 달력에 가격을 표시할 때 사용 (없으면 빈 값). */
    public OptionalLong findLowestAdultPrice(Airport origin, Airport destination, LocalDate date, CabinClass cabin) {
        return findBookable(origin, destination, date, cabin, 1).stream()
                .mapToLong(flight -> getAdultPrice(flight, cabin))
                .min();
    }

    /** 운항 정보(노선) 조회. 비행 시간 안내 등에 사용 */
    public Optional<Route> findRoute(Airport origin, Airport destination) {
        return flightRepository.findAllRoutes().stream()
                .filter(route -> route.getOrigin() == origin && route.getDestination() == destination)
                .findFirst();
    }
}
