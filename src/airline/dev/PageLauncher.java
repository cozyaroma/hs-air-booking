package airline.dev;

import airline.app.AppFactory;
import airline.app.SampleData;
import airline.domain.user.User;
import airline.exception.AirlineException;
import airline.service.AppServices;
import airline.ui.Application;
import airline.ui.BookingSession;
import airline.ui.Screen;

import java.util.Arrays;
import java.util.Locale;

/**
 * 페이지 하나만 바로 열어보는 개발 도구 (공용 파일).
 *
 * 앞 페이지가 아직 완성되지 않았어도, 준비된 데이터(SessionFixtures)를 채운 상태로 내 페이지부터 시작한다.
 * 시연용 계정(demo)으로 자동 로그인하고, 개발 모드라서:
 *   - 페이지를 이동할 때마다 콘솔에 [세션 검사] 결과가 찍힌다 (다음 페이지 입장 조건을 만족했는지)
 *   - 헤더에 [세션 보기] 버튼이 생겨 지금 세션에 무엇이 들어 있는지 볼 수 있다
 *
 * 사용법:  page.bat SEAT ROUNDTRIP      (macOS/Linux: ./page.sh SEAT ROUNDTRIP)
 *   페이지: SEARCH, FLIGHT_SELECT, PASSENGER, SEAT, PAYMENT, CONFIRMATION, MY_BOOKINGS
 *   데이터: ONEWAY, ROUNDTRIP, INTL, INTL_ROUNDTRIP   (생략하면 ROUNDTRIP)
 */
public final class PageLauncher {

    private PageLauncher() {
    }

    public static void main(String[] args) throws AirlineException {
        Screen screen;
        SessionFixtures.Fixture fixture;
        try {
            screen = Screen.valueOf(arg(args, 0, "SEARCH"));
            fixture = SessionFixtures.Fixture.valueOf(arg(args, 1, "ROUNDTRIP"));
        } catch (IllegalArgumentException e) {
            printUsage();
            return;
        }
        AppServices services = AppFactory.createDefault();
        User user = services.auth().login(SampleData.DEMO_USERNAME, SampleData.DEMO_PASSWORD);
        BookingSession session = SessionFixtures.prepare(services, user, fixture, screen);
        System.out.println("[PageLauncher] " + screen.getTitle() + " 페이지 (담당 " + screen.getOwner().getCode()
                + ")를 '" + fixture.getDescription() + "' 데이터로 엽니다.");
        System.out.println("[PageLauncher] 입장 조건 검사: " + (screen.check(session).isEmpty() ? "통과" : screen.check(session)));
        Application.launchAt(services, user, session, screen);
    }

    private static String arg(String[] args, int index, String fallback) {
        return args.length > index ? args[index].trim().toUpperCase(Locale.ROOT) : fallback;
    }

    private static void printUsage() {
        System.out.println("사용법: page.bat <페이지> [데이터]");
        System.out.println("  페이지: " + Arrays.toString(Screen.values()));
        for (SessionFixtures.Fixture f : SessionFixtures.Fixture.values()) {
            System.out.println("  데이터 " + f + " : " + f.getDescription());
        }
    }
}
