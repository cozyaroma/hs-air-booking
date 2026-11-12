package airline.ui.page.payment;

import airline.domain.booking.Booking;
import airline.domain.booking.BookingSegment;
import airline.domain.booking.Ticket;
import airline.domain.flight.Flight;
import airline.ui.AppContext;
import airline.ui.BasePanel;
import airline.ui.Screen;
import airline.ui.UiTheme;
import airline.util.Formats;

import javax.swing.JButton;
import javax.swing.JTextArea;
import java.awt.Font;

/**
 * [C 담당] 예약 완료 페이지.
 * 요구사항 전체: docs/pages/C-payment-booking.md
 *
 * ── 페이지 계약 ──────────────────────────────────────────────
 * 들어올 때 보장: session().getLastBooking()에 방금 만든 예약이 있음
 * 나갈 때 할 일: [내 예약 보기] → app.navigate(Screen.MY_BOOKINGS) / [새 예매] → app.startNewBooking()
 *
 * ── 지금 상태 (뼈대) ─────────────────────────────────────────
 *  [완료] 예약번호와 예약 내용을 글자로 보여줌
 *  TODO(C) 영수증/e-티켓 형태 레이아웃 (예약번호 크게, 구간별 카드, 승객별 좌석·e-티켓 번호)
 *  TODO(C) 결제 정보는 가려진 값(getMaskedInfo)만, 취소 규정 요약 안내
 */
public class ConfirmationPage extends BasePanel {

    private final JTextArea text = new JTextArea();

    public ConfirmationPage(AppContext app) {
        super(app, Screen.CONFIRMATION);
        text.setEditable(false);
        text.setFont(UiTheme.font(Font.PLAIN, 15));
        text.setBorder(UiTheme.cardBorder());
        setScrollableContent(text);

        JButton myBookings = UiTheme.secondaryButton("내 예약 보기");
        myBookings.addActionListener(e -> app.navigate(Screen.MY_BOOKINGS));
        JButton again = UiTheme.primaryButton("새 예매");
        again.addActionListener(e -> app.startNewBooking());
        addFooterButton(myBookings);
        addFooterButton(again);
    }

    @Override
    protected void onShow() {
        Booking booking = session().getLastBooking();
        setSubtitle("예약번호 " + booking.getBookingCode() + " · " + booking.getItinerary().describe());
        text.setText(describe(booking));
        text.setCaretPosition(0);
    }

    /** 예약 내용을 글자로 정리한다 (MyBookingsPage도 사용) */
    static String describe(Booking booking) {
        StringBuilder sb = new StringBuilder();
        sb.append("예약번호  ").append(booking.getBookingCode()).append("   (").append(booking.getStatusText()).append(")\n");
        sb.append("여정  ").append(booking.getItinerary().describe()).append("\n\n");
        for (BookingSegment segment : booking.getSegments()) {
            Flight flight = segment.getFlight();
            sb.append("[").append(segment.getType().getDisplayName()).append("] ")
                    .append(flight.getFlightNumber()).append("  ")
                    .append(Formats.dateTime(flight.getDepartureTime())).append(" ")
                    .append(flight.getOrigin()).append(" → ")
                    .append(Formats.time(flight.getArrivalTime())).append(" ").append(flight.getDestination())
                    .append(segment.isCheckedIn() ? "   체크인 완료" : "").append('\n');
            for (Ticket ticket : segment.getTickets()) {
                sb.append("    ").append(ticket.getPassenger()).append(" (").append(ticket.getPassenger().getGender())
                        .append(")  좌석 ").append(ticket.getSeatLabel())
                        .append("  e-티켓 ").append(ticket.getTicketNumber()).append('\n');
            }
            sb.append('\n');
        }
        sb.append("결제  ").append(booking.getPayment().methodName()).append("  ").append(booking.getPayment().maskedInfo())
                .append("\n승인번호  ").append(booking.getPayment().approvalNumber())
                .append("\n결제 금액  ").append(Formats.money(booking.getTotalPaid()));
        if (booking.getCancellation() != null) {
            sb.append("\n\n취소 일시  ").append(Formats.dateTime(booking.getCancelledAt()))
                    .append("\n취소 수수료  ").append(Formats.money(booking.getCancellation().totalFee()))
                    .append("\n환불 금액  ").append(Formats.money(booking.getCancellation().refundAmount()))
                    .append("  (").append(booking.getPaymentMethod().getRefundGuide()).append(")");
        }
        return sb.toString();
    }
}
