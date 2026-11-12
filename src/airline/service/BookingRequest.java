package airline.service;

import airline.domain.booking.Itinerary;
import airline.domain.booking.SegmentType;
import airline.domain.passenger.Passenger;
import airline.domain.seat.Seat;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 결제 직전까지 모은 예약 요청: 여정, 승객, 구간별 좌석, 구간별 추가 수하물.
 * BookingSession.toRequest()가 만든다. 만든 뒤에는 내용이 바뀌지 않는다.
 */
public final class BookingRequest {

    private final Itinerary itinerary;
    private final List<Passenger> passengers;
    private final Map<SegmentType, Map<Passenger, Seat>> seats = new EnumMap<>(SegmentType.class);
    private final Map<SegmentType, Map<Passenger, Integer>> extraBaggage = new EnumMap<>(SegmentType.class);

    public BookingRequest(Itinerary itinerary, List<Passenger> passengers,
                          Map<SegmentType, Map<Passenger, Seat>> seats,
                          Map<SegmentType, Map<Passenger, Integer>> extraBaggage) {
        this.itinerary = Objects.requireNonNull(itinerary);
        this.passengers = List.copyOf(passengers);
        for (SegmentType segment : itinerary.getSegments()) {
            this.seats.put(segment, new HashMap<>(seats.getOrDefault(segment, Map.of())));
            this.extraBaggage.put(segment, new HashMap<>(extraBaggage.getOrDefault(segment, Map.of())));
        }
    }

    public Itinerary getItinerary() {
        return itinerary;
    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    /** 좌석이 없으면(유아 또는 미선택) null */
    public Seat getSeat(SegmentType segment, Passenger passenger) {
        return seats.getOrDefault(segment, Map.of()).get(passenger);
    }

    public int getExtraBaggage(SegmentType segment, Passenger passenger) {
        return extraBaggage.getOrDefault(segment, Map.of()).getOrDefault(passenger, 0);
    }
}
