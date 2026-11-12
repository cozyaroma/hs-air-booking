package airline;

import airline.dev.SessionFixtures.Fixture;
import airline.domain.booking.Booking;
import airline.domain.booking.BookingSegment;
import airline.domain.booking.BookingStatus;
import airline.domain.booking.CancellationQuote;
import airline.domain.booking.SegmentType;
import airline.domain.booking.Ticket;
import airline.domain.seat.Seat;
import airline.domain.user.User;
import airline.exception.BookingNotFoundException;
import airline.exception.CancellationNotAllowedException;
import airline.exception.CheckInNotAvailableException;
import airline.exception.InvalidPaymentInfoException;
import airline.exception.InvalidRequestException;
import airline.exception.PaymentFailedException;
import airline.payment.BankTransferPayment;
import airline.payment.CreditCardPayment;
import airline.payment.PaymentFormats;
import airline.payment.PaymentResult;
import airline.payment.SimplePayment;
import airline.service.BookingRequest;
import airline.service.BookingService;
import airline.ui.BookingSession;
import airline.ui.Screen;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static airline.TestSupport.check;
import static airline.TestSupport.equal;
import static airline.TestSupport.expectThrows;
import static airline.TestSupport.section;
import static airline.TestSupport.test;

/**
 * [C 담당 영역] 결제 정보, 예약 생성, 내 예약, 취소, 체크인.
 * C가 페이지에서 쓰는 공용 기능이 약속대로 동작하는지 확인한다.
 * C가 계산 로직을 별도 클래스로 만들면(예: 카드번호 입력 자동 띄어쓰기) 그 테스트도 이 파일에 추가한다.
 */
public final class PaymentBookingTest {

    private static final LocalDateTime NOW = TestWorld.START;
    private static final String TEST_CARD = "4111 1111 1111 1111";

    private PaymentBookingTest() {
    }

    public static void run() {
        section("C: 결제 정보 입력 규칙 (모의 결제)");

        test("카드번호 체크섬(Luhn)과 4자리 띄어쓰기·마스킹", () -> {
            check(PaymentFormats.isLuhnValid("4111111111111111"), "테스트 번호 통과");
            check(!PaymentFormats.isLuhnValid("4111111111111112"), "한 자리 틀리면 실패");
            equal("4111 1111 1111 1111", PaymentFormats.formatCardNumber("4111111111111111"), "띄어쓰기");
            equal("**** **** **** 1111", PaymentFormats.maskCardNumber("4111111111111111"), "마스킹");
        });

        test("신용카드: 형식이 틀리면 만들 수 없다 (번호, 유효기간, CVC, 생년월일, 비밀번호)", () -> {
            expectThrows(InvalidPaymentInfoException.class, () -> card("4111 1111 1111 1112", "12/30", "123", 0));
            expectThrows(InvalidPaymentInfoException.class, () -> card(TEST_CARD, "13/30", "123", 0));
            expectThrows(InvalidPaymentInfoException.class, () -> card(TEST_CARD, "12/30", "12", 0));
            expectThrows(InvalidPaymentInfoException.class, () -> new CreditCardPayment("신한카드", TEST_CARD,
                    "12/30", "123", "951315", "12", 0));
            expectThrows(InvalidPaymentInfoException.class, () -> new CreditCardPayment("신한카드", TEST_CARD,
                    "12/30", "123", "950315", "1", 0));
            expectThrows(InvalidPaymentInfoException.class, () -> card(TEST_CARD, "12/30", "123", 1));
        });

        test("신용카드: 결제 결과에는 가려진 번호만 남는다", () -> {
            PaymentResult result = card(TEST_CARD, "12/30", "123", 0).pay(100_000, NOW);
            check(result.approvalNumber().startsWith("CC"), "승인번호");
            check(result.maskedInfo().endsWith("1111") && !result.maskedInfo().contains("4111"), "마스킹: " + result.maskedInfo());
        });

        test("신용카드: 유효기간 지난 카드, 5만원 미만 할부는 결제 실패", () -> {
            expectThrows(PaymentFailedException.class, () -> card(TEST_CARD, "10/26", "123", 0).pay(100_000, NOW));
            expectThrows(PaymentFailedException.class, () -> card(TEST_CARD, "12/30", "123", 3).pay(49_000, NOW));
            card(TEST_CARD, "11/26", "123", 0).pay(100_000, NOW);   // 이번 달까지는 사용 가능
        });

        test("계좌이체: 계좌번호 10~14자리, 예금주 필수, 가려진 번호만", () -> {
            expectThrows(InvalidPaymentInfoException.class, () -> new BankTransferPayment("신한은행", "123-45", "홍길동"));
            expectThrows(InvalidPaymentInfoException.class, () -> new BankTransferPayment("신한은행", "110-123-456789", " "));
            expectThrows(InvalidPaymentInfoException.class, () -> new BankTransferPayment("신한은행", "110-12a-456789", "홍길동"));
            BankTransferPayment ok = new BankTransferPayment("신한은행", "110-123-456789", "홍길동");
            check(ok.getMaskedInfo().contains("6789") && !ok.getMaskedInfo().contains("110123"), "마스킹: " + ok.getMaskedInfo());
        });

        test("간편결제: 1회 한도 초과는 결제 실패", () -> {
            expectThrows(PaymentFailedException.class,
                    () -> new SimplePayment("카카오페이").pay(SimplePayment.LIMIT_PER_PAYMENT + 1, NOW));
        });

        section("C: 예약 생성");

        test("왕복 예약: 구간 2개 × 승객 3명 = e-티켓 6장, 결제 금액 = 견적 총액", () -> {
            TestWorld world = new TestWorld();
            User user = world.demoUser();
            BookingSession session = world.sessionFor(Fixture.ROUNDTRIP, Screen.PAYMENT);
            BookingService service = world.services.booking();
            BookingRequest request = session.toRequest();
            long expected = service.quoteTotal(request).total();
            Booking booking = service.createBooking(user, request, card(TEST_CARD, "12/30", "123", 0));
            equal(2, booking.getSegments().size(), "구간 수");
            equal(6, booking.getAllTickets().size(), "e-티켓 수");
            equal(expected, booking.getTotalPaid(), "결제 금액");
            for (Ticket ticket : booking.getAllTickets()) {
                check(ticket.getSeat() != null && !ticket.getSeat().isAvailable(), "좌석이 예약 상태");
            }
            check(service.getMyBookings(user).contains(booking), "내 예약 목록에 있음");
        });

        test("추가 수하물은 구간별로 요금에 더해지고, 한도를 넘으면 거절", () -> {
            TestWorld world = new TestWorld();
            BookingSession session = world.sessionFor(Fixture.ROUNDTRIP, Screen.PAYMENT);
            BookingService service = world.services.booking();
            long before = service.quoteTotal(session.toRequest()).total();
            var first = session.getSeatedPassengers().get(0);
            session.setExtraBaggage(SegmentType.OUTBOUND, first, 1);
            long fee = session.requireItinerary().getOutbound().getBaggagePolicy().getExtraFeePerUnit();
            equal(before + fee, service.quoteTotal(session.toRequest()).total(), "가는 편 수하물 1단위");
            session.setExtraBaggage(SegmentType.INBOUND, first, 9);
            expectThrows(InvalidRequestException.class,
                    () -> service.createBooking(world.demoUser(), session.toRequest(), new SimplePayment("토스페이")));
        });

        test("결제에 실패하면 잡았던 좌석을 모두 되돌린다", () -> {
            TestWorld world = new TestWorld();
            BookingSession session = world.sessionFor(Fixture.INTL_ROUNDTRIP, Screen.PAYMENT);
            BookingService service = world.services.booking();
            check(service.quoteTotal(session.toRequest()).total() > SimplePayment.LIMIT_PER_PAYMENT, "준비: 한도 초과 금액");
            Seat seat = session.getSeat(SegmentType.INBOUND, session.getSeatedPassengers().get(0));
            expectThrows(PaymentFailedException.class,
                    () -> service.createBooking(world.demoUser(), session.toRequest(), new SimplePayment("카카오페이")));
            check(seat.isAvailable(), "오는 편 좌석도 다시 판매 가능");
        });

        section("C: 내 예약 · 취소 · 체크인");

        test("시연용 계정에는 예약 2건(왕복 1, 국제선 편도 1)이 있다", () -> {
            TestWorld world = new TestWorld();
            List<Booking> bookings = world.services.booking().getMyBookings(world.demoUser());
            equal(2, bookings.size(), "예약 수");
            check(bookings.get(0).getItinerary().isRoundTrip(), "첫 번째는 왕복");
            check(bookings.get(1).getItinerary().requiresPassport(), "두 번째는 국제선");
        });

        test("다른 회원의 예약은 찾을 수도 취소할 수도 없다", () -> {
            TestWorld world = new TestWorld();
            Booking demoBooking = world.services.booking().getMyBookings(world.demoUser()).get(0);
            User other = world.services.auth().signUp("other01", "pass1234", "pass1234");
            check(world.services.booking().getMyBookings(other).isEmpty(), "남의 예약 안 보임");
            expectThrows(BookingNotFoundException.class,
                    () -> world.services.booking().findMyBooking(other, demoBooking.getBookingCode()));
            expectThrows(BookingNotFoundException.class, () -> world.services.booking().cancel(other, demoBooking));
        });

        test("취소 수수료는 구간마다 남은 시간으로 계산한다", () -> {
            TestWorld world = new TestWorld();
            User user = world.demoUser();
            BookingService service = world.services.booking();
            Booking booking = service.getMyBookings(user).get(0);   // 내일 출발, 4일 뒤 귀국 (국내선)
            CancellationQuote quote = service.quoteCancellation(user, booking);
            equal(10, quote.segmentFees().get(0).feeRatePercent(), "가는 편 (출발 1일 이상 전)");
            equal(10, quote.segmentFees().get(1).feeRatePercent(), "오는 편 (출발 4일 전)");
            world.clock.advance(Duration.ofHours(20));               // 가는 편 출발 24시간 이내로
            CancellationQuote later = service.quoteCancellation(user, booking);
            equal(20, later.segmentFees().get(0).feeRatePercent(), "가는 편 수수료 증가");
            equal(10, later.segmentFees().get(1).feeRatePercent(), "오는 편은 그대로");
            check(later.refundAmount() == booking.getTotalPaid() - later.totalFee(), "환불 = 결제 - 수수료");
        });

        test("취소하면 모든 구간 좌석이 풀리고, 두 번 취소할 수 없다", () -> {
            TestWorld world = new TestWorld();
            User user = world.demoUser();
            BookingService service = world.services.booking();
            Booking booking = service.getMyBookings(user).get(0);
            Seat inboundSeat = booking.getSegment(SegmentType.INBOUND).getTickets().get(0).getSeat();
            service.cancel(user, booking);
            equal(BookingStatus.CANCELLED, booking.getStatus(), "상태");
            check(inboundSeat.isAvailable(), "오는 편 좌석 반환");
            expectThrows(CancellationNotAllowedException.class, () -> service.cancel(user, booking));
        });

        test("체크인은 구간별로, 국내선 출발 24시간 전부터 / 체크인 후에는 온라인 취소 불가", () -> {
            TestWorld world = new TestWorld();
            User user = world.demoUser();
            BookingService service = world.services.booking();
            Booking booking = service.getMyBookings(user).get(0);
            BookingSegment outbound = booking.getSegment(SegmentType.OUTBOUND);
            check(!outbound.isCheckInOpen(world.clock.now()), "지금은 아직 (출발 27시간 전)");
            expectThrows(CheckInNotAvailableException.class, () -> service.checkIn(user, booking, SegmentType.OUTBOUND));
            world.clock.advance(Duration.ofHours(4));
            check(outbound.isCheckInOpen(world.clock.now()), "출발 24시간 이내");
            service.checkIn(user, booking, SegmentType.OUTBOUND);
            check(outbound.isCheckedIn(), "가는 편 체크인 완료");
            expectThrows(CheckInNotAvailableException.class, () -> service.checkIn(user, booking, SegmentType.INBOUND));
            equal("가는 편 체크인 완료", booking.getStatusText(), "상태 문구");
            expectThrows(CancellationNotAllowedException.class, () -> service.cancel(user, booking));
        });
    }

    private static CreditCardPayment card(String number, String expiry, String cvc, int months)
            throws InvalidPaymentInfoException {
        return new CreditCardPayment("신한카드", number, expiry, cvc, "950315", "12", months);
    }
}
