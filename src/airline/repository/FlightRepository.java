package airline.repository;

import airline.domain.flight.Airport;
import airline.domain.flight.Flight;
import airline.domain.flight.Route;

import java.time.LocalDate;
import java.util.List;

/**
 * 항공권(운항편) 저장소.
 * 서비스는 이 인터페이스에만 의존한다. 지금은 메모리에 저장하지만,
 * 파일이나 DB 저장으로 바꿀 때 구현 클래스만 새로 만들면 된다 (의존성 역전 원칙).
 */
public interface FlightRepository {

    void save(Flight flight);

    List<Flight> findByRouteAndDate(Airport origin, Airport destination, LocalDate date);

    List<Flight> findAll();

    /** 운항 중인 노선 목록 (중복 없이, 등록 순서) */
    List<Route> findAllRoutes();
}
