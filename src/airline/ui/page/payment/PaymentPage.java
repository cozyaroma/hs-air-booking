package airline.ui.page.payment;

import airline.domain.booking.Booking;
import airline.domain.booking.PriceBreakdown;
import airline.domain.booking.SegmentType;
import airline.exception.AirlineException;
import airline.payment.PaymentMethod;
import airline.payment.SimplePayment;
import airline.service.BookingRequest;
import airline.service.BookingService;
import airline.ui.AppContext;
import airline.ui.BasePanel;
import airline.ui.Screen;
import airline.ui.UiTheme;
import airline.util.Formats;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

/**
 * [C 담당] 5단계: 결제 페이지.
 * 요구사항 전체: docs/pages/C-payment-booking.md
 *
 * ── 페이지 계약 ──────────────────────────────────────────────
 * 들어올 때 보장: 여정, 승객, 모든 구간 좌석이 세션에 있음 → session().toRequest()로 예약 요청을 만들 수 있음
 *               상단의 "선택한 항공권" 요약 바는 BasePanel이 자동으로 그린다
 * 나갈 때 할 일: PaymentMethod 객체를 만들고
 *               Booking b = app.getBookingService().createBooking(app.getCurrentUser(), session().toRequest(), 결제수단)
 *               session().completeBooking(b) → app.navigate(Screen.CONFIRMATION)
 *
 * ── 쓸 수 있는 공용 기능 ─────────────────────────────────────
 * BookingService.quote(request, 구간, 승객) / quoteSegment(request, 구간) / quoteTotal(request) → PriceBreakdown
 *   PriceBreakdown: fare(운임), tax(세금), seatFee(좌석), baggageFee(수하물), total()
 * session().setExtraBaggage(구간, 승객, 단위수)  +  flight.getBaggagePolicy() (무료 허용량, 단위 요금, 최대 단위)
 * 결제 수단 (생성자에서 형식 검사, 틀리면 InvalidPaymentInfoException):
 *   new CreditCardPayment(카드사, 카드번호, "MM/YY", CVC, 생년월일6, 비밀번호앞2, 할부개월)
 *   new BankTransferPayment(은행, 계좌번호, 예금주)
 *   new SimplePayment(제공사)
 *   목록: CreditCardPayment.CARD_COMPANIES, BankTransferPayment.BANKS, SimplePayment.PROVIDERS
 * PaymentFormats: digitsOnly, isLuhnValid, formatCardNumber, maskCardNumber, TEST_CARD_NUMBERS
 *
 * ── 지금 상태 (뼈대) ─────────────────────────────────────────
 *  [완료] 구간별 합계와 총액 표시, 간편결제로 예약 확정
 *  TODO(C) 결제 수단 선택: 신용카드(카드사, 카드번호, 유효기간, CVC, 생년월일 6자리, 비밀번호 앞 2자리, 할부)
 *          / 계좌이체(은행, 계좌번호, 예금주) / 간편결제
 *  TODO(C) 카드번호 4자리마다 띄우기, CVC·비밀번호는 가려서 입력(JPasswordField), "실제 카드 정보 입력 금지" 안내
 *  TODO(C) 구간별·승객별 추가 수하물 선택 → 금액 즉시 갱신
 *  TODO(C) 요금 상세표 (승객별 운임/세금/좌석/수하물), 결제 전 최종 확인 창, 규정 동의 체크
 */
public class PaymentPage extends BasePanel {

    private final JPanel summary = new JPanel();
    private final JComboBox<String> providerBox = new JComboBox<>(SimplePayment.PROVIDERS.toArray(new String[0]));
    private final JLabel totalLabel = UiTheme.title("");

    public PaymentPage(AppContext app) {
        super(app, Screen.PAYMENT);
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        summary.setOpaque(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        JPanel priceCard = UiTheme.card(new BorderLayout(0, 8));
        priceCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        priceCard.add(UiTheme.heading("요금"), BorderLayout.NORTH);
        priceCard.add(summary, BorderLayout.CENTER);
        priceCard.add(totalLabel, BorderLayout.SOUTH);
        content.add(priceCard);
        content.add(Box.createVerticalStrut(12));

        JPanel methodCard = UiTheme.card(new FlowLayout(FlowLayout.LEFT, 10, 0));
        methodCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        methodCard.add(UiTheme.heading("결제 수단"));
        methodCard.add(new JLabel("간편결제 (뼈대: TODO(C) 카드·계좌이체 추가)"));
        methodCard.add(providerBox);
        content.add(methodCard);
        setScrollableContent(content);

        addBackButton();
        JButton pay = UiTheme.primaryButton("결제하기");
        pay.addActionListener(e -> pay());
        addFooterButton(pay);
    }

    @Override
    protected void onShow() {
        BookingService service = app.getBookingService();
        BookingRequest request = session().toRequest();
        summary.removeAll();
        for (SegmentType segment : request.getItinerary().getSegments()) {
            PriceBreakdown price = service.quoteSegment(request, segment);
            summary.add(new JLabel(String.format("%s  운임 %s · 세금 %s · 좌석 %s · 수하물 %s  =  %s",
                    segment.getDisplayName(), Formats.money(price.fare()), Formats.money(price.tax()),
                    Formats.money(price.seatFee()), Formats.money(price.baggageFee()), Formats.money(price.total()))));
        }
        totalLabel.setText("총 결제 금액  " + Formats.money(service.quoteTotal(request).total()));
        summary.revalidate();
        summary.repaint();
    }

    private void pay() {
        try {
            PaymentMethod method = new SimplePayment((String) providerBox.getSelectedItem());
            long total = app.getBookingService().quoteTotal(session().toRequest()).total();
            if (!confirm("결제 확인", Formats.money(total) + "을 " + method.getDisplayName() + "로 결제할까요?")) {
                return;
            }
            Booking booking = app.getBookingService().createBooking(app.getCurrentUser(), session().toRequest(), method);
            session().completeBooking(booking);     // ← 페이지 계약: 예약 완료 페이지가 읽을 값
            app.navigate(Screen.CONFIRMATION);
        } catch (AirlineException e) {
            showError(e);
        }
    }
}
