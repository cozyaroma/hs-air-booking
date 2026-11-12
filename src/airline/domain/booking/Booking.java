package airline.domain.booking;

import airline.domain.flight.Flight;
import airline.domain.passenger.Passenger;
import airline.domain.user.User;
import airline.exception.CancellationNotAllowedException;
import airline.exception.CheckInNotAvailableException;
import airline.payment.PaymentMethod;
import airline.payment.PaymentResult;
import airline.util.Formats;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 예약 한 건. 예약번호 하나에 여정(편도/왕복), 승객, 구간별 e-티켓, 결제 정보가 묶인다.
 *
 * [은닉성] 상태(status)는 private이고 cancel()과 checkIn()을 통해서만 바뀐다.
 *         각 메서드가 "이미 취소됨", "체크인 시간 아님" 같은 규칙을 스스로 검사하므로
 *         어디서 호출하든 잘못된 상태 변화가 일어나지 않는다.
 * [협력] 취소 수수료율은 각 구간의 Flight가 가진 CancellationPolicy에게 묻는다.
 *       Booking은 국내선인지 국제선인지 알 필요가 없다.
 */
public class Booking {

    private final String bookingCode;
    private final String ownerUsername;
    private final Itinerary itinerary;
    private final List<Passenger> passengers;
    private final List<BookingSegment> segments;
    private final PaymentMethod paymentMethod;
    private final PaymentResult payment;
    private final LocalDateTime bookedAt;

    private BookingStatus status = BookingStatus.CONFIRMED;
    private LocalDateTime cancelledAt;
    private CancellationQuote cancellation;

    public Booking(String bookingCode, String ownerUsername, Itinerary itinerary, List<Passenger> passengers,
                   List<BookingSegment> segments, PaymentMethod paymentMethod, PaymentResult payment,
                   LocalDateTime bookedAt) {
        this.bookingCode = Objects.requireNonNull(bookingCode);
        this.ownerUsername = Objects.requireNonNull(ownerUsername);
        this.itinerary = Objects.requireNonNull(itinerary);
        this.passengers = List.copyOf(passengers);
        if (segments == null || segments.size() != itinerary.getSegments().size()) {
            throw new IllegalArgumentException("여정의 구간 수와 예약 구간 수가 다릅니다.");
        }
        this.segments = List.copyOf(segments);   // 밖에서 목록을 바꿔도 예약 내용은 바뀌지 않는다
        this.paymentMethod = Objects.requireNonNull(paymentMethod);
        this.payment = Objects.requireNonNull(payment);
        this.bookedAt = Objects.requireNonNull(bookedAt);
    }

    // ===== 취소 =====

    /** 취소하면 얼마를 돌려받는지 미리 계산한다. 상태는 바꾸지 않는다. */
    public CancellationQuote quoteCancellation(LocalDateTime now) throws CancellationNotAllowedException {
        if (status == BookingStatus.CANCELLED) {
            throw new CancellationNotAllowedException("이미 취소된 예약입니다.");
        }
        if (segments.stream().anyMatch(BookingSegment::isCheckedIn)) {
            throw new CancellationNotAllowedException("체크인을 마친 구간이 있어 온라인으로 취소할 수 없습니다. 공항 카운터에 문의하세요.");
        }
        List<CancellationQuote.SegmentFee> fees = new ArrayList<>();
        for (BookingSegment segment : segments) {
            Flight flight = segment.getFlight();
            Duration timeBefore = Duration.between(now, flight.getDepartureTime());
            if (timeBefore.isNegative() || timeBefore.isZero()) {
                throw new CancellationNotAllowedException(
                        segment.getType().getDisplayName() + " 항공권이 이미 출발해서 온라인으로 취소할 수 없습니다.");
            }
            double rate = flight.getCancellationPolicy().getFeeRate(timeBefore);
            long fare = segment.getPrice().fare();
            fees.add(new CancellationQuote.SegmentFee(segment.getType(), flight.getFlightNumber(), timeBefore,
                    rate, fare, Formats.round100(fare * rate)));
        }
        return new CancellationQuote(fees, getTotalPaid());
    }

    /** 예약 전체를 취소하고 모든 구간의 좌석을 다시 판매 가능 상태로 돌린다. */
    public CancellationQuote cancel(LocalDateTime now) throws CancellationNotAllowedException {
        CancellationQuote quote = quoteCancellation(now);
        segments.forEach(BookingSegment::releaseSeats);
        changeStatus(BookingStatus.CANCELLED);
        cancelledAt = now;
        cancellation = quote;
        return quote;
    }

    // ===== 체크인 =====

    /** 구간 하나를 체크인한다. 가는 편과 오는 편은 체크인 가능 시간이 달라서 따로 한다. */
    public void checkIn(SegmentType segmentType, LocalDateTime now) throws CheckInNotAvailableException {
        if (status == BookingStatus.CANCELLED) {
            throw new CheckInNotAvailableException("취소된 예약은 체크인할 수 없습니다.");
        }
        getSegment(segmentType).checkIn(now);
    }

    private void changeStatus(BookingStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException(status + "에서 " + next + "(으)로 바꿀 수 없습니다.");
        }
        status = next;
    }

    // ===== 조회 =====

    public boolean isOwnedBy(User user) {
        return user != null && ownerUsername.equals(user.getUsername());
    }

    /** @throws IllegalArgumentException 편도 예약에 오는 편을 요청한 경우 */
    public BookingSegment getSegment(SegmentType type) {
        return segments.stream()
                .filter(segment -> segment.getType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("이 예약에는 " + type.getDisplayName() + "이 없습니다."));
    }

    /** 모든 구간의 e-티켓 (가는 편 → 오는 편 순서) */
    public List<Ticket> getAllTickets() {
        List<Ticket> all = new ArrayList<>();
        segments.forEach(segment -> all.addAll(segment.getTickets()));
        return all;
    }

    public PriceBreakdown getTotalPrice() {
        return segments.stream().map(BookingSegment::getPrice).reduce(PriceBreakdown.ZERO, PriceBreakdown::plus);
    }

    public long getTotalPaid() {
        return payment.amount();
    }

    public LocalDateTime getFirstDeparture() {
        return segments.get(0).getFlight().getDepartureTime();
    }

    /** 목록 표시용 상태 문구 */
    public String getStatusText() {
        if (status == BookingStatus.CANCELLED) {
            return status.getDisplayName();
        }
        long checkedIn = segments.stream().filter(BookingSegment::isCheckedIn).count();
        if (checkedIn == 0) {
            return status.getDisplayName();
        }
        if (checkedIn == segments.size()) {
            return "체크인 완료";
        }
        return segments.stream().filter(BookingSegment::isCheckedIn)
                .map(segment -> segment.getType().getDisplayName())
                .findFirst().orElse("") + " 체크인 완료";
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public Itinerary getItinerary() {
        return itinerary;
    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    public List<BookingSegment> getSegments() {
        return segments;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentResult getPayment() {
        return payment;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    /** 취소 전이면 null */
    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    /** 취소 전이면 null */
    public CancellationQuote getCancellation() {
        return cancellation;
    }
}
