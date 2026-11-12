package airline.service;

import airline.domain.booking.Booking;
import airline.domain.booking.BookingSegment;
import airline.domain.booking.CancellationQuote;
import airline.domain.booking.Itinerary;
import airline.domain.booking.PriceBreakdown;
import airline.domain.booking.SegmentType;
import airline.domain.booking.Ticket;
import airline.domain.flight.Flight;
import airline.domain.passenger.AdultPassenger;
import airline.domain.passenger.InfantPassenger;
import airline.domain.passenger.Passenger;
import airline.domain.seat.CabinClass;
import airline.domain.seat.Seat;
import airline.domain.user.User;
import airline.exception.AirlineException;
import airline.exception.BookingClosedException;
import airline.exception.BookingNotFoundException;
import airline.exception.InvalidPassengerException;
import airline.exception.InvalidRequestException;
import airline.exception.SeatRestrictionException;
import airline.exception.SeatUnavailableException;
import airline.payment.PaymentMethod;
import airline.payment.PaymentResult;
import airline.policy.BaggagePolicy;
import airline.repository.BookingRepository;
import airline.util.AppClock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 예약 흐름을 조율하는 서비스: 승객 검증 → 좌석 확인 → 요금 계산 → 결제 → 예약 저장 → 조회·취소·체크인.
 *
 * 규칙 자체는 각 객체가 가지고 있고(나이 규칙은 Passenger, 착석 제한은 Seat, 취소 규정은 Flight),
 * 이 클래스는 순서를 정하고 객체들을 협력시키는 역할만 한다. 화면(Swing)은 전혀 모른다.
 */
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FareCalculator fareCalculator;
    private final BookingCodeGenerator codeGenerator;
    private final AppClock clock;

    public BookingService(BookingRepository bookingRepository, FareCalculator fareCalculator,
                          BookingCodeGenerator codeGenerator, AppClock clock) {
        this.bookingRepository = bookingRepository;
        this.fareCalculator = fareCalculator;
        this.codeGenerator = codeGenerator;
        this.clock = clock;
    }

    // ===== 승객 (B 담당 화면이 사용) =====

    /**
     * 승객 정보 단계 검증: 여정의 모든 구간에 대해 나이·여권을 확인하고 유아와 보호자를 연결한다.
     * 왕복이면 오는 편 날짜 기준으로도 나이를 다시 확인한다 (그 사이 생일이 지나 유형이 바뀔 수 있음).
     */
    public void validatePassengers(Itinerary itinerary, List<Passenger> passengers) throws AirlineException {
        if (passengers.isEmpty()) {
            throw new InvalidPassengerException("탑승객 정보가 없습니다.");
        }
        for (Flight flight : itinerary.getFlights()) {
            for (Passenger passenger : passengers) {
                passenger.validateAge(flight);         // 성인/소아/유아가 각자 자기 규칙으로 검사
                passenger.validateDocuments(flight);   // 국제선이면 여권 확인
            }
            assignGuardians(flight, passengers);
        }
    }

    /** 유아마다 보호자가 될 수 있는 성인을 입력 순서대로 한 명씩 연결한다. */
    private void assignGuardians(Flight flight, List<Passenger> passengers) throws InvalidPassengerException {
        List<AdultPassenger> adults = new ArrayList<>();
        for (Passenger p : passengers) {
            if (p instanceof AdultPassenger adult) {
                adults.add(adult);
            }
        }
        for (Passenger p : passengers) {
            if (!(p instanceof InfantPassenger infant)) {
                continue;
            }
            if (infant.getGuardian() != null && adults.contains(infant.getGuardian())) {
                infant.getGuardian().attachInfant(infant, flight);   // 이미 연결됨: 이 구간에서도 보호자 나이 확인
                continue;
            }
            AdultPassenger guardian = adults.stream()
                    .filter(a -> !a.isAccompanyingInfant() && a.canBeGuardianOn(flight))
                    .findFirst()
                    .orElseThrow(() -> new InvalidPassengerException(String.format(
                            "유아 %s 승객을 동반할 보호자가 없습니다.\n%s 유아 보호자는 탑승일 기준 만 %d세 이상 성인이어야 하며, 성인 1명당 유아 1명까지 가능합니다.",
                            infant.getFullName(), flight.getTypeName(), flight.getGuardianMinAge())));
            guardian.attachInfant(infant, flight);
        }
    }

    // ===== 좌석 (B 담당 화면이 사용) =====

    /** 이 승객이 이 좌석을 고를 수 있는지 확인한다. 문제가 있으면 이유를 담은 예외를 던진다. */
    public void checkSeat(Flight flight, CabinClass cabin, Passenger passenger, Seat seat) throws AirlineException {
        if (seat.getCabin() != cabin) {
            throw new SeatRestrictionException(String.format(
                    "%s 좌석은 %s입니다. 예약한 등급(%s)의 좌석을 선택하세요.",
                    seat.getCode(), seat.getCabin(), cabin));
        }
        if (!seat.isAvailable()) {
            throw new SeatUnavailableException(seat.getCode() + " 좌석은 이미 예약되었습니다. 다른 좌석을 선택하세요.");
        }
        seat.checkOccupant(passenger, flight.getDepartureDate());   // 좌석 종류별 착석 규칙 (다형성)
    }

    /**
     * 좌석을 아직 고르지 않은 승객에게 앞쪽부터 빈 좌석을 차례로 배정한다 (구간 하나).
     * 추가 요금이 있는 좌석(비상구석)은 자동 배정하지 않는다.
     */
    public Map<Passenger, Seat> autoAssignSeats(Flight flight, CabinClass cabin, List<Passenger> passengers,
                                                Map<Passenger, Seat> alreadyAssigned) throws AirlineException {
        Map<Passenger, Seat> result = new LinkedHashMap<>(alreadyAssigned);
        Set<Seat> taken = new HashSet<>(alreadyAssigned.values());
        for (Passenger passenger : passengers) {
            if (!passenger.occupiesSeat() || result.containsKey(passenger)) {
                continue;
            }
            Seat chosen = null;
            for (Seat seat : flight.getSeats()) {
                if (seat.getCabin() != cabin || !seat.isAvailable() || seat.getExtraFee() > 0 || taken.contains(seat)) {
                    continue;
                }
                chosen = seat;
                break;
            }
            if (chosen == null) {
                throw new SeatUnavailableException(flight.getFlightNumber() + " " + cabin + "에 자동 배정할 빈 좌석이 부족합니다.");
            }
            result.put(passenger, chosen);
            taken.add(chosen);
        }
        return result;
    }

    // ===== 요금 (C 담당 화면이 사용) =====

    /** 승객 한 명의 한 구간 요금 */
    public PriceBreakdown quote(BookingRequest request, SegmentType segment, Passenger passenger) {
        Itinerary itinerary = request.getItinerary();
        return fareCalculator.calculate(itinerary.getFlight(segment), itinerary.getCabin(), passenger,
                request.getSeat(segment, passenger), request.getExtraBaggage(segment, passenger));
    }

    /** 한 구간 전체 승객의 요금 합계 */
    public PriceBreakdown quoteSegment(BookingRequest request, SegmentType segment) {
        PriceBreakdown total = PriceBreakdown.ZERO;
        for (Passenger passenger : request.getPassengers()) {
            total = total.plus(quote(request, segment, passenger));
        }
        return total;
    }

    /** 결제할 총액 (모든 구간, 모든 승객) */
    public PriceBreakdown quoteTotal(BookingRequest request) {
        PriceBreakdown total = PriceBreakdown.ZERO;
        for (SegmentType segment : request.getItinerary().getSegments()) {
            total = total.plus(quoteSegment(request, segment));
        }
        return total;
    }

    // ===== 예약 생성 (C 담당 화면이 사용) =====

    /**
     * 예약을 확정한다.
     * 모든 구간의 좌석을 먼저 잡고 결제하며, 결제가 실패하면 잡았던 좌석을 모두 되돌린다.
     */
    public Booking createBooking(User owner, BookingRequest request, PaymentMethod paymentMethod)
            throws AirlineException {
        Objects.requireNonNull(owner, "로그인한 사용자");
        Objects.requireNonNull(paymentMethod, "결제 수단");
        LocalDateTime now = clock.now();
        Itinerary itinerary = request.getItinerary();

        for (Flight flight : itinerary.getFlights()) {
            if (!flight.isBookable(now)) {
                throw new BookingClosedException(String.format("%s 항공권은 예약이 마감되었습니다. (%s 출발 %d분 전 마감)",
                        flight.getFlightNumber(), flight.getTypeName(), flight.getCheckInCloseMinutes()));
            }
        }
        validatePassengers(itinerary, request.getPassengers());
        Map<SegmentType, Set<Seat>> seatsToReserve = new LinkedHashMap<>();
        for (SegmentType segment : itinerary.getSegments()) {
            seatsToReserve.put(segment, validateSeatsAndBaggage(request, segment));
        }

        List<Seat> reserved = new ArrayList<>();
        try {
            for (Set<Seat> seats : seatsToReserve.values()) {
                for (Seat seat : seats) {
                    seat.reserve();
                    reserved.add(seat);
                }
            }
            List<BookingSegment> segments = new ArrayList<>();
            for (SegmentType segment : itinerary.getSegments()) {
                List<Ticket> tickets = new ArrayList<>();
                for (Passenger passenger : request.getPassengers()) {
                    tickets.add(new Ticket(codeGenerator.nextTicketNumber(), passenger,
                            request.getSeat(segment, passenger), request.getExtraBaggage(segment, passenger),
                            quote(request, segment, passenger)));
                }
                segments.add(new BookingSegment(segment, itinerary.getFlight(segment), tickets));
            }
            long total = segments.stream().mapToLong(s -> s.getPrice().total()).sum();
            PaymentResult payment = paymentMethod.pay(total, now);

            String code = codeGenerator.nextBookingCode(bookingRepository::exists);
            Booking booking = new Booking(code, owner.getUsername(), itinerary, request.getPassengers(),
                    segments, paymentMethod, payment, now);
            bookingRepository.save(booking);
            return booking;
        } catch (AirlineException | RuntimeException e) {
            reserved.forEach(Seat::release);   // 실패하면 좌석을 다시 판매 가능 상태로
            throw e;
        }
    }

    private Set<Seat> validateSeatsAndBaggage(BookingRequest request, SegmentType segment) throws AirlineException {
        Itinerary itinerary = request.getItinerary();
        Flight flight = itinerary.getFlight(segment);
        BaggagePolicy baggagePolicy = flight.getBaggagePolicy();
        Set<Seat> seats = new LinkedHashSet<>();
        for (Passenger passenger : request.getPassengers()) {
            Seat seat = request.getSeat(segment, passenger);
            int bags = request.getExtraBaggage(segment, passenger);
            if (bags < 0 || bags > baggagePolicy.getMaxExtraUnits()) {
                throw new InvalidRequestException(String.format("추가 수하물은 1인당 구간별 최대 %d단위(%s)까지 구매할 수 있습니다.",
                        baggagePolicy.getMaxExtraUnits(), baggagePolicy.getExtraUnitName()));
            }
            if (!passenger.occupiesSeat()) {
                if (seat != null) {
                    throw new SeatRestrictionException("유아는 좌석을 배정받지 않습니다.");
                }
                if (bags > 0) {
                    throw new InvalidRequestException("유아 승객은 추가 수하물을 구매할 수 없습니다.");
                }
                continue;
            }
            if (seat == null) {
                throw new InvalidRequestException(segment.getDisplayName() + " " + passenger.getFullName() + " 승객의 좌석을 선택하세요.");
            }
            if (flight.findSeat(seat.getCode()).orElse(null) != seat) {
                throw new SeatRestrictionException(seat.getCode() + " 좌석은 " + segment.getDisplayName() + " 항공권의 좌석이 아닙니다.");
            }
            if (!seats.add(seat)) {
                throw new SeatUnavailableException(seat.getCode() + " 좌석을 두 명이 함께 선택했습니다.");
            }
            checkSeat(flight, itinerary.getCabin(), passenger, seat);
        }
        return seats;
    }

    // ===== 내 예약 조회·취소·체크인 (C 담당 화면이 사용) =====

    /** 로그인한 회원의 예약 목록 (출발이 빠른 순) */
    public List<Booking> getMyBookings(User user) {
        return bookingRepository.findByOwner(user.getUsername()).stream()
                .sorted(Comparator.comparing(Booking::getFirstDeparture))
                .toList();
    }

    /**
     * 예약번호로 내 예약을 찾는다.
     * 번호가 없을 때와 다른 사람의 예약일 때 같은 메시지를 보여줘서 남의 예약번호 존재 여부를 알 수 없게 한다.
     */
    public Booking findMyBooking(User user, String bookingCode) throws BookingNotFoundException {
        if (bookingCode == null || bookingCode.isBlank()) {
            throw new BookingNotFoundException("예약번호를 입력하세요.");
        }
        return bookingRepository.findByCode(bookingCode.trim().toUpperCase())
                .filter(booking -> booking.isOwnedBy(user))
                .orElseThrow(() -> new BookingNotFoundException("예약 정보를 찾을 수 없습니다. 예약번호를 확인하세요."));
    }

    public CancellationQuote quoteCancellation(User user, Booking booking) throws AirlineException {
        requireOwner(user, booking);
        return booking.quoteCancellation(clock.now());
    }

    public CancellationQuote cancel(User user, Booking booking) throws AirlineException {
        requireOwner(user, booking);
        return booking.cancel(clock.now());
    }

    public void checkIn(User user, Booking booking, SegmentType segment) throws AirlineException {
        requireOwner(user, booking);
        booking.checkIn(segment, clock.now());
    }

    /** 다른 회원의 예약은 존재하지 않는 것처럼 처리한다 */
    private void requireOwner(User user, Booking booking) throws BookingNotFoundException {
        if (!booking.isOwnedBy(user)) {
            throw new BookingNotFoundException("예약 정보를 찾을 수 없습니다.");
        }
    }

    /** 체크인·취소 화면에서 현재 시각이 필요할 때 */
    public LocalDateTime now() {
        return clock.now();
    }
}
