package airline.ui;

import airline.domain.booking.Booking;
import airline.domain.booking.Itinerary;
import airline.domain.booking.SegmentType;
import airline.domain.flight.Flight;
import airline.domain.passenger.Passenger;
import airline.domain.seat.Seat;
import airline.exception.InvalidRequestException;
import airline.service.BookingRequest;
import airline.service.SearchCriteria;
import airline.service.SearchResult;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 페이지끼리 진행 중인 예약 정보를 주고받는 유일한 통로 (페이지 간 계약).
 *
 * 각 페이지는 앞 단계가 남긴 값을 읽고, 자기 단계의 결과를 여기에 저장한 뒤 다음 페이지로 이동한다.
 * 페이지끼리 서로의 클래스를 직접 부르거나 static 변수로 데이터를 넘기지 않는다.
 *
 *   [A] 항공권 검색   → startSearch(criteria, result)
 *   [A] 항공권 선택   → selectOutbound(flight), selectInbound(flight)   (왕복이면 둘 다)
 *   [B] 승객 정보     → setPassengers(list)
 *   [B] 좌석 선택     → assignSeat(segment, passenger, seat) / replaceSeats(segment, map)
 *   [C] 결제          → setExtraBaggage(...), toRequest() → 예약 생성 → completeBooking(booking)
 *
 * 앞 단계를 다시 바꾸면 그 뒤 단계의 값은 자동으로 비워진다 (예: 승객을 바꾸면 좌석·수하물 초기화).
 * 각 페이지에 들어오기 전에 무엇이 채워져 있어야 하는지는 Screen.check()에 정리되어 있다.
 */
public class BookingSession {

    private SearchCriteria criteria;
    private SearchResult searchResult;
    private Flight outbound;
    private Flight inbound;
    private List<Passenger> passengers = List.of();
    private final Map<SegmentType, Map<Passenger, Seat>> seats = new EnumMap<>(SegmentType.class);
    private final Map<SegmentType, Map<Passenger, Integer>> extraBaggage = new EnumMap<>(SegmentType.class);
    private Booking lastBooking;

    public BookingSession() {
        clearSeatsAndBaggage();
    }

    // ===== 1. 항공권 검색 [A] =====

    /** 새로 검색하면 그 뒤 단계(항공권 선택, 승객, 좌석, 수하물)를 모두 비운다. */
    public void startSearch(SearchCriteria newCriteria, SearchResult result) {
        this.criteria = newCriteria;
        this.searchResult = result;
        this.outbound = null;
        this.inbound = null;
        this.passengers = List.of();
        clearSeatsAndBaggage();
    }

    public SearchCriteria getCriteria() {
        return criteria;
    }

    public SearchResult getSearchResult() {
        return searchResult;
    }

    public boolean hasSearch() {
        return criteria != null && searchResult != null;
    }

    // ===== 2. 항공권 선택 [A] =====

    /** 가는 편 선택. 바뀌면 가는 편 좌석을 비우고, 더 이상 묶을 수 없는 오는 편 선택도 취소한다. */
    public void selectOutbound(Flight flight) {
        if (flight != outbound) {
            outbound = flight;
            seats.get(SegmentType.OUTBOUND).clear();
            if (inbound != null && (flight == null || !Itinerary.canPair(flight, inbound))) {
                inbound = null;
                seats.get(SegmentType.INBOUND).clear();
            }
        }
    }

    /**
     * 오는 편 선택 (왕복만). 가는 편을 먼저 골라야 한다.
     * @throws InvalidRequestException 가는 편이 없거나 시간상 왕복으로 묶을 수 없는 경우
     */
    public void selectInbound(Flight flight) throws InvalidRequestException {
        if (flight != null) {
            if (outbound == null) {
                throw new InvalidRequestException("가는 편 항공권을 먼저 선택하세요.");
            }
            Itinerary.roundTrip(outbound, flight, criteria.cabin());   // 규칙 검사만 (실패하면 예외)
        }
        if (flight != inbound) {
            inbound = flight;
            seats.get(SegmentType.INBOUND).clear();
        }
    }

    public Flight getOutbound() {
        return outbound;
    }

    /** 편도이거나 아직 안 골랐으면 null */
    public Flight getInbound() {
        return inbound;
    }

    /** 편도면 가는 편, 왕복이면 가는 편+오는 편이 모두 골라졌는지 */
    public boolean hasCompleteItinerary() {
        return hasSearch() && outbound != null && (!criteria.isRoundTrip() || inbound != null);
    }

    /** 고른 여정. 아직 다 고르지 않았으면 null */
    public Itinerary getItinerary() {
        if (!hasCompleteItinerary()) {
            return null;
        }
        if (!criteria.isRoundTrip()) {
            return Itinerary.oneWay(outbound, criteria.cabin());
        }
        try {
            return Itinerary.roundTrip(outbound, inbound, criteria.cabin());
        } catch (InvalidRequestException e) {
            return null;   // selectInbound에서 이미 검사했으므로 일어나지 않음
        }
    }

    /** 승객·좌석·결제 페이지용: 여정이 반드시 있어야 하는 곳에서 사용 */
    public Itinerary requireItinerary() {
        Itinerary itinerary = getItinerary();
        if (itinerary == null) {
            throw new IllegalStateException("항공권 선택이 끝나지 않았습니다. (Screen.check로 먼저 확인하세요)");
        }
        return itinerary;
    }

    // ===== 3. 승객 정보 [B] =====

    /** 승객을 새로 저장하면 좌석과 수하물 선택이 초기화된다. */
    public void setPassengers(List<Passenger> newPassengers) {
        passengers = List.copyOf(newPassengers);
        clearSeatsAndBaggage();
    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    /** 좌석을 차지하는 승객 (유아 제외) */
    public List<Passenger> getSeatedPassengers() {
        return passengers.stream().filter(Passenger::occupiesSeat).toList();
    }

    /** 검색 인원 수만큼 승객 정보가 저장되었는지 */
    public boolean hasPassengers() {
        return criteria != null && !passengers.isEmpty() && passengers.size() == criteria.getTotalCount();
    }

    // ===== 4. 좌석 선택 [B] (구간마다 따로) =====

    public void assignSeat(SegmentType segment, Passenger passenger, Seat seat) {
        seats.get(segment).put(passenger, seat);
    }

    public void unassignSeat(SegmentType segment, Passenger passenger) {
        seats.get(segment).remove(passenger);
    }

    public void clearSeats(SegmentType segment) {
        seats.get(segment).clear();
    }

    public void replaceSeats(SegmentType segment, Map<Passenger, Seat> newSeats) {
        seats.get(segment).clear();
        seats.get(segment).putAll(newSeats);
    }

    public Seat getSeat(SegmentType segment, Passenger passenger) {
        return seats.get(segment).get(passenger);
    }

    /** 구간의 좌석 배정 복사본 (바꿔도 세션에는 영향 없음) */
    public Map<Passenger, Seat> getSeats(SegmentType segment) {
        return new LinkedHashMap<>(seats.get(segment));
    }

    /** 이 좌석을 고른 승객 (좌석 배치도에서 "선택됨" 표시할 때) */
    public Optional<Passenger> findPassengerAt(SegmentType segment, Seat seat) {
        return seats.get(segment).entrySet().stream()
                .filter(entry -> entry.getValue() == seat)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public boolean allSeatsAssigned(SegmentType segment) {
        return getSeatedPassengers().stream().allMatch(seats.get(segment)::containsKey);
    }

    /** 여정의 모든 구간에서 좌석을 차지하는 승객 전원의 좌석이 골라졌는지 */
    public boolean allSeatsAssigned() {
        Itinerary itinerary = getItinerary();
        return itinerary != null && hasPassengers()
                && itinerary.getSegments().stream().allMatch(this::allSeatsAssigned);
    }

    // ===== 5. 결제 [C] =====

    public void setExtraBaggage(SegmentType segment, Passenger passenger, int units) {
        extraBaggage.get(segment).put(passenger, units);
    }

    public int getExtraBaggage(SegmentType segment, Passenger passenger) {
        return extraBaggage.get(segment).getOrDefault(passenger, 0);
    }

    /** 결제 직전 예약 요청 만들기 (BookingService.quoteTotal / createBooking에 넘긴다) */
    public BookingRequest toRequest() {
        return new BookingRequest(requireItinerary(), passengers, seats, extraBaggage);
    }

    /** 예약이 끝나면 진행 정보를 비우고 결과만 남긴다. 검색 조건은 남겨서 다시 검색하기 쉽게 한다. */
    public void completeBooking(Booking booking) {
        lastBooking = booking;
        searchResult = null;
        outbound = null;
        inbound = null;
        passengers = List.of();
        clearSeatsAndBaggage();
    }

    /** 방금 끝난 예약 (예약 완료 페이지가 보여줌). 없으면 null */
    public Booking getLastBooking() {
        return lastBooking;
    }

    /** 모두 처음 상태로 */
    public void reset() {
        criteria = null;
        completeBooking(null);
    }

    private void clearSeatsAndBaggage() {
        for (SegmentType segment : SegmentType.values()) {
            seats.computeIfAbsent(segment, s -> new LinkedHashMap<>()).clear();
            extraBaggage.computeIfAbsent(segment, s -> new LinkedHashMap<>()).clear();
        }
    }

    /** 개발용: 지금 세션에 무엇이 들어 있는지 사람이 읽을 수 있게 */
    public String describeForDebug() {
        StringBuilder sb = new StringBuilder();
        sb.append("검색 조건: ").append(criteria == null ? "없음" : criteria.describe()).append('\n');
        if (searchResult != null) {
            sb.append("검색 결과: 가는 편 ").append(searchResult.outbound().size()).append("개");
            if (criteria.isRoundTrip()) {
                sb.append(", 오는 편 ").append(searchResult.inbound().size()).append("개");
            }
            sb.append('\n');
        }
        sb.append("가는 편: ").append(outbound == null ? "미선택" : outbound).append('\n');
        if (criteria != null && criteria.isRoundTrip()) {
            sb.append("오는 편: ").append(inbound == null ? "미선택" : inbound).append('\n');
        }
        sb.append("승객: ").append(passengers.isEmpty() ? "없음" : passengers).append('\n');
        for (SegmentType segment : SegmentType.values()) {
            if (!seats.get(segment).isEmpty()) {
                sb.append(segment.getDisplayName()).append(" 좌석: ");
                seats.get(segment).forEach((p, s) -> sb.append(p.getFullName()).append('=').append(s).append(' '));
                sb.append('\n');
            }
            if (!extraBaggage.get(segment).isEmpty()) {
                sb.append(segment.getDisplayName()).append(" 추가 수하물: ").append(extraBaggage.get(segment)).append('\n');
            }
        }
        sb.append("마지막 예약: ").append(lastBooking == null ? "없음" : lastBooking.getBookingCode());
        return sb.toString();
    }
}
