package airline.dev;

import airline.domain.booking.Booking;
import airline.domain.booking.Itinerary;
import airline.domain.booking.SegmentType;
import airline.domain.flight.Airport;
import airline.domain.flight.Flight;
import airline.domain.passenger.Gender;
import airline.domain.passenger.Passenger;
import airline.domain.passenger.PassengerFactory;
import airline.domain.passenger.PassengerType;
import airline.domain.seat.CabinClass;
import airline.domain.user.User;
import airline.exception.AirlineException;
import airline.payment.CreditCardPayment;
import airline.payment.PaymentFormats;
import airline.service.AppServices;
import airline.service.BookingService;
import airline.service.SearchCriteria;
import airline.service.SearchResult;
import airline.ui.BookingSession;
import airline.ui.Screen;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 개발·테스트용 "미리 채운 세션" (공용 파일).
 *
 * 앞 페이지가 아직 완성되지 않아도 내 페이지를 열어볼 수 있도록,
 * 원하는 페이지의 입장 조건(Screen.check)을 만족하는 BookingSession을 만들어 준다.
 * PageLauncher(직접 눌러 보기)와 PageSmokeTest(자동 테스트)가 같은 데이터를 쓴다.
 */
public final class SessionFixtures {

    /** 준비된 상황들 */
    public enum Fixture {
        ONEWAY("국내선 편도 · 김포→제주 · 성인 1"),
        ROUNDTRIP("국내선 왕복 · 김포↔제주 · 성인 2 + 소아 1"),
        INTL("국제선 편도 · 인천→도쿄 · 성인 1 + 유아 1 (여권 필요)"),
        INTL_ROUNDTRIP("국제선 왕복 · 인천↔방콕 · 성인 2 + 소아 1 + 유아 1");

        private final String description;

        Fixture(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private SessionFixtures() {
    }

    /**
     * target 페이지를 바로 열 수 있게 세션을 채운다.
     * 예) SEAT → 검색 + 항공권 선택 + 승객 입력까지 끝난 세션
     */
    public static BookingSession prepare(AppServices services, User user, Fixture fixture, Screen target)
            throws AirlineException {
        BookingSession session = new BookingSession();
        if (target == Screen.SEARCH || target == Screen.MY_BOOKINGS) {
            return session;
        }
        LocalDate today = services.clock().today();
        SearchCriteria criteria = criteriaFor(fixture, today);
        SearchResult result = services.search().search(criteria);
        session.startSearch(criteria, result);
        if (target == Screen.FLIGHT_SELECT) {
            return session;
        }

        Flight outbound = result.outbound().get(0);
        session.selectOutbound(outbound);
        if (criteria.isRoundTrip()) {
            session.selectInbound(result.inboundOptionsFor(outbound).get(0));
        }
        if (target == Screen.PASSENGER) {
            return session;
        }

        BookingService bookingService = services.booking();
        Itinerary itinerary = session.requireItinerary();
        List<Passenger> passengers = passengersFor(fixture, today);
        bookingService.validatePassengers(itinerary, passengers);
        session.setPassengers(passengers);
        if (target == Screen.SEAT) {
            return session;
        }

        for (SegmentType segment : itinerary.getSegments()) {
            session.replaceSeats(segment, bookingService.autoAssignSeats(itinerary.getFlight(segment),
                    itinerary.getCabin(), session.getSeatedPassengers(), session.getSeats(segment)));
        }
        if (target == Screen.PAYMENT) {
            return session;
        }

        Booking booking = bookingService.createBooking(user, session.toRequest(), testCard(today));
        session.completeBooking(booking);
        return session;
    }

    /** 시연용 테스트 카드 (유효기간은 항상 3년 뒤) */
    private static CreditCardPayment testCard(LocalDate today) throws AirlineException {
        LocalDate expiry = today.plusYears(3);
        return new CreditCardPayment("신한카드", PaymentFormats.TEST_CARD_NUMBERS.get(0),
                String.format("%02d/%02d", expiry.getMonthValue(), expiry.getYear() % 100), "123", "950315", "12", 0);
    }

    private static SearchCriteria criteriaFor(Fixture fixture, LocalDate today) {
        return switch (fixture) {
            case ONEWAY -> SearchCriteria.oneWay(Airport.GMP, Airport.CJU, today.plusDays(2), CabinClass.ECONOMY, 1, 0, 0);
            case ROUNDTRIP -> SearchCriteria.roundTrip(Airport.GMP, Airport.CJU, today.plusDays(2), today.plusDays(5),
                    CabinClass.ECONOMY, 2, 1, 0);
            case INTL -> SearchCriteria.oneWay(Airport.ICN, Airport.NRT, today.plusDays(7), CabinClass.ECONOMY, 1, 0, 1);
            case INTL_ROUNDTRIP -> SearchCriteria.roundTrip(Airport.ICN, Airport.BKK, today.plusDays(7), today.plusDays(12),
                    CabinClass.ECONOMY, 2, 1, 1);
        };
    }

    private static List<Passenger> passengersFor(Fixture fixture, LocalDate today) throws AirlineException {
        List<Passenger> list = new ArrayList<>();
        switch (fixture) {
            case ONEWAY -> list.add(adult("HONG", "GILDONG", Gender.MALE, LocalDate.of(1995, 3, 15), null));
            case ROUNDTRIP -> {
                list.add(adult("HONG", "GILDONG", Gender.MALE, LocalDate.of(1995, 3, 15), null));
                list.add(adult("KIM", "YUNA", Gender.FEMALE, LocalDate.of(1996, 9, 5), null));
                list.add(PassengerFactory.create(PassengerType.CHILD, "HONG", "JIHO", Gender.MALE,
                        today.minusYears(9), null));
            }
            case INTL -> {
                list.add(adult("LEE", "SUJIN", Gender.FEMALE, LocalDate.of(1990, 5, 20), "M12345678"));
                list.add(PassengerFactory.create(PassengerType.INFANT, "LEE", "DOYUN", Gender.MALE,
                        today.minusMonths(8), "M23456789"));
            }
            case INTL_ROUNDTRIP -> {
                list.add(adult("PARK", "MINSU", Gender.MALE, LocalDate.of(1985, 1, 10), "M34567890"));
                list.add(adult("PARK", "JIYEON", Gender.FEMALE, LocalDate.of(1987, 4, 22), "M45678901"));
                list.add(PassengerFactory.create(PassengerType.CHILD, "PARK", "SEOJUN", Gender.MALE,
                        today.minusYears(7), "M56789012"));
                list.add(PassengerFactory.create(PassengerType.INFANT, "PARK", "HARIN", Gender.FEMALE,
                        today.minusYears(1), "M67890123"));
            }
        }
        return list;
    }

    private static Passenger adult(String last, String first, Gender gender, LocalDate birth, String passport)
            throws AirlineException {
        return PassengerFactory.create(PassengerType.ADULT, last, first, gender, birth, passport);
    }
}
