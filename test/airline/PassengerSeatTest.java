package airline;

import airline.dev.SessionFixtures.Fixture;
import airline.domain.booking.Itinerary;
import airline.domain.booking.SegmentType;
import airline.domain.flight.AircraftType;
import airline.domain.flight.Airport;
import airline.domain.flight.DomesticFlight;
import airline.domain.flight.Flight;
import airline.domain.flight.InternationalFlight;
import airline.domain.flight.Route;
import airline.domain.passenger.AdultPassenger;
import airline.domain.passenger.Gender;
import airline.domain.passenger.InfantPassenger;
import airline.domain.passenger.Passenger;
import airline.domain.passenger.PassengerFactory;
import airline.domain.passenger.PassengerType;
import airline.domain.seat.CabinClass;
import airline.domain.seat.ExitRowSeat;
import airline.domain.seat.Seat;
import airline.exception.InvalidPassengerException;
import airline.exception.SeatRestrictionException;
import airline.exception.SeatUnavailableException;
import airline.service.BookingService;
import airline.ui.BookingSession;
import airline.ui.Screen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static airline.TestSupport.check;
import static airline.TestSupport.equal;
import static airline.TestSupport.expectThrows;
import static airline.TestSupport.section;
import static airline.TestSupport.test;

/**
 * [B 담당 영역] 승객 정보 규칙과 좌석 규칙.
 * B가 페이지에서 쓰는 공용 기능이 약속대로 동작하는지 확인한다.
 * B가 계산 로직을 별도 클래스로 만들면(예: 좌석 배치 좌표 계산) 그 테스트도 이 파일에 추가한다.
 */
public final class PassengerSeatTest {

    private static final Route GMP_CJU = new Route(Airport.GMP, Airport.CJU, 70, 68_000, 8_800);
    private static final Route ICN_NRT = new Route(Airport.ICN, Airport.NRT, 140, 240_000, 23_000);
    private static final LocalDateTime DEPARTURE = LocalDateTime.of(2026, 11, 20, 10, 0);

    private PassengerSeatTest() {
    }

    public static void run() {
        section("B: 승객 정보 (이름·성별·생년월일·여권)");

        test("성별까지 입력해야 승객이 만들어진다", () -> {
            Passenger p = PassengerFactory.create(PassengerType.ADULT, " hong ", "gil  dong", Gender.MALE,
                    LocalDate.of(1995, 3, 15), null);
            equal("HONG/GIL DONG", p.getFullName(), "영문 이름 정리(대문자, 공백 하나)");
            equal(Gender.MALE, p.getGender(), "성별");
            expectThrows(InvalidPassengerException.class, () -> PassengerFactory.create(PassengerType.ADULT,
                    "HONG", "GILDONG", null, LocalDate.of(1995, 3, 15), null));
            expectThrows(InvalidPassengerException.class, () -> PassengerFactory.create(PassengerType.ADULT,
                    "홍", "길동", Gender.MALE, LocalDate.of(1995, 3, 15), null));
        });

        test("국제선은 여권번호가 필요하고 형식을 검사한다", () -> {
            Flight intl = new InternationalFlight("T701", ICN_NRT, DEPARTURE, AircraftType.B737_8, 240_000);
            Passenger noPassport = adult("KIM", LocalDate.of(1990, 1, 1), null);
            expectThrows(InvalidPassengerException.class, () -> noPassport.validateDocuments(intl));
            Passenger badPassport = adult("KIM", LocalDate.of(1990, 1, 1), "12345");
            expectThrows(InvalidPassengerException.class, () -> badPassport.validateDocuments(intl));
            check(Passenger.isValidPassportFormat("m123a4567"), "소문자 입력도 형식 검사 통과");
        });

        test("왕복이면 오는 편 날짜 기준으로도 나이를 다시 확인한다", () -> {
            TestWorld world = new TestWorld();
            BookingSession session = world.sessionFor(Fixture.ROUNDTRIP, Screen.PASSENGER);
            Itinerary itinerary = session.requireItinerary();
            LocalDate returnDate = itinerary.getInbound().getDepartureDate();
            // 가는 날에는 만 12세(소아), 오는 날에는 만 13세(국내선 성인)가 되는 아이
            LocalDate birth = returnDate.minusYears(13);
            List<Passenger> passengers = List.of(
                    adult("HONG", LocalDate.of(1990, 1, 1), null),
                    adult("KIM", LocalDate.of(1991, 1, 1), null),
                    PassengerFactory.create(PassengerType.CHILD, "HONG", "MINA", Gender.FEMALE, birth, null));
            InvalidPassengerException e = expectThrows(InvalidPassengerException.class,
                    () -> world.services.booking().validatePassengers(itinerary, passengers));
            check(e.getMessage().contains(returnDate.toString()), "오는 편 날짜가 메시지에: " + e.getMessage());
        });

        test("유아는 보호자 성인과 연결된다 (국제선 보호자는 만 18세 이상)", () -> {
            Flight intl = new InternationalFlight("T701", ICN_NRT, DEPARTURE, AircraftType.B737_8, 240_000);
            Itinerary itinerary = Itinerary.oneWay(intl, CabinClass.ECONOMY);
            BookingService service = new TestWorld().services.booking();
            Passenger teen = adult("LEE", DEPARTURE.toLocalDate().minusYears(16), "M12345678");   // 국제선 성인이지만 보호자 불가
            Passenger infant = infant(DEPARTURE.toLocalDate().minusMonths(6));
            expectThrows(InvalidPassengerException.class, () -> service.validatePassengers(itinerary, List.of(teen, infant)));

            Passenger parent = adult("LEE", LocalDate.of(1990, 1, 1), "M23456789");
            Passenger infant2 = infant(DEPARTURE.toLocalDate().minusMonths(6));
            service.validatePassengers(itinerary, List.of(parent, infant2));
            equal(parent, ((InfantPassenger) infant2).getGuardian(), "보호자 연결");
        });

        section("B: 좌석 (구간별)");

        test("자동 배정: 좌석이 필요한 승객만, 겹치지 않게, 비상구 제외", () -> {
            Flight flight = new DomesticFlight("T100", GMP_CJU, DEPARTURE, AircraftType.A321NEO, 68_000);
            Passenger a = adult("HONG", LocalDate.of(1990, 1, 1), null);
            Passenger b = adult("KIM", LocalDate.of(1991, 1, 1), null);
            Passenger baby = infant(DEPARTURE.toLocalDate().minusMonths(6));
            Map<Passenger, Seat> seats = new TestWorld().services.booking()
                    .autoAssignSeats(flight, CabinClass.ECONOMY, List.of(a, b, baby), Map.of());
            equal(2, seats.size(), "유아 제외 2명");
            equal(2, new HashSet<>(seats.values()).size(), "서로 다른 좌석");
            check(seats.values().stream().noneMatch(s -> s instanceof ExitRowSeat), "비상구 자동 배정 안 함");
        });

        test("비상구석: 만 15세 미만, 유아 동반 승객은 앉을 수 없다", () -> {
            Flight flight = new DomesticFlight("T100", GMP_CJU, DEPARTURE, AircraftType.A321NEO, 68_000);
            Seat exit = flight.getSeats().stream().filter(s -> s instanceof ExitRowSeat).findFirst().orElseThrow();
            BookingService service = new TestWorld().services.booking();
            Passenger child = PassengerFactory.create(PassengerType.CHILD, "HONG", "JIHO", Gender.MALE,
                    DEPARTURE.toLocalDate().minusYears(10), null);
            expectThrows(SeatRestrictionException.class, () -> service.checkSeat(flight, CabinClass.ECONOMY, child, exit));

            AdultPassenger parent = (AdultPassenger) adult("HONG", LocalDate.of(1990, 1, 1), null);
            parent.attachInfant((InfantPassenger) infant(DEPARTURE.toLocalDate().minusMonths(6)), flight);
            expectThrows(SeatRestrictionException.class, () -> service.checkSeat(flight, CabinClass.ECONOMY, parent, exit));

            Passenger adult = adult("KIM", LocalDate.of(1990, 1, 1), null);
            service.checkSeat(flight, CabinClass.ECONOMY, adult, exit);   // 통과
            check(exit.getExtraFee() > 0, "비상구석은 추가 요금");
        });

        test("다른 등급 좌석과 이미 예약된 좌석은 고를 수 없다", () -> {
            Flight flight = new DomesticFlight("T100", GMP_CJU, DEPARTURE, AircraftType.A321NEO, 68_000);
            BookingService service = new TestWorld().services.booking();
            Passenger adult = adult("KIM", LocalDate.of(1990, 1, 1), null);
            Seat business = flight.findSeat("1A").orElseThrow();
            expectThrows(SeatRestrictionException.class, () -> service.checkSeat(flight, CabinClass.ECONOMY, adult, business));
            Seat taken = flight.findSeat("5C").orElseThrow();
            taken.reserve();
            expectThrows(SeatUnavailableException.class, () -> service.checkSeat(flight, CabinClass.ECONOMY, adult, taken));
        });

        test("왕복 좌석은 구간마다 따로: 한 구간만 고르면 결제로 갈 수 없다", () -> {
            TestWorld world = new TestWorld();
            BookingSession session = world.sessionFor(Fixture.ROUNDTRIP, Screen.SEAT);
            Itinerary itinerary = session.requireItinerary();
            session.replaceSeats(SegmentType.OUTBOUND, world.services.booking().autoAssignSeats(
                    itinerary.getOutbound(), itinerary.getCabin(), session.getSeatedPassengers(), Map.of()));
            check(session.allSeatsAssigned(SegmentType.OUTBOUND), "가는 편 완료");
            check(!session.allSeatsAssigned(), "오는 편이 남음");
            List<String> missing = Screen.PAYMENT.check(session);
            check(missing.size() == 1 && missing.get(0).contains("오는 편"), "결제 입장 조건: " + missing);
        });
    }

    private static Passenger adult(String lastName, LocalDate birth, String passport) throws InvalidPassengerException {
        return PassengerFactory.create(PassengerType.ADULT, lastName, "TEST", Gender.FEMALE, birth, passport);
    }

    private static Passenger infant(LocalDate birth) throws InvalidPassengerException {
        return PassengerFactory.create(PassengerType.INFANT, "LEE", "BABY", Gender.MALE, birth, "M98765432");
    }
}
