package airline;

import airline.dev.SessionFixtures;
import airline.dev.SessionFixtures.Fixture;
import airline.domain.booking.SegmentType;
import airline.domain.flight.Flight;
import airline.domain.passenger.Passenger;
import airline.domain.user.User;
import airline.exception.AuthException;
import airline.exception.InvalidRequestException;
import airline.service.AuthService;
import airline.ui.BookingSession;
import airline.ui.Screen;

import java.util.List;

import static airline.TestSupport.check;
import static airline.TestSupport.equal;
import static airline.TestSupport.expectThrows;
import static airline.TestSupport.section;
import static airline.TestSupport.test;

/** [공용] 로그인, 페이지 간 세션 계약(BookingSession + Screen.check), 개발용 데이터 */
public final class CoreTest {

    private CoreTest() {
    }

    public static void run() {
        section("공용: 회원가입·로그인");

        test("회원가입하면 바로 로그인 상태가 된다", () -> {
            AuthService auth = new TestWorld().services.auth();
            User user = auth.signUp("hansung01", "pass1234", "pass1234");
            equal("hansung01", user.getUsername(), "가입한 아이디");
            check(auth.isLoggedIn() && auth.getCurrentUser().orElseThrow().equals(user), "가입 직후 로그인 상태");
        });

        test("아이디는 소문자로 정리되고 중복 가입은 막는다", () -> {
            AuthService auth = new TestWorld().services.auth();
            auth.signUp("Student7", "abcd5678", "abcd5678");
            AuthException e = expectThrows(AuthException.class, () -> auth.signUp("student7", "abcd5678", "abcd5678"));
            check(e.getMessage().contains("이미 사용 중"), "중복 아이디 메시지: " + e.getMessage());
            check(!auth.isUsernameAvailable("STUDENT7"), "중복 확인 버튼: 사용 불가");
        });

        test("비밀번호 규칙: 8~20자, 영문과 숫자 모두 포함, 확인 값 일치", () -> {
            AuthService auth = new TestWorld().services.auth();
            expectThrows(AuthException.class, () -> auth.signUp("user01", "short1", "short1"));
            expectThrows(AuthException.class, () -> auth.signUp("user01", "onlyletters", "onlyletters"));
            expectThrows(AuthException.class, () -> auth.signUp("user01", "12345678", "12345678"));
            expectThrows(AuthException.class, () -> auth.signUp("user01", "pass1234", "pass9999"));
            expectThrows(AuthException.class, () -> auth.signUp("ab", "pass1234", "pass1234"));
            check(!auth.isLoggedIn(), "실패하면 로그인되지 않음");
        });

        test("없는 아이디와 틀린 비밀번호는 같은 메시지 (아이디 존재 여부를 알 수 없게)", () -> {
            AuthService auth = new TestWorld().services.auth();
            String unknown = expectThrows(AuthException.class, () -> auth.login("nobody", "pass1234")).getMessage();
            String wrong = expectThrows(AuthException.class, () -> auth.login("demo", "wrong1234")).getMessage();
            equal(unknown, wrong, "로그인 실패 메시지");
        });

        test("시연용 계정 로그인 후 로그아웃", () -> {
            TestWorld world = new TestWorld();
            world.demoUser();
            check(world.services.auth().isLoggedIn(), "로그인됨");
            world.services.auth().logout();
            check(world.services.auth().getCurrentUser().isEmpty(), "로그아웃 후 사용자 없음");
        });

        section("공용: 페이지 간 세션 계약 (Screen.check)");

        test("빈 세션이면 검색·내 예약만 열 수 있다", () -> {
            BookingSession empty = new BookingSession();
            check(Screen.SEARCH.check(empty).isEmpty(), "검색은 언제든");
            check(Screen.MY_BOOKINGS.check(empty).isEmpty(), "내 예약은 언제든");
            for (Screen screen : List.of(Screen.FLIGHT_SELECT, Screen.PASSENGER, Screen.SEAT, Screen.PAYMENT, Screen.CONFIRMATION)) {
                check(!screen.check(empty).isEmpty(), screen + "은(는) 막혀야 함");
            }
        });

        test("모든 페이지 × 모든 개발용 데이터: 준비된 세션이 입장 조건을 만족한다", () -> {
            TestWorld world = new TestWorld();
            for (Fixture fixture : Fixture.values()) {
                for (Screen screen : Screen.values()) {
                    BookingSession session = world.sessionFor(fixture, screen);
                    List<String> missing = screen.check(session);
                    check(missing.isEmpty(), fixture + "/" + screen + " 입장 불가: " + missing);
                }
            }
        });

        test("왕복인데 오는 편을 안 고르면 승객 페이지가 막힌다", () -> {
            TestWorld world = new TestWorld();
            BookingSession session = world.sessionFor(Fixture.ROUNDTRIP, Screen.FLIGHT_SELECT);
            session.selectOutbound(session.getSearchResult().outbound().get(0));
            List<String> missing = Screen.PASSENGER.check(session);
            check(missing.size() == 1 && missing.get(0).contains("오는 편"), "오는 편 안내: " + missing);
        });

        test("앞 단계를 바꾸면 뒤 단계 값이 비워진다", () -> {
            TestWorld world = new TestWorld();
            BookingSession session = world.sessionFor(Fixture.ROUNDTRIP, Screen.PAYMENT);
            check(session.allSeatsAssigned(), "준비: 좌석 배정 완료");
            Passenger first = session.getSeatedPassengers().get(0);
            Flight otherOutbound = session.getSearchResult().outbound().get(1);
            session.selectOutbound(otherOutbound);
            check(session.getSeat(SegmentType.OUTBOUND, first) == null, "가는 편을 바꾸면 가는 편 좌석 초기화");
            session.setPassengers(session.getPassengers());
            check(session.getSeat(SegmentType.INBOUND, first) == null, "승객을 다시 저장하면 모든 좌석 초기화");
            session.startSearch(session.getCriteria(), session.getSearchResult());
            check(session.getOutbound() == null && session.getPassengers().isEmpty(), "새 검색이면 선택·승객 초기화");
        });

        test("시간이 맞지 않는 오는 편은 세션에 저장되지 않는다", () -> {
            TestWorld world = new TestWorld();
            BookingSession session = world.sessionFor(Fixture.ROUNDTRIP, Screen.FLIGHT_SELECT);
            List<Flight> outbound = session.getSearchResult().outbound();
            session.selectOutbound(outbound.get(outbound.size() - 1));
            Flight sameDayEarlier = world.services.search().search(airline.service.SearchCriteria.oneWay(
                    session.getCriteria().destination(), session.getCriteria().origin(),
                    session.getCriteria().departureDate(), session.getCriteria().cabin(), 1, 0, 0)).outbound().get(0);
            expectThrows(InvalidRequestException.class, () -> session.selectInbound(sameDayEarlier));
            check(session.getInbound() == null, "저장되지 않음");
        });

        test("개발용 데이터 CONFIRMATION은 실제 예약을 만든다", () -> {
            TestWorld world = new TestWorld();
            BookingSession session = SessionFixtures.prepare(world.services, world.demoUser(), Fixture.INTL, Screen.CONFIRMATION);
            check(session.getLastBooking() != null, "예약 있음");
            check(session.getOutbound() == null && session.getPassengers().isEmpty(), "예약 후 진행 정보 비움");
        });
    }
}
