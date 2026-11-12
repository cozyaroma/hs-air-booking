package airline.ui.page.search;

import airline.domain.booking.TripType;
import airline.domain.flight.Airport;
import airline.domain.passenger.PassengerType;
import airline.domain.seat.CabinClass;
import airline.exception.AirlineException;
import airline.service.FlightSearchService;
import airline.service.SearchCriteria;
import airline.service.SearchResult;
import airline.ui.AppContext;
import airline.ui.BasePanel;
import airline.ui.Screen;
import airline.ui.UiTheme;
import airline.util.Formats;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * [A 담당] 1단계: 항공권 검색 페이지.
 * 요구사항 전체: docs/pages/A-search.md
 *
 * ── 페이지 계약 ──────────────────────────────────────────────
 * 들어올 때 보장: 없음 (언제든 열 수 있음). 로그인한 사용자는 app.getCurrentUser()
 * 나갈 때 할 일: session().startSearch(criteria, result) 후 app.navigate(Screen.FLIGHT_SELECT)
 *   → FLIGHT_SELECT의 입장 조건(Screen.check) = "검색 조건과 결과가 세션에 있음"
 *
 * ── 쓸 수 있는 공용 기능 ─────────────────────────────────────
 * FlightSearchService: getOrigins(), getDestinations(origin), getFirstSearchableDate(),
 *                      getLastSearchableDate(), findLowestAdultPrice(...), search(criteria)
 * SearchCriteria.oneWay(...) / SearchCriteria.roundTrip(...)
 * AgeCalculator.calculate("19950315", 탑승일) → 만 나이와 국내선/국제선 승객 유형
 * PassengerType.getAgeGuide() → 인원 선택 옆 안내 문구
 *
 * ── 지금 상태 (뼈대) ─────────────────────────────────────────
 * 동작은 하지만 모양은 최소한이다. 아래 TODO를 A가 구현한다.
 *  [완료] 출발지를 고르면 그 출발지에서 갈 수 있는 도착지만 나온다 (getDestinations)
 *  TODO(A) 날짜 선택을 콤보박스 대신 달력으로 (지난 날짜·스케줄 없는 날짜는 선택 불가)
 *  TODO(A) 왕복이면 오는 날 달력, 편도면 숨기기 / 오는 날 ≥ 가는 날 즉시 안내
 *  TODO(A) 성인·소아·유아 옆에 나이 안내 + "나이 계산기" (생년월일 8자리 → 만 나이)
 *  TODO(A) 출발지·도착지 바꾸기(⇄) 버튼, 입력값 검증 메시지를 입력칸 근처에 표시
 */
public class SearchPage extends BasePanel {

    private static final int DATE_CHOICES = 30;

    private final JRadioButton oneWayButton = new JRadioButton("편도", true);
    private final JRadioButton roundTripButton = new JRadioButton("왕복");
    private final JComboBox<Airport> originBox = new JComboBox<>();
    private final JComboBox<Airport> destinationBox = new JComboBox<>();
    private final JComboBox<LocalDate> departureBox = new JComboBox<>();   // TODO(A): 달력으로 교체
    private final JComboBox<LocalDate> returnBox = new JComboBox<>();      // TODO(A): 달력으로 교체
    private final JComboBox<CabinClass> cabinBox = new JComboBox<>(CabinClass.values());
    private final JSpinner adultSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 9, 1));
    private final JSpinner childSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 8, 1));
    private final JSpinner infantSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 9, 1));

    public SearchPage(AppContext app) {
        super(app, Screen.SEARCH);
        ButtonGroup tripGroup = new ButtonGroup();
        tripGroup.add(oneWayButton);
        tripGroup.add(roundTripButton);
        oneWayButton.addActionListener(e -> returnBox.setEnabled(false));
        roundTripButton.addActionListener(e -> returnBox.setEnabled(true));
        returnBox.setEnabled(false);

        originBox.addActionListener(e -> reloadDestinations());
        DateRenderer renderer = new DateRenderer();
        departureBox.setRenderer(renderer);
        returnBox.setRenderer(renderer);

        JPanel form = UiTheme.card(new GridBagLayout());
        int row = 0;
        addRow(form, row++, "여정", flow(oneWayButton, roundTripButton));
        addRow(form, row++, "출발지", originBox);
        addRow(form, row++, "도착지", destinationBox);
        addRow(form, row++, "가는 날", departureBox);
        addRow(form, row++, "오는 날", returnBox);
        addRow(form, row++, "좌석 등급", cabinBox);
        addRow(form, row++, PassengerType.ADULT.getDisplayName(), flow(adultSpinner, UiTheme.muted(PassengerType.ADULT.getAgeGuide())));
        addRow(form, row++, PassengerType.CHILD.getDisplayName(), flow(childSpinner, UiTheme.muted(PassengerType.CHILD.getAgeGuide())));
        addRow(form, row, PassengerType.INFANT.getDisplayName(), flow(infantSpinner, UiTheme.muted(PassengerType.INFANT.getAgeGuide())));

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.add(form);
        setContent(wrapper);

        JButton searchButton = UiTheme.primaryButton("항공권 검색");
        searchButton.addActionListener(e -> search());
        addFooterButton(searchButton);
    }

    @Override
    protected void onShow() {
        FlightSearchService service = app.getSearchService();
        if (originBox.getItemCount() == 0) {
            originBox.setModel(new DefaultComboBoxModel<>(service.getOrigins().toArray(new Airport[0])));
            reloadDestinations();
        }
        reloadDates(service);   // "시각 이동"으로 오늘이 바뀌었을 수 있다
    }

    /** 출발지가 바뀌면 그 출발지에서 갈 수 있는 도착지만 다시 채운다. */
    private void reloadDestinations() {
        Airport origin = (Airport) originBox.getSelectedItem();
        List<Airport> destinations = origin == null ? List.of() : app.getSearchService().getDestinations(origin);
        destinationBox.setModel(new DefaultComboBoxModel<>(destinations.toArray(new Airport[0])));
    }

    private void reloadDates(FlightSearchService service) {
        LocalDate first = service.getFirstSearchableDate();
        LocalDate last = service.getLastSearchableDate();
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(last) && dates.size() < DATE_CHOICES; d = d.plusDays(1)) {
            dates.add(d);
        }
        Object keepDeparture = departureBox.getSelectedItem();
        Object keepReturn = returnBox.getSelectedItem();
        departureBox.setModel(new DefaultComboBoxModel<>(dates.toArray(new LocalDate[0])));
        returnBox.setModel(new DefaultComboBoxModel<>(dates.toArray(new LocalDate[0])));
        if (keepDeparture != null && dates.contains(keepDeparture)) {
            departureBox.setSelectedItem(keepDeparture);
        } else if (dates.size() > 1) {
            departureBox.setSelectedIndex(1);   // 기본: 내일
        }
        if (keepReturn != null && dates.contains(keepReturn)) {
            returnBox.setSelectedItem(keepReturn);
        } else if (dates.size() > 3) {
            returnBox.setSelectedIndex(3);
        }
    }

    private void search() {
        TripType tripType = roundTripButton.isSelected() ? TripType.ROUND_TRIP : TripType.ONE_WAY;
        SearchCriteria criteria = new SearchCriteria(tripType,
                (Airport) originBox.getSelectedItem(), (Airport) destinationBox.getSelectedItem(),
                (LocalDate) departureBox.getSelectedItem(), (LocalDate) returnBox.getSelectedItem(),
                (CabinClass) cabinBox.getSelectedItem(),
                (Integer) adultSpinner.getValue(), (Integer) childSpinner.getValue(), (Integer) infantSpinner.getValue());
        try {
            SearchResult result = app.getSearchService().search(criteria);
            if (!result.hasOptions()) {
                showInfo("검색 결과 없음", "조건에 맞는 항공권이 없습니다. 날짜나 인원을 바꿔 보세요.");
                return;
            }
            session().startSearch(criteria, result);    // ← 페이지 계약: 다음 페이지가 읽을 값
            app.navigate(Screen.FLIGHT_SELECT);
        } catch (AirlineException e) {
            showError(e);
        }
    }

    // ===== 배치 도우미 =====

    private static void addRow(JPanel form, int row, String label, Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(6, 4, 6, 16);
        c.anchor = GridBagConstraints.WEST;
        form.add(UiTheme.heading(label), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
    }

    private static JPanel flow(Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        for (Component component : components) {
            if (component instanceof JRadioButton radio) {
                radio.setOpaque(false);
            }
            panel.add(component);
        }
        return panel;
    }

    /** 콤보박스에 날짜를 "11월 13일 (금)"처럼 보여준다 */
    private static final class DateRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof LocalDate date) {
                label.setText(Formats.shortDate(date));
            }
            label.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            return label;
        }
    }
}
