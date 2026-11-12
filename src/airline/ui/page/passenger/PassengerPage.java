package airline.ui.page.passenger;

import airline.domain.booking.Itinerary;
import airline.domain.passenger.AgeCalculator;
import airline.domain.passenger.Gender;
import airline.domain.passenger.Passenger;
import airline.domain.passenger.PassengerFactory;
import airline.domain.passenger.PassengerType;
import airline.exception.AirlineException;
import airline.service.SearchCriteria;
import airline.ui.AppContext;
import airline.ui.BasePanel;
import airline.ui.Screen;
import airline.ui.UiTheme;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * [B 담당] 3단계: 승객 정보 입력 페이지.
 * 요구사항 전체: docs/pages/B-passenger-seat.md
 *
 * ── 페이지 계약 ──────────────────────────────────────────────
 * 들어올 때 보장: session().requireItinerary()로 여정(가는 편, 왕복이면 오는 편)을 얻을 수 있음
 *               session().getCriteria().toPassengerTypes() = 입력받을 승객 유형 순서 (성인→소아→유아)
 *               상단의 "선택한 항공권" 요약 바는 BasePanel이 자동으로 그린다
 * 나갈 때 할 일: 승객 목록을 만들어 app.getBookingService().validatePassengers(itinerary, 목록)으로 검증한 뒤
 *               session().setPassengers(목록) → app.navigate(Screen.SEAT)
 *
 * ── 쓸 수 있는 공용 기능 ─────────────────────────────────────
 * PassengerFactory.create(유형, 영문성, 영문이름, 성별, 생년월일, 여권번호)  ← 이름 형식이 틀리면 예외
 * AgeCalculator.parseBirthDate("19950315") / tryCalculate(문자열, 탑승일)    ← 만 나이 미리보기
 * Passenger.isValidPassportFormat(문자열)                                    ← 여권번호 즉시 검사
 * itinerary.requiresPassport()  ← 국제선이면 여권번호 입력칸을 보여준다
 *
 * ── 지금 상태 (뼈대) ─────────────────────────────────────────
 *  [완료] 승객 수만큼 입력줄을 만들고, 다음을 누르면 검증 후 세션에 저장
 *  TODO(B) 승객별 카드 형태 레이아웃 (승객 번호, 유형, 나이 기준 안내)
 *  TODO(B) 생년월일 입력 즉시 "탑승일 기준 만 N세" 표시, 유형과 안 맞으면 빨간 안내
 *  TODO(B) 영문 이름 자동 대문자, 잘못된 칸 강조, 유아-보호자 연결 안내
 *  TODO(B) 이전 페이지로 갔다 와도 입력값 유지 (지금은 저장된 승객만 다시 채움)
 */
public class PassengerPage extends BasePanel {

    private final JPanel formList = new JPanel();
    private final List<PassengerRow> rows = new ArrayList<>();
    private SearchCriteria builtFor;    // 어떤 검색 조건으로 입력줄을 만들었는지 (조건이 바뀌면 새로 만든다)

    public PassengerPage(AppContext app) {
        super(app, Screen.PASSENGER);
        formList.setLayout(new BoxLayout(formList, BoxLayout.Y_AXIS));
        formList.setOpaque(false);
        setScrollableContent(formList);

        addBackButton();
        JButton next = UiTheme.primaryButton("다음: 좌석 선택");
        next.addActionListener(e -> goNext());
        addFooterButton(next);
    }

    @Override
    protected void onShow() {
        SearchCriteria criteria = session().getCriteria();
        Itinerary itinerary = session().requireItinerary();
        if (criteria != builtFor) {
            buildRows(criteria, itinerary.requiresPassport());
            builtFor = criteria;
        }
        List<Passenger> saved = session().getPassengers();
        for (int i = 0; i < saved.size() && i < rows.size(); i++) {
            rows.get(i).fill(saved.get(i));
        }
    }

    private void buildRows(SearchCriteria criteria, boolean needsPassport) {
        formList.removeAll();
        rows.clear();
        List<PassengerType> types = criteria.toPassengerTypes();
        for (int i = 0; i < types.size(); i++) {
            PassengerRow row = new PassengerRow(i + 1, types.get(i), needsPassport);
            rows.add(row);
            formList.add(row.panel);
            formList.add(Box.createVerticalStrut(10));
        }
        formList.revalidate();
        formList.repaint();
    }

    private void goNext() {
        Itinerary itinerary = session().requireItinerary();
        try {
            List<Passenger> passengers = new ArrayList<>();
            for (PassengerRow row : rows) {
                passengers.add(row.toPassenger());
            }
            app.getBookingService().validatePassengers(itinerary, passengers);
            session().setPassengers(passengers);     // ← 페이지 계약: 좌석 페이지가 읽을 값
            app.navigate(Screen.SEAT);
        } catch (AirlineException e) {
            showError(e);
        }
    }

    /** 승객 한 명의 입력줄 */
    private static final class PassengerRow {

        private final PassengerType type;
        private final JPanel panel;
        private final JTextField lastName = new JTextField(8);
        private final JTextField firstName = new JTextField(10);
        private final JComboBox<Gender> gender = new JComboBox<>(Gender.values());
        private final JTextField birth = new JTextField(8);
        private final JTextField passport = new JTextField(9);

        PassengerRow(int number, PassengerType type, boolean needsPassport) {
            this.type = type;
            gender.setSelectedIndex(-1);
            panel = UiTheme.card(new FlowLayout(FlowLayout.LEFT, 8, 4));
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(UiTheme.heading("승객 " + number + " (" + type.getDisplayName() + ")"));
            panel.add(field("영문 성", lastName));
            panel.add(field("영문 이름", firstName));
            panel.add(field("성별", gender));
            panel.add(field("생년월일 8자리", birth));
            if (needsPassport) {
                panel.add(field("여권번호", passport));
            }
        }

        Passenger toPassenger() throws AirlineException {
            return PassengerFactory.create(type, lastName.getText(), firstName.getText(),
                    (Gender) gender.getSelectedItem(), AgeCalculator.parseBirthDate(birth.getText()),
                    passport.getText());
        }

        void fill(Passenger passenger) {
            lastName.setText(passenger.getLastName());
            firstName.setText(passenger.getFirstName());
            gender.setSelectedItem(passenger.getGender());
            birth.setText(passenger.getBirthDate().toString().replace("-", ""));
            passport.setText(passenger.getPassportNumber() == null ? "" : passenger.getPassportNumber());
        }

        private static JComponent field(String label, JComponent input) {
            JPanel box = new JPanel();
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setOpaque(false);
            JLabel title = UiTheme.muted(label);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            input.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.add(title);
            box.add(input);
            return box;
        }
    }
}
