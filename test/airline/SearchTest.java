package airline;

import airline.domain.booking.Itinerary;
import airline.domain.flight.Airport;
import airline.domain.flight.Flight;
import airline.domain.flight.TimeBand;
import airline.domain.passenger.AgeCalculator;
import airline.domain.passenger.AgeCalculator.AgeResult;
import airline.domain.passenger.PassengerType;
import airline.domain.seat.CabinClass;
import airline.exception.InvalidPassengerException;
import airline.exception.InvalidRequestException;
import airline.service.FlightSearchService;
import airline.service.SearchCriteria;
import airline.service.SearchResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static airline.TestSupport.check;
import static airline.TestSupport.equal;
import static airline.TestSupport.expectThrows;
import static airline.TestSupport.section;
import static airline.TestSupport.test;

/**
 * [A 담당 영역] 항공권 검색·선택, 나이 계산.
 * A가 페이지에서 쓰는 공용 기능이 약속대로 동작하는지 확인한다.
 * A가 계산 로직을 별도 클래스로 만들면(예: 달력 월 계산) 그 테스트도 이 파일에 추가한다.
 */
public final class SearchTest {

    private SearchTest() {
    }

    public static void run() {
        section("A: 출발지·도착지·날짜");

        test("출발지를 고르면 그 출발지의 도착지만 나온다", () -> {
            FlightSearchService search = new TestWorld().services.search();
            equal(List.of(Airport.GMP, Airport.ICN, Airport.CJU, Airport.PUS, Airport.NRT, Airport.BKK),
                    search.getOrigins(), "출발지 목록");
            equal(List.of(Airport.CJU, Airport.PUS), search.getDestinations(Airport.GMP), "김포 출발");
            equal(List.of(Airport.NRT, Airport.BKK), search.getDestinations(Airport.ICN), "인천 출발");
            equal(List.of(Airport.ICN), search.getDestinations(Airport.NRT), "도쿄 출발");
            equal(List.of(Airport.GMP, Airport.PUS), search.getDestinations(Airport.CJU), "제주 출발");
        });

        test("달력 범위: 오늘부터 30일", () -> {
            TestWorld world = new TestWorld();
            equal(world.today(), world.services.search().getFirstSearchableDate(), "첫날");
            equal(world.today().plusDays(29), world.services.search().getLastSearchableDate(), "마지막 날");
        });

        test("모든 노선·방향에 날마다 항공권이 2~3개", () -> {
            TestWorld world = new TestWorld();
            FlightSearchService search = world.services.search();
            LocalDate date = world.today().plusDays(3);
            for (Airport origin : search.getOrigins()) {
                for (Airport destination : search.getDestinations(origin)) {
                    int count = search.search(SearchCriteria.oneWay(origin, destination, date, CabinClass.ECONOMY, 1, 0, 0))
                            .outbound().size();
                    check(count >= 2 && count <= 3, origin + "→" + destination + " 항공권 " + count + "개");
                }
            }
        });

        test("운항하지 않는 노선, 지난 날짜, 오는 날이 가는 날보다 빠르면 오류", () -> {
            TestWorld world = new TestWorld();
            FlightSearchService search = world.services.search();
            LocalDate d = world.today().plusDays(2);
            expectThrows(InvalidRequestException.class, () -> search.search(
                    SearchCriteria.oneWay(Airport.GMP, Airport.NRT, d, CabinClass.ECONOMY, 1, 0, 0)));
            expectThrows(InvalidRequestException.class, () -> search.search(
                    SearchCriteria.oneWay(Airport.GMP, Airport.CJU, world.today().minusDays(1), CabinClass.ECONOMY, 1, 0, 0)));
            expectThrows(InvalidRequestException.class, () -> search.search(
                    SearchCriteria.roundTrip(Airport.GMP, Airport.CJU, d, d.minusDays(1), CabinClass.ECONOMY, 1, 0, 0)));
        });

        test("인원 규칙: 성인 1명 이상, 좌석 9명까지, 유아는 성인 수까지", () -> {
            FlightSearchService search = new TestWorld().services.search();
            LocalDate d = LocalDate.of(2026, 11, 20);
            expectThrows(InvalidPassengerException.class, () -> search.search(
                    SearchCriteria.oneWay(Airport.GMP, Airport.CJU, d, CabinClass.ECONOMY, 0, 1, 0)));
            expectThrows(InvalidPassengerException.class, () -> search.search(
                    SearchCriteria.oneWay(Airport.GMP, Airport.CJU, d, CabinClass.ECONOMY, 5, 5, 0)));
            expectThrows(InvalidPassengerException.class, () -> search.search(
                    SearchCriteria.oneWay(Airport.GMP, Airport.CJU, d, CabinClass.ECONOMY, 1, 0, 2)));
        });

        section("A: 편도·왕복 검색 결과");

        test("편도: 가는 편만, 출발 시각 순", () -> {
            TestWorld world = new TestWorld();
            SearchResult result = world.services.search().search(SearchCriteria.oneWay(
                    Airport.GMP, Airport.CJU, world.today().plusDays(1), CabinClass.ECONOMY, 1, 0, 0));
            check(result.inbound().isEmpty(), "오는 편 없음");
            List<Flight> out = result.outbound();
            for (int i = 1; i < out.size(); i++) {
                check(out.get(i - 1).getDepartureTime().isBefore(out.get(i).getDepartureTime()), "시각 순 정렬");
            }
        });

        test("왕복: 오는 편은 도착지 → 출발지, 오는 날 기준", () -> {
            TestWorld world = new TestWorld();
            LocalDate back = world.today().plusDays(4);
            SearchResult result = world.services.search().search(SearchCriteria.roundTrip(
                    Airport.ICN, Airport.NRT, world.today().plusDays(1), back, CabinClass.ECONOMY, 2, 0, 0));
            check(!result.inbound().isEmpty(), "오는 편 있음");
            for (Flight f : result.inbound()) {
                check(f.getOrigin() == Airport.NRT && f.getDestination() == Airport.ICN, "방향: " + f);
                equal(back, f.getDepartureDate(), "오는 날");
            }
        });

        test("같은 날 왕복: 가는 편 도착 1시간 뒤 출발하는 오는 편만 고를 수 있다", () -> {
            TestWorld world = new TestWorld();
            LocalDate d = world.today().plusDays(2);
            SearchResult result = world.services.search().search(SearchCriteria.roundTrip(
                    Airport.GMP, Airport.CJU, d, d, CabinClass.ECONOMY, 1, 0, 0));
            Flight lastOut = result.outbound().get(result.outbound().size() - 1);   // 18:40 출발 → 19:50 도착
            for (Flight in : result.inboundOptionsFor(lastOut)) {
                check(!in.getDepartureTime().isBefore(lastOut.getArrivalTime().plus(Itinerary.MIN_STAY)), "시간 맞음: " + in);
            }
            check(result.inboundOptionsFor(lastOut).size() < result.inbound().size(), "일부가 걸러짐");
            Flight tooEarly = result.inbound().get(0);
            expectThrows(InvalidRequestException.class, () -> Itinerary.roundTrip(lastOut, tooEarly, CabinClass.ECONOMY));
        });

        test("달력용 최저가와 시간대", () -> {
            TestWorld world = new TestWorld();
            check(world.services.search().findLowestAdultPrice(Airport.GMP, Airport.CJU,
                    world.today().plusDays(1), CabinClass.ECONOMY).isPresent(), "내일 최저가 있음");
            check(world.services.search().findLowestAdultPrice(Airport.GMP, Airport.CJU,
                    world.today().plusDays(40), CabinClass.ECONOMY).isEmpty(), "스케줄 밖은 없음");
            equal(TimeBand.MORNING, TimeBand.of(LocalTime.of(7, 30)), "07:30");
            equal(TimeBand.DAWN, TimeBand.of(LocalTime.of(1, 40)), "01:40");
            equal(TimeBand.EVENING, TimeBand.of(LocalTime.of(18, 0)), "18:00");
        });

        section("A: 나이 계산기 (생년월일 8자리 → 만 나이)");

        test("8자리 입력을 날짜로 바꾼다 (구분 기호 허용)", () -> {
            equal(LocalDate.of(1995, 3, 15), AgeCalculator.parseBirthDate("19950315"), "숫자 8자리");
            equal(LocalDate.of(1995, 3, 15), AgeCalculator.parseBirthDate("1995-03-15"), "하이픈");
            expectThrows(InvalidPassengerException.class, () -> AgeCalculator.parseBirthDate("950315"));
            expectThrows(InvalidPassengerException.class, () -> AgeCalculator.parseBirthDate("20230230"));
        });

        test("생일 전날과 당일의 만 나이, 국내선/국제선 유형", () -> {
            AgeResult before = AgeCalculator.calculate("20141113", LocalDate.of(2026, 11, 12));
            equal(11, before.age(), "생일 전날");
            equal(PassengerType.CHILD, before.typeFor(true), "국내선");
            equal(PassengerType.CHILD, before.typeFor(false), "국제선");
            AgeResult on = AgeCalculator.calculate("20141113", LocalDate.of(2026, 11, 13));
            equal(12, on.age(), "생일 당일");
            equal(PassengerType.CHILD, on.domesticType(), "국내선 만 12세는 소아");
            equal(PassengerType.ADULT, on.internationalType(), "국제선 만 12세는 성인");
        });

        test("유아와 생후 7일 미만, 미래 날짜", () -> {
            AgeResult baby = AgeCalculator.calculate(LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 12));
            equal(PassengerType.INFANT, baby.domesticType(), "유아");
            check(baby.isTooYoungToFly(), "생후 4일은 탑승 불가");
            expectThrows(InvalidPassengerException.class,
                    () -> AgeCalculator.calculate(LocalDate.of(2027, 1, 1), LocalDate.of(2026, 11, 12)));
            check(AgeCalculator.tryCalculate("1995", LocalDate.of(2026, 11, 12)).isEmpty(), "입력 중에는 빈 값");
        });
    }
}
