package airline.domain.booking;

import airline.domain.passenger.Passenger;
import airline.domain.seat.Seat;

import java.util.Objects;

/**
 * 승객 한 명의 한 구간 발권 내역 (e-티켓).
 * 왕복 2명이면 가는 편 2장 + 오는 편 2장 = 4장이 생긴다.
 * 화면에서 "항공권"은 운항편(Flight)을 뜻하므로, 이 클래스는 화면에서 "e-티켓"이라고 부른다.
 */
public final class Ticket {

    private final String ticketNumber;
    private final Passenger passenger;
    private final Seat seat;                 // 유아는 null
    private final int extraBaggageUnits;
    private final PriceBreakdown price;

    public Ticket(String ticketNumber, Passenger passenger, Seat seat, int extraBaggageUnits, PriceBreakdown price) {
        this.ticketNumber = Objects.requireNonNull(ticketNumber);
        this.passenger = Objects.requireNonNull(passenger);
        this.price = Objects.requireNonNull(price);
        if (passenger.occupiesSeat() && seat == null) {
            throw new IllegalArgumentException(passenger.getFullName() + " 승객의 좌석이 없습니다.");
        }
        if (extraBaggageUnits < 0) {
            throw new IllegalArgumentException("추가 수하물 수량은 음수일 수 없습니다.");
        }
        this.seat = seat;
        this.extraBaggageUnits = extraBaggageUnits;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    /** 좌석이 없으면(유아) null */
    public Seat getSeat() {
        return seat;
    }

    public String getSeatLabel() {
        return seat == null ? "보호자 좌석" : seat.getCode();
    }

    public int getExtraBaggageUnits() {
        return extraBaggageUnits;
    }

    public PriceBreakdown getPrice() {
        return price;
    }
}
