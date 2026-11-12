package airline.ui.page.passenger;

import airline.domain.booking.Itinerary;
import airline.domain.booking.SegmentType;
import airline.domain.flight.Flight;
import airline.domain.passenger.Passenger;
import airline.domain.seat.Seat;
import airline.exception.AirlineException;
import airline.ui.AppContext;
import airline.ui.BasePanel;
import airline.ui.Screen;
import airline.ui.UiTheme;
import airline.util.Formats;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Map;

/**
 * [B 담당] 4단계: 좌석 선택 페이지 (구간마다 따로).
 * 요구사항 전체: docs/pages/B-passenger-seat.md
 *
 * ── 페이지 계약 ──────────────────────────────────────────────
 * 들어올 때 보장: 여정(session().requireItinerary())과 승객(session().getPassengers())이 있음
 *               좌석이 필요한 승객 = session().getSeatedPassengers() (유아 제외)
 * 나갈 때 할 일: 여정의 모든 구간(itinerary.getSegments())에서 좌석이 필요한 승객 전원에게
 *               session().assignSeat(구간, 승객, 좌석) → session().allSeatsAssigned()가 true
 *               그 다음 app.navigate(Screen.PAYMENT)
 *
 * ── 쓸 수 있는 공용 기능 ─────────────────────────────────────
 * itinerary.getFlight(구간).getSeats()       ← 좌석 객체 전체 (Seat: getRow, getColumn, getCode, isAvailable,
 *                                              getCabin, getExtraFee, getDescription)
 * itinerary.getFlight(구간).getAircraft().getTotalRows(), AircraftType.ALL_COLUMNS  ← 배치도 그리기
 * BookingService.checkSeat(flight, cabin, passenger, seat)  ← 등급, 이미 예약됨, 비상구 나이·유아동반 규칙 검사
 * BookingService.autoAssignSeats(flight, cabin, 승객들, 이미 고른 좌석)  ← 남은 승객 자동 배정
 * session().findPassengerAt(구간, 좌석)  ← 이 좌석을 고른 승객 (배치도에 "선택됨" 표시용)
 *
 * ── 지금 상태 (뼈대) ─────────────────────────────────────────
 *  [완료] 구간마다 "자동 배정" 버튼과 배정 결과 표시
 *  TODO(B) 좌석 배치도 (구간별 탭: 가는 편 / 오는 편), 좌석 버튼 클릭으로 선택·해제
 *  TODO(B) 좌석 상태별 색 (선택 가능 / 이미 예약됨 / 비상구 유료 / 다른 등급) + 범례
 *  TODO(B) 지금 좌석을 고르는 승객 선택 (승객 목록에서 클릭), 클릭 시 checkSeat로 규칙 검사
 *  TODO(B) 비상구석 추가 요금 안내, 승객별 선택 좌석 요약
 */
public class SeatPage extends BasePanel {

    private final JPanel segmentList = new JPanel();

    public SeatPage(AppContext app) {
        super(app, Screen.SEAT);
        segmentList.setLayout(new BoxLayout(segmentList, BoxLayout.Y_AXIS));
        segmentList.setOpaque(false);
        setScrollableContent(segmentList);

        addBackButton();
        JButton autoAll = UiTheme.secondaryButton("모든 구간 자동 배정");
        autoAll.addActionListener(e -> autoAssignAll());
        addFooterButton(autoAll);
        JButton next = UiTheme.primaryButton("다음: 결제");
        next.addActionListener(e -> goNext());
        addFooterButton(next);
    }

    @Override
    protected void onShow() {
        Itinerary itinerary = session().requireItinerary();
        segmentList.removeAll();
        for (SegmentType segment : itinerary.getSegments()) {
            segmentList.add(createSegmentPanel(segment, itinerary.getFlight(segment)));
            segmentList.add(Box.createVerticalStrut(12));
        }
        segmentList.revalidate();
        segmentList.repaint();
    }

    private JPanel createSegmentPanel(SegmentType segment, Flight flight) {
        JPanel panel = UiTheme.card(new BorderLayout(0, 8));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        head.setOpaque(false);
        head.add(UiTheme.heading(segment.getDisplayName() + "  " + flight.getFlightNumber()));
        head.add(UiTheme.muted(Formats.dateTime(flight.getDepartureTime()) + " · " + flight.getAircraft()));
        JButton auto = UiTheme.secondaryButton("자동 배정");
        auto.addActionListener(e -> autoAssign(segment));
        head.add(auto);
        panel.add(head, BorderLayout.NORTH);

        JPanel assigned = new JPanel();
        assigned.setLayout(new BoxLayout(assigned, BoxLayout.Y_AXIS));
        assigned.setOpaque(false);
        for (Passenger passenger : session().getSeatedPassengers()) {
            Seat seat = session().getSeat(segment, passenger);
            String text = passenger.getFullName() + "  →  " + (seat == null ? "미선택" : seat.getCode() + " " + seat.getDescription());
            assigned.add(new JLabel(text));
        }
        panel.add(assigned, BorderLayout.CENTER);
        return panel;
    }

    private void autoAssign(SegmentType segment) {
        Itinerary itinerary = session().requireItinerary();
        try {
            Map<Passenger, Seat> seats = app.getBookingService().autoAssignSeats(itinerary.getFlight(segment),
                    itinerary.getCabin(), session().getSeatedPassengers(), session().getSeats(segment));
            session().replaceSeats(segment, seats);    // ← 페이지 계약: 결제 페이지가 읽을 값
            onShow();
        } catch (AirlineException e) {
            showError(e);
        }
    }

    private void autoAssignAll() {
        for (SegmentType segment : session().requireItinerary().getSegments()) {
            autoAssign(segment);
        }
    }

    private void goNext() {
        if (!session().allSeatsAssigned()) {
            showWarning("좌석 선택", "모든 구간에서 승객 전원의 좌석을 선택하세요.");
            return;
        }
        app.navigate(Screen.PAYMENT);
    }
}
