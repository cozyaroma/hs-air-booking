package airline.service;

import airline.domain.booking.Itinerary;
import airline.domain.flight.Flight;

import java.util.List;

/**
 * 검색 결과: 가는 편 항공권 목록과 (왕복이면) 오는 편 항공권 목록.
 * 두 목록 모두 지금 예약할 수 있고 남은 좌석이 충분한 항공권만, 출발 시각 순으로 담는다.
 */
public record SearchResult(SearchCriteria criteria, List<Flight> outbound, List<Flight> inbound) {

    public SearchResult {
        outbound = List.copyOf(outbound);
        inbound = List.copyOf(inbound);
    }

    /** 고른 가는 편과 왕복으로 묶을 수 있는 오는 편만 (같은 날 왕복이면 도착 1시간 뒤 출발편부터) */
    public List<Flight> inboundOptionsFor(Flight selectedOutbound) {
        return inbound.stream().filter(in -> Itinerary.canPair(selectedOutbound, in)).toList();
    }

    /** 예약을 진행할 수 있는 결과인지 (편도: 가는 편 1개 이상, 왕복: 양쪽 다 1개 이상) */
    public boolean hasOptions() {
        return !outbound.isEmpty() && (!criteria.isRoundTrip() || !inbound.isEmpty());
    }
}
