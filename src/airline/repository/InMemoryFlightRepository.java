package airline.repository;

import airline.domain.flight.Airport;
import airline.domain.flight.Flight;
import airline.domain.flight.Route;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/** 메모리(List)에 항공권(운항편)을 저장하는 구현. 프로그램을 끄면 사라진다. */
public class InMemoryFlightRepository implements FlightRepository {

    private final List<Flight> flights = new ArrayList<>();

    @Override
    public void save(Flight flight) {
        flights.add(flight);
    }

    @Override
    public List<Flight> findByRouteAndDate(Airport origin, Airport destination, LocalDate date) {
        return flights.stream()
                .filter(f -> f.getOrigin() == origin && f.getDestination() == destination)
                .filter(f -> f.getDepartureDate().equals(date))
                .toList();
    }

    @Override
    public List<Flight> findAll() {
        return Collections.unmodifiableList(flights);
    }

    @Override
    public List<Route> findAllRoutes() {
        LinkedHashSet<Route> routes = new LinkedHashSet<>();
        for (Flight flight : flights) {
            routes.add(flight.getRoute());
        }
        return List.copyOf(routes);
    }
}
