package airline.ui;

import airline.domain.booking.Itinerary;
import airline.domain.booking.SegmentType;

import java.util.ArrayList;
import java.util.List;

/**
 * 페이지 목록과 각 페이지의 "입장 조건".
 *
 * check()는 그 페이지를 열기 전에 BookingSession에 무엇이 채워져 있어야 하는지 검사하고,
 * 빠진 것을 사람이 읽을 수 있는 문장 목록으로 돌려준다 (비어 있으면 입장 가능).
 * 다시 말해 "어떤 페이지의 다음 버튼이 끝내야 할 일" = "다음 페이지의 check()가 통과하는 것"이다.
 *
 * MainFrame.navigate()가 이동할 때마다 이 검사를 하고, 개발 모드에서는 결과를 콘솔에 출력한다.
 */
public enum Screen {
    SEARCH("항공권 검색", "출발지와 도착지, 날짜, 인원을 선택하세요.", 1, PageOwner.A, false),
    FLIGHT_SELECT("항공권 선택", "원하는 시간대의 항공권을 선택하세요.", 2, PageOwner.A, false),
    PASSENGER("승객 정보", "여권(신분증)과 같은 영문 이름으로 입력하세요.", 3, PageOwner.B, true),
    SEAT("좌석 선택", "구간마다 승객별 좌석을 선택하세요.", 4, PageOwner.B, true),
    PAYMENT("결제", "추가 수하물과 결제 수단을 선택하세요.", 5, PageOwner.C, true),
    CONFIRMATION("예약 완료", "예약이 확정되었습니다.", 0, PageOwner.C, false),
    MY_BOOKINGS("내 예약", "예약 내역을 확인하고 취소·체크인할 수 있습니다.", 0, PageOwner.C, false);

    /** 예약 진행 단계 표시줄에 나오는 순서 */
    public static final List<Screen> BOOKING_FLOW = List.of(SEARCH, FLIGHT_SELECT, PASSENGER, SEAT, PAYMENT);

    private final String title;
    private final String subtitle;
    private final int step;              // 0이면 단계 표시줄을 보여주지 않음
    private final PageOwner owner;
    private final boolean showsTicketSummary;

    Screen(String title, String subtitle, int step, PageOwner owner, boolean showsTicketSummary) {
        this.title = title;
        this.subtitle = subtitle;
        this.step = step;
        this.owner = owner;
        this.showsTicketSummary = showsTicketSummary;
    }

    /** 이 페이지를 열기 위해 세션에 빠진 것들. 빈 목록이면 열 수 있다. */
    public List<String> check(BookingSession session) {
        List<String> missing = new ArrayList<>();
        switch (this) {
            case SEARCH, MY_BOOKINGS -> {
                // 조건 없음
            }
            case CONFIRMATION -> {
                if (session.getLastBooking() == null) {
                    missing.add("완료된 예약이 없습니다. (결제 후 completeBooking 필요)");
                }
            }
            default -> checkBookingFlow(session, missing);
        }
        return missing;
    }

    private void checkBookingFlow(BookingSession session, List<String> missing) {
        if (!session.hasSearch()) {
            missing.add("항공권 검색이 필요합니다. (startSearch 필요)");
            return;
        }
        if (this == FLIGHT_SELECT) {
            return;
        }
        if (session.getOutbound() == null) {
            missing.add("가는 편 항공권을 선택해야 합니다. (selectOutbound 필요)");
        }
        if (session.getCriteria().isRoundTrip() && session.getInbound() == null) {
            missing.add("오는 편 항공권을 선택해야 합니다. (selectInbound 필요)");
        }
        if (!missing.isEmpty() || this == PASSENGER) {
            return;
        }
        if (!session.hasPassengers()) {
            missing.add("승객 " + session.getCriteria().getTotalCount() + "명의 정보가 필요합니다. (setPassengers 필요)");
            return;
        }
        if (this == SEAT) {
            return;
        }
        Itinerary itinerary = session.requireItinerary();
        for (SegmentType segment : itinerary.getSegments()) {
            if (!session.allSeatsAssigned(segment)) {
                missing.add(segment.getDisplayName() + " 좌석을 모두 선택해야 합니다. (assignSeat 필요)");
            }
        }
    }

    /** 단계 표시줄 기준 바로 앞 페이지 (없으면 null) */
    public Screen previous() {
        int index = BOOKING_FLOW.indexOf(this);
        return index > 0 ? BOOKING_FLOW.get(index - 1) : null;
    }

    /** 단계 표시줄 기준 다음 페이지 (없으면 null) */
    public Screen next() {
        int index = BOOKING_FLOW.indexOf(this);
        return (index >= 0 && index < BOOKING_FLOW.size() - 1) ? BOOKING_FLOW.get(index + 1) : null;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public int getStep() {
        return step;
    }

    public PageOwner getOwner() {
        return owner;
    }

    /** 상단에 "선택한 항공권 요약"을 보여주는 페이지인지 (승객 정보 ~ 결제) */
    public boolean showsTicketSummary() {
        return showsTicketSummary;
    }
}
