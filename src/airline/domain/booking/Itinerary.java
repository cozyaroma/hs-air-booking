package airline.domain.booking;

import airline.domain.flight.Flight;
import airline.domain.seat.CabinClass;
import airline.exception.InvalidRequestException;
import airline.util.Formats;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 사용자가 고른 여정: 가는 편 항공권 + (왕복이면) 오는 편 항공권 + 좌석 등급.
 *
 * [은닉성] 생성자를 막고 oneWay()/roundTrip()으로만 만들 수 있다.
 *         roundTrip()이 "오는 편은 가는 편 도착 후 출발" 규칙을 검사하므로
 *         말이 안 되는 왕복 여정 객체는 만들어질 수 없다.
 */
public final class Itinerary {

    /** 같은 날 왕복일 때 가는 편 도착 후 오는 편 출발까지 필요한 최소 시간 */
    public static final Duration MIN_STAY = Duration.ofHours(1);

    private final Flight outbound;
    private final Flight inbound;     // 편도면 null
    private final CabinClass cabin;

    private Itinerary(Flight outbound, Flight inbound, CabinClass cabin) {
        this.outbound = Objects.requireNonNull(outbound);
        this.inbound = inbound;
        this.cabin = Objects.requireNonNull(cabin);
    }

    public static Itinerary oneWay(Flight outbound, CabinClass cabin) {
        return new Itinerary(outbound, null, cabin);
    }

    public static Itinerary roundTrip(Flight outbound, Flight inbound, CabinClass cabin) throws InvalidRequestException {
        Objects.requireNonNull(inbound, "오는 편 항공권");
        if (!isReverseRoute(outbound, inbound)) {
            throw new InvalidRequestException("오는 편은 가는 편의 도착지에서 출발지로 돌아오는 항공권이어야 합니다.");
        }
        if (!canPair(outbound, inbound)) {
            throw new InvalidRequestException(String.format(
                    "오는 편은 가는 편 도착(%s) %d시간 이후에 출발해야 합니다.",
                    Formats.dateTime(outbound.getArrivalTime()), MIN_STAY.toHours()));
        }
        return new Itinerary(outbound, inbound, cabin);
    }

    /** 이 두 항공권을 왕복으로 묶을 수 있는지 (오는 편 목록을 걸러낼 때 사용) */
    public static boolean canPair(Flight outbound, Flight inbound) {
        return isReverseRoute(outbound, inbound)
                && !inbound.getDepartureTime().isBefore(outbound.getArrivalTime().plus(MIN_STAY));
    }

    private static boolean isReverseRoute(Flight outbound, Flight inbound) {
        return inbound.getOrigin() == outbound.getDestination() && inbound.getDestination() == outbound.getOrigin();
    }

    public TripType getTripType() {
        return inbound == null ? TripType.ONE_WAY : TripType.ROUND_TRIP;
    }

    public boolean isRoundTrip() {
        return inbound != null;
    }

    /** 편도면 [가는 편], 왕복이면 [가는 편, 오는 편] */
    public List<SegmentType> getSegments() {
        return isRoundTrip() ? List.of(SegmentType.OUTBOUND, SegmentType.INBOUND) : List.of(SegmentType.OUTBOUND);
    }

    /** @throws IllegalArgumentException 편도인데 오는 편을 요청한 경우 */
    public Flight getFlight(SegmentType segment) {
        if (segment == SegmentType.INBOUND) {
            if (inbound == null) {
                throw new IllegalArgumentException("편도 여정에는 오는 편이 없습니다.");
            }
            return inbound;
        }
        return outbound;
    }

    public List<Flight> getFlights() {
        List<Flight> flights = new ArrayList<>();
        for (SegmentType segment : getSegments()) {
            flights.add(getFlight(segment));
        }
        return flights;
    }

    public Flight getOutbound() {
        return outbound;
    }

    /** 편도면 null */
    public Flight getInbound() {
        return inbound;
    }

    public CabinClass getCabin() {
        return cabin;
    }

    public boolean isDomestic() {
        return outbound.isDomestic();
    }

    public boolean requiresPassport() {
        return outbound.requiresPassport();
    }

    /** 예) "서울/김포 → 제주 왕복 · 일반석" */
    public String describe() {
        return outbound.getRoute() + " " + getTripType().getDisplayName() + " · " + cabin.getDisplayName();
    }
}
