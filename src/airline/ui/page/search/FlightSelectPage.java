package airline.ui.page.search;

import airline.domain.flight.Flight;
import airline.domain.seat.CabinClass;
import airline.exception.AirlineException;
import airline.service.SearchCriteria;
import airline.service.SearchResult;
import airline.ui.AppContext;
import airline.ui.BasePanel;
import airline.ui.Screen;
import airline.ui.UiTheme;
import airline.util.Formats;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.List;

/**
 * [A 담당] 2단계: 항공권(시간대) 선택 페이지.
 * 요구사항 전체: docs/pages/A-search.md
 *
 * ── 페이지 계약 ──────────────────────────────────────────────
 * 들어올 때 보장: session().getCriteria(), session().getSearchResult()가 있음
 *   (왕복이면 getSearchResult().inbound()에 오는 편 목록이 있음)
 * 나갈 때 할 일: session().selectOutbound(가는 편)
 *               왕복이면 session().selectInbound(오는 편)   ← 시간이 안 맞으면 예외(InvalidRequestException)
 *               그 다음 app.navigate(Screen.PASSENGER)
 *
 * ── 쓸 수 있는 공용 기능 ─────────────────────────────────────
 * SearchResult.outbound() / inbound() / inboundOptionsFor(가는 편)  ← 같은 날 왕복이면 시간이 맞는 오는 편만
 * FlightSearchService.getAdultPrice(flight, cabin)                 ← 성인 1인 요금(운임+세금)
 * Flight: getFlightNumber(), getDepartureTime(), getArrivalTime(), getFlightTime(), getTimeBand(),
 *         countAvailableSeats(cabin), getAircraft(), getTypeName()
 * TimeBand: 새벽/오전/오후/저녁 (시간대 필터용)
 *
 * ── 지금 상태 (뼈대) ─────────────────────────────────────────
 *  [완료] 가는 편·오는 편 목록을 보여주고 고르면 세션에 저장
 *  TODO(A) 목록을 카드/표 형태로 (출발·도착 시각, 소요 시간, 기종, 남은 좌석, 요금)
 *  TODO(A) 시간대 필터(오전/오후/저녁)와 정렬(출발 시각순, 가격순)
 *  TODO(A) 왕복: 가는 편을 고르면 오는 편 목록이 시간이 맞는 것만 나오도록 (inboundOptionsFor)
 *  TODO(A) 선택한 항공권 합계 요금(성인 1인 기준, 왕복 합계) 표시
 */
public class FlightSelectPage extends BasePanel {

    private final DefaultListModel<Flight> outboundModel = new DefaultListModel<>();
    private final DefaultListModel<Flight> inboundModel = new DefaultListModel<>();
    private final JList<Flight> outboundList = new JList<>(outboundModel);
    private final JList<Flight> inboundList = new JList<>(inboundModel);
    private final JLabel outboundTitle = UiTheme.heading("가는 편");
    private final JPanel inboundPanel;

    public FlightSelectPage(AppContext app) {
        super(app, Screen.FLIGHT_SELECT);
        FlightRenderer renderer = new FlightRenderer();
        for (JList<Flight> list : List.of(outboundList, inboundList)) {
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setCellRenderer(renderer);
        }

        JPanel lists = new JPanel(new GridLayout(1, 2, 16, 0));
        lists.setOpaque(false);
        lists.add(listPanel(outboundTitle, outboundList));
        inboundPanel = listPanel(UiTheme.heading("오는 편"), inboundList);
        lists.add(inboundPanel);
        setContent(lists);

        addBackButton();
        JButton next = UiTheme.primaryButton("다음: 승객 정보");
        next.addActionListener(e -> goNext());
        addFooterButton(next);
    }

    @Override
    protected void onShow() {
        SearchResult result = session().getSearchResult();
        SearchCriteria criteria = result.criteria();
        setSubtitle(criteria.describe());
        fill(outboundModel, result.outbound());
        fill(inboundModel, result.inbound());
        inboundPanel.setVisible(criteria.isRoundTrip());
        outboundList.setSelectedValue(session().getOutbound(), true);
        inboundList.setSelectedValue(session().getInbound(), true);
    }

    private static void fill(DefaultListModel<Flight> model, List<Flight> flights) {
        model.clear();
        flights.forEach(model::addElement);
    }

    private void goNext() {
        Flight outbound = outboundList.getSelectedValue();
        if (outbound == null) {
            showWarning("항공권 선택", "가는 편 항공권을 선택하세요.");
            return;
        }
        session().selectOutbound(outbound);
        if (session().getCriteria().isRoundTrip()) {
            Flight inbound = inboundList.getSelectedValue();
            if (inbound == null) {
                showWarning("항공권 선택", "오는 편 항공권을 선택하세요.");
                return;
            }
            try {
                session().selectInbound(inbound);
            } catch (AirlineException e) {
                showError(e);
                return;
            }
        }
        app.navigate(Screen.PASSENGER);
    }

    private static JPanel listPanel(JLabel title, JList<Flight> list) {
        JPanel panel = UiTheme.card(new BorderLayout(0, 8));
        panel.add(title, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /** 목록 한 줄: "HS1205 · 12:10 → 13:20 (1시간 10분) · 오후 · 잔여 34석 · 89,000원" */
    private final class FlightRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Flight flight) {
                CabinClass cabin = session().getCriteria().cabin();
                label.setText(String.format("%s · %s → %s (%s) · %s · 잔여 %d석 · %s",
                        flight.getFlightNumber(), Formats.time(flight.getDepartureTime()),
                        Formats.time(flight.getArrivalTime()), Formats.duration(flight.getFlightTime()),
                        flight.getTimeBand(), flight.countAvailableSeats(cabin),
                        Formats.money(app.getSearchService().getAdultPrice(flight, cabin))));
            }
            label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            return label;
        }
    }
}
