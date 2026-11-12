package airline.domain.booking;

import airline.domain.flight.Flight;
import airline.exception.CheckInNotAvailableException;
import airline.util.Formats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 예약 안의 한 구간 (가는 편 또는 오는 편).
 * 구간마다 항공권(운항편), 승객별 e-티켓, 체크인 여부를 가진다.
 */
public final class BookingSegment {

    private final SegmentType type;
    private final Flight flight;
    private final List<Ticket> tickets;
    private LocalDateTime checkedInAt;    // 체크인 전이면 null

    public BookingSegment(SegmentType type, Flight flight, List<Ticket> tickets) {
        this.type = Objects.requireNonNull(type);
        this.flight = Objects.requireNonNull(flight);
        if (tickets == null || tickets.isEmpty()) {
            throw new IllegalArgumentException("구간에 e-티켓이 하나 이상 있어야 합니다.");
        }
        this.tickets = List.copyOf(tickets);
    }

    /** 온라인 체크인. 국내선은 출발 24시간 전, 국제선은 48시간 전부터 가능하다. Booking에서만 호출한다. */
    void checkIn(LocalDateTime now) throws CheckInNotAvailableException {
        if (isCheckedIn()) {
            throw new CheckInNotAvailableException(type.getDisplayName() + "은 이미 체크인을 완료했습니다.");
        }
        if (now.isBefore(flight.getCheckInOpenTime())) {
            throw new CheckInNotAvailableException(String.format(
                    "%s(%s) %s 온라인 체크인은 출발 %d시간 전부터 가능합니다.\n체크인 시작: %s",
                    type.getDisplayName(), flight.getFlightNumber(), flight.getTypeName(),
                    flight.getCheckInOpenHours(), Formats.dateTime(flight.getCheckInOpenTime())));
        }
        if (!now.isBefore(flight.getBookingDeadline())) {
            throw new CheckInNotAvailableException(String.format(
                    "%s(%s) 체크인이 마감되었습니다. (%s 출발 %d분 전 마감)",
                    type.getDisplayName(), flight.getFlightNumber(), flight.getTypeName(),
                    flight.getCheckInCloseMinutes()));
        }
        checkedInAt = now;
    }

    /** 지금 체크인 버튼을 눌러도 되는 시간인지 (화면에서 버튼 활성화에 사용) */
    public boolean isCheckInOpen(LocalDateTime now) {
        return !isCheckedIn() && !now.isBefore(flight.getCheckInOpenTime()) && now.isBefore(flight.getBookingDeadline());
    }

    void releaseSeats() {
        for (Ticket ticket : tickets) {
            if (ticket.getSeat() != null) {
                ticket.getSeat().release();
            }
        }
    }

    public PriceBreakdown getPrice() {
        return tickets.stream().map(Ticket::getPrice).reduce(PriceBreakdown.ZERO, PriceBreakdown::plus);
    }

    public SegmentType getType() {
        return type;
    }

    public Flight getFlight() {
        return flight;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public boolean isCheckedIn() {
        return checkedInAt != null;
    }

    /** 체크인 전이면 null */
    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }
}
