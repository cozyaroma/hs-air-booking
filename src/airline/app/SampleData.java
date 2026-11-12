package airline.app;

import airline.domain.booking.Booking;
import airline.domain.booking.Itinerary;
import airline.domain.booking.SegmentType;
import airline.domain.flight.AircraftType;
import airline.domain.flight.Airport;
import airline.domain.flight.DomesticFlight;
import airline.domain.flight.Flight;
import airline.domain.flight.InternationalFlight;
import airline.domain.flight.Route;
import airline.domain.passenger.Gender;
import airline.domain.passenger.Passenger;
import airline.domain.passenger.PassengerFactory;
import airline.domain.passenger.PassengerType;
import airline.domain.seat.CabinClass;
import airline.domain.seat.Seat;
import airline.domain.user.User;
import airline.exception.AirlineException;
import airline.exception.SeatUnavailableException;
import airline.payment.BankTransferPayment;
import airline.payment.CreditCardPayment;
import airline.payment.PaymentMethod;
import airline.repository.FlightRepository;
import airline.service.AppServices;
import airline.service.BookingRequest;
import airline.service.BookingService;
import airline.service.SearchCriteria;
import airline.service.SearchResult;
import airline.util.Formats;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 시연용 샘플 데이터. 노선, 시간표, 운임은 모두 가상의 값이며 시각은 시차 없이 한국 시간으로 통일했다.
 *
 * 노선(출발지 → 도착지): 김포 ↔ 제주, 김포 ↔ 부산, 부산 ↔ 제주, 인천 ↔ 도쿄, 인천 ↔ 방콕
 * 출발지마다 갈 수 있는 도착지가 다르다 (예: 인천 → 도쿄·방콕만, 도쿄 → 인천만).
 * 노선·방향마다 하루 2~3개 항공권이 있고, 오늘부터 30일치를 만든다.
 * 다른 고객이 이미 예약한 것처럼 좌석의 25~75%를 미리 채운다.
 */
public final class SampleData {

    public static final String DEMO_USERNAME = "demo";
    public static final String DEMO_PASSWORD = "demo1234";

    private record Schedule(String flightNumber, Route route, LocalTime departure, AircraftType aircraft) {
    }

    private static final Route GMP_CJU = new Route(Airport.GMP, Airport.CJU, 70, 68_000, 8_800);
    private static final Route CJU_GMP = new Route(Airport.CJU, Airport.GMP, 70, 68_000, 8_800);
    private static final Route GMP_PUS = new Route(Airport.GMP, Airport.PUS, 60, 62_000, 8_800);
    private static final Route PUS_GMP = new Route(Airport.PUS, Airport.GMP, 60, 62_000, 8_800);
    private static final Route PUS_CJU = new Route(Airport.PUS, Airport.CJU, 55, 55_000, 8_800);
    private static final Route CJU_PUS = new Route(Airport.CJU, Airport.PUS, 55, 55_000, 8_800);
    private static final Route ICN_NRT = new Route(Airport.ICN, Airport.NRT, 140, 240_000, 23_000);
    private static final Route NRT_ICN = new Route(Airport.NRT, Airport.ICN, 155, 240_000, 23_000);
    private static final Route ICN_BKK = new Route(Airport.ICN, Airport.BKK, 350, 390_000, 52_000);
    private static final Route BKK_ICN = new Route(Airport.BKK, Airport.ICN, 330, 390_000, 52_000);

    private static final List<Schedule> SCHEDULES = List.of(
            schedule("HS1201", GMP_CJU, "07:30", AircraftType.A321NEO),
            schedule("HS1205", GMP_CJU, "12:10", AircraftType.A321NEO),
            schedule("HS1209", GMP_CJU, "18:40", AircraftType.A321NEO),
            schedule("HS1202", CJU_GMP, "09:40", AircraftType.A321NEO),
            schedule("HS1206", CJU_GMP, "14:20", AircraftType.A321NEO),
            schedule("HS1210", CJU_GMP, "20:50", AircraftType.A321NEO),
            schedule("HS1101", GMP_PUS, "08:00", AircraftType.A321NEO),
            schedule("HS1105", GMP_PUS, "17:30", AircraftType.A321NEO),
            schedule("HS1102", PUS_GMP, "10:00", AircraftType.A321NEO),
            schedule("HS1106", PUS_GMP, "19:30", AircraftType.A321NEO),
            schedule("HS1301", PUS_CJU, "09:10", AircraftType.A321NEO),
            schedule("HS1305", PUS_CJU, "16:40", AircraftType.A321NEO),
            schedule("HS1302", CJU_PUS, "11:20", AircraftType.A321NEO),
            schedule("HS1306", CJU_PUS, "18:50", AircraftType.A321NEO),
            schedule("HS701", ICN_NRT, "09:00", AircraftType.B737_8),
            schedule("HS703", ICN_NRT, "13:30", AircraftType.B737_8),
            schedule("HS705", ICN_NRT, "19:10", AircraftType.B737_8),
            schedule("HS702", NRT_ICN, "12:20", AircraftType.B737_8),
            schedule("HS704", NRT_ICN, "16:50", AircraftType.B737_8),
            schedule("HS706", NRT_ICN, "21:30", AircraftType.B737_8),
            schedule("HS751", ICN_BKK, "10:30", AircraftType.B737_8),
            schedule("HS753", ICN_BKK, "18:20", AircraftType.B737_8),
            schedule("HS752", BKK_ICN, "01:40", AircraftType.B737_8),
            schedule("HS754", BKK_ICN, "12:30", AircraftType.B737_8));

    private SampleData() {
    }

    private static Schedule schedule(String number, Route route, String time, AircraftType aircraft) {
        return new Schedule(number, route, LocalTime.parse(time), aircraft);
    }

    // ===== 운항 스케줄 =====

    public static void loadFlights(FlightRepository repository, LocalDate startDate, int days, Random random) {
        for (int d = 0; d < days; d++) {
            LocalDate date = startDate.plusDays(d);
            for (Schedule schedule : SCHEDULES) {
                Flight flight = createFlight(schedule, date.atTime(schedule.departure()));
                fillSeatsRandomly(flight, random);
                repository.save(flight);
            }
        }
    }

    /** 노선이 국내선인지에 따라 알맞은 Flight 하위 클래스를 만든다. */
    private static Flight createFlight(Schedule schedule, LocalDateTime departure) {
        long fare = fareFor(schedule.route(), departure);
        if (schedule.route().isDomestic()) {
            return new DomesticFlight(schedule.flightNumber(), schedule.route(), departure, schedule.aircraft(), fare);
        }
        return new InternationalFlight(schedule.flightNumber(), schedule.route(), departure, schedule.aircraft(), fare);
    }

    /** 이른 아침은 싸고, 저녁과 주말(금~일)은 비싸게 */
    private static long fareFor(Route route, LocalDateTime departure) {
        double factor = 1.0;
        if (departure.getHour() < 9) {
            factor *= 0.85;
        } else if (departure.getHour() >= 18) {
            factor *= 1.10;
        }
        DayOfWeek day = departure.getDayOfWeek();
        if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            factor *= 1.15;
        }
        return Formats.round100(route.getStandardFare() * factor);
    }

    /** 다른 고객의 예약을 흉내 내기 위해 좌석의 25~75%를 미리 채운다. */
    private static void fillSeatsRandomly(Flight flight, Random random) {
        double occupancy = 0.25 + random.nextDouble() * 0.5;
        for (Seat seat : flight.getSeats()) {
            if (random.nextDouble() < occupancy) {
                try {
                    seat.reserve();
                } catch (SeatUnavailableException e) {
                    throw new IllegalStateException(e);   // 새로 만든 좌석이라 일어날 수 없음
                }
            }
        }
    }

    // ===== 시연용 계정과 예약 =====

    /**
     * 시연용 계정 demo / demo1234 과 예약 2건을 만든다.
     *  1) 내일 12:10 김포 → 제주 왕복 (3일 뒤 귀국), 성인 1 + 소아 1, 신용카드
     *  2) 10일 뒤 인천 → 도쿄 편도, 성인 1 + 유아 1, 계좌이체
     */
    public static void createDemoAccount(AppServices services) {
        try {
            User demo = services.auth().register(DEMO_USERNAME, DEMO_PASSWORD);
            LocalDate today = services.clock().today();
            BookingService bookingService = services.booking();

            SearchResult jeju = services.search().search(SearchCriteria.roundTrip(
                    Airport.GMP, Airport.CJU, today.plusDays(1), today.plusDays(4), CabinClass.ECONOMY, 1, 1, 0));
            Flight noon = jeju.outbound().stream().filter(f -> f.getFlightNumber().equals("HS1205"))
                    .findFirst().orElse(jeju.outbound().get(0));   // 내일 12:10 → 시각 이동으로 체크인 시연하기 좋음
            Itinerary roundTrip = Itinerary.roundTrip(noon, jeju.inbound().get(0), CabinClass.ECONOMY);
            List<Passenger> family = List.of(
                    PassengerFactory.create(PassengerType.ADULT, "HONG", "GILDONG", Gender.MALE,
                            LocalDate.of(1995, 3, 15), null),
                    PassengerFactory.create(PassengerType.CHILD, "HONG", "SEOA", Gender.FEMALE,
                            today.minusYears(8), null));
            book(bookingService, demo, roundTrip, family, new CreditCardPayment("신한카드",
                    "4111 1111 1111 1111", "12/30", "123", "950315", "12", 0));

            SearchResult tokyo = services.search().search(SearchCriteria.oneWay(
                    Airport.ICN, Airport.NRT, today.plusDays(10), CabinClass.ECONOMY, 1, 0, 1));
            Itinerary oneWay = Itinerary.oneWay(tokyo.outbound().get(0), CabinClass.ECONOMY);
            List<Passenger> withInfant = List.of(
                    PassengerFactory.create(PassengerType.ADULT, "KIM", "MINJI", Gender.FEMALE,
                            LocalDate.of(1992, 8, 2), "M12345678"),
                    PassengerFactory.create(PassengerType.INFANT, "KIM", "HANEUL", Gender.MALE,
                            today.minusMonths(10), "M87654321"));
            book(bookingService, demo, oneWay, withInfant,
                    new BankTransferPayment("KB국민은행", "123456-01-234567", "김민지"));
        } catch (AirlineException | RuntimeException e) {
            System.out.println("[시연용 예약] 만들지 못했습니다: " + e.getMessage());
        }
    }

    private static Booking book(BookingService service, User owner, Itinerary itinerary, List<Passenger> passengers,
                                PaymentMethod payment) throws AirlineException {
        service.validatePassengers(itinerary, passengers);
        Map<SegmentType, Map<Passenger, Seat>> seats = new EnumMap<>(SegmentType.class);
        for (SegmentType segment : itinerary.getSegments()) {
            seats.put(segment, service.autoAssignSeats(itinerary.getFlight(segment), itinerary.getCabin(),
                    passengers, Map.of()));
        }
        BookingRequest request = new BookingRequest(itinerary, passengers, seats, Map.of());
        return service.createBooking(owner, request, payment);
    }
}
