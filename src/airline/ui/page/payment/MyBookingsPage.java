package airline.ui.page.payment;

import airline.domain.booking.Booking;
import airline.domain.booking.CancellationQuote;
import airline.domain.booking.SegmentType;
import airline.exception.AirlineException;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * [C 담당] 내 예약 (예약 조회·취소·체크인) 페이지. 헤더의 [내 예약] 탭으로 언제든 연다.
 * 요구사항 전체: docs/pages/C-payment-booking.md
 *
 * ── 페이지 계약 ──────────────────────────────────────────────
 * 들어올 때 보장: 로그인한 사용자 app.getCurrentUser() (세션 조건 없음)
 * 나갈 때 할 일: 없음 (다른 페이지로 데이터를 넘기지 않는다)
 *
 * ── 쓸 수 있는 공용 기능 ─────────────────────────────────────
 * BookingService.getMyBookings(user)                 ← 내 예약 목록 (출발 빠른 순)
 * BookingService.quoteCancellation(user, booking)    ← 취소 견적 (구간별 수수료, 환불액) / 불가하면 예외
 * BookingService.cancel(user, booking)               ← 취소 확정
 * BookingService.checkIn(user, booking, 구간)        ← 구간별 온라인 체크인 / 시간이 안 맞으면 예외
 * BookingSegment.isCheckInOpen(now), isCheckedIn()  ← 체크인 버튼 활성화
 * Booking.getStatusText(), getCancellation()
 * 헤더의 [시각 이동]으로 시간을 앞당겨 체크인·취소 수수료 변화를 시연할 수 있다 (페이지가 자동으로 다시 그려짐)
 *
 * ── 지금 상태 (뼈대) ─────────────────────────────────────────
 *  [완료] 내 예약 목록, 선택한 예약 내용, 취소(견적 확인 후), 구간 선택 체크인
 *  TODO(C) 목록을 카드 형태로 (상태별 색: 예약 확정/체크인/취소됨, 지난 여정 구분)
 *  TODO(C) 구간별 체크인 버튼 (체크인 가능 시간에만 활성화, 남은 시간 안내)
 *  TODO(C) 취소 견적 표 (구간별 남은 시간·수수료율·수수료, 환불 금액·환불 방법)
 *  TODO(C) 예약번호 검색, 탑승권(체크인 완료 후) 보기
 */
public class MyBookingsPage extends BasePanel {

    private final DefaultListModel<Booking> listModel = new DefaultListModel<>();
    private final JList<Booking> bookingList = new JList<>(listModel);
    private final JTextArea detail = new JTextArea();
    private final JButton cancelButton = UiTheme.dangerButton("예약 취소");
    private final JButton checkInButton = UiTheme.primaryButton("체크인");

    public MyBookingsPage(AppContext app) {
        super(app, Screen.MY_BOOKINGS);
        bookingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookingList.setCellRenderer(new BookingRenderer());
        bookingList.addListSelectionListener(e -> showDetail());
        detail.setEditable(false);
        detail.setFont(UiTheme.font(Font.PLAIN, 14));
        detail.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JScrollPane listScroll = new JScrollPane(bookingList);
        listScroll.setPreferredSize(new Dimension(360, 400));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, new JScrollPane(detail));
        split.setResizeWeight(0.35);
        split.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(split, BorderLayout.CENTER);
        setContent(content);

        cancelButton.addActionListener(e -> cancelSelected());
        checkInButton.addActionListener(e -> checkInSelected());
        addFooterButton(cancelButton);
        addFooterButton(checkInButton);
    }

    @Override
    protected void onShow() {
        Booking selected = bookingList.getSelectedValue();
        List<Booking> bookings = app.getBookingService().getMyBookings(app.getCurrentUser());
        listModel.clear();
        bookings.forEach(listModel::addElement);
        setSubtitle(app.getCurrentUser().getUsername() + " 님의 예약 " + bookings.size() + "건");
        if (selected != null && bookings.contains(selected)) {
            bookingList.setSelectedValue(selected, true);
        } else if (!bookings.isEmpty()) {
            bookingList.setSelectedIndex(0);
        }
        showDetail();
    }

    private void showDetail() {
        Booking booking = bookingList.getSelectedValue();
        detail.setText(booking == null ? "예약을 선택하세요." : ConfirmationPage.describe(booking));
        detail.setCaretPosition(0);
        cancelButton.setEnabled(booking != null);
        checkInButton.setEnabled(booking != null);
    }

    private void cancelSelected() {
        Booking booking = bookingList.getSelectedValue();
        try {
            CancellationQuote quote = app.getBookingService().quoteCancellation(app.getCurrentUser(), booking);
            StringBuilder message = new StringBuilder("예약 " + booking.getBookingCode() + "을(를) 취소할까요?\n\n");
            for (CancellationQuote.SegmentFee fee : quote.segmentFees()) {
                message.append(String.format("%s %s: 출발 %s 전, 수수료 %d%% = %s%n", fee.segment().getDisplayName(),
                        fee.flightNumber(), Formats.remaining(fee.timeBeforeDeparture()), fee.feeRatePercent(),
                        Formats.money(fee.fee())));
            }
            message.append("\n환불 예정 금액: ").append(Formats.money(quote.refundAmount()));
            if (!confirm("예약 취소", message.toString())) {
                return;
            }
            app.getBookingService().cancel(app.getCurrentUser(), booking);
            onShow();
        } catch (AirlineException e) {
            showError(e);
        }
    }

    private void checkInSelected() {
        Booking booking = bookingList.getSelectedValue();
        List<SegmentType> segments = booking.getItinerary().getSegments();
        SegmentType segment = segments.get(0);
        if (segments.size() > 1) {
            Object answer = JOptionPane.showInputDialog(this, "체크인할 구간을 선택하세요.", "체크인",
                    JOptionPane.QUESTION_MESSAGE, null, segments.toArray(), segments.get(0));
            if (answer == null) {
                return;
            }
            segment = (SegmentType) answer;
        }
        try {
            app.getBookingService().checkIn(app.getCurrentUser(), booking, segment);
            showInfo("체크인 완료", segment.getDisplayName() + " 체크인을 완료했습니다.");
            onShow();
        } catch (AirlineException e) {
            showError(e);
        }
    }

    /** 목록 한 줄: "ABC123 · 서울/김포 → 제주 왕복 · 11월 13일 (금) · 예약 확정" */
    private static final class BookingRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Booking booking) {
                label.setText(String.format("%s · %s · %s · %s", booking.getBookingCode(),
                        booking.getItinerary().getOutbound().getRoute() + " " + booking.getItinerary().getTripType(),
                        Formats.shortDate(booking.getFirstDeparture().toLocalDate()), booking.getStatusText()));
            }
            label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            return label;
        }
    }
}
