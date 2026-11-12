package airline.ui.common;

import airline.domain.booking.Itinerary;
import airline.domain.booking.SegmentType;
import airline.domain.flight.Flight;
import airline.service.SearchCriteria;
import airline.ui.BookingSession;
import airline.ui.UiTheme;
import airline.util.Formats;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * 승객 정보 ~ 결제 페이지 상단에 "내가 고른 항공권"을 보여주는 요약 바 (공용 부품).
 * BasePanel이 Screen.showsTicketSummary()가 true인 페이지에 자동으로 붙이고, 페이지가 보일 때마다 update()한다.
 * 페이지 담당자는 이 클래스를 직접 만들거나 고칠 필요가 없다.
 *
 *   [가는 편] HS1205  11월 13일 (금) 12:10 서울/김포(GMP) → 13:20 제주(CJU)  1시간 10분 · 국내선
 *   [오는 편] HS1206  11월 16일 (월) 14:20 제주(CJU) → 15:30 서울/김포(GMP)  1시간 10분 · 국내선
 *                                                      왕복 · 일반석 · 성인 2, 소아 1
 */
public class TicketSummaryBar extends JPanel {

    private static final Color BACKGROUND = new Color(0xEEF4FB);
    private static final Color TAG_OUT = new Color(0x1D4F91);
    private static final Color TAG_IN = new Color(0x2F7D6D);

    private final JPanel rows = new JPanel();
    private final JLabel tripLabel = new JLabel();

    public TicketSummaryBar() {
        super(new BorderLayout(16, 0));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC9D8EC)),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        tripLabel.setFont(UiTheme.font(Font.BOLD, 13));
        tripLabel.setForeground(UiTheme.PRIMARY_DARK);
        tripLabel.setVerticalAlignment(JLabel.CENTER);
        add(rows, BorderLayout.CENTER);
        add(tripLabel, BorderLayout.EAST);
    }

    /** 세션 내용으로 다시 그린다. 항공권을 아직 안 골랐으면 안내 문구만 보여준다. */
    public void update(BookingSession session) {
        rows.removeAll();
        Itinerary itinerary = session.getItinerary();
        SearchCriteria criteria = session.getCriteria();
        if (itinerary == null || criteria == null) {
            rows.add(UiTheme.muted("선택한 항공권이 없습니다."));
            tripLabel.setText("");
        } else {
            for (SegmentType segment : itinerary.getSegments()) {
                rows.add(createRow(segment, itinerary.getFlight(segment)));
                rows.add(Box.createVerticalStrut(4));
            }
            tripLabel.setText(itinerary.getTripType().getDisplayName() + " · "
                    + itinerary.getCabin().getDisplayName() + " · " + criteria.describePassengers());
        }
        setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
        revalidate();
        repaint();
    }

    private JPanel createRow(SegmentType segment, Flight flight) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        Color tagColor = segment == SegmentType.OUTBOUND ? TAG_OUT : TAG_IN;
        row.add(UiTheme.tag(segment.getDisplayName(), tagColor, Color.WHITE));

        JLabel number = new JLabel(flight.getFlightNumber());
        number.setFont(UiTheme.font(Font.BOLD, 14));
        row.add(number);

        JLabel schedule = new JLabel(String.format("%s  %s %s(%s) → %s %s(%s)",
                Formats.shortDate(flight.getDepartureDate()),
                Formats.time(flight.getDepartureTime()), flight.getOrigin().getCityName(), flight.getOrigin().getCode(),
                Formats.time(flight.getArrivalTime()), flight.getDestination().getCityName(), flight.getDestination().getCode()));
        schedule.setFont(UiTheme.font(Font.PLAIN, 14));
        schedule.setForeground(UiTheme.TEXT);
        row.add(schedule);

        row.add(UiTheme.muted(Formats.duration(flight.getFlightTime()) + " · " + flight.getTypeName()));
        return row;
    }
}
