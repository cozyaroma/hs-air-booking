package airline;

import airline.app.AppFactory;
import airline.app.SampleData;
import airline.dev.SessionFixtures;
import airline.domain.user.User;
import airline.exception.AirlineException;
import airline.service.AppServices;
import airline.ui.BookingSession;
import airline.ui.Screen;
import airline.util.AppClock;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 테스트용 세상: 시각이 고정되어 있어서(2026-11-12 목 09:00) 매번 같은 결과가 나온다.
 * 테스트마다 새로 만들면 서로 영향을 주지 않는다 (좌석 예약, 시각 이동 등).
 */
public final class TestWorld {

    public static final LocalDateTime START = LocalDateTime.of(2026, 11, 12, 9, 0);

    public final AppClock clock = AppClock.fixedAt(START);
    public final AppServices services = AppFactory.create(clock, 7L, true);

    /** 시연용 계정(demo)으로 로그인한 사용자 */
    public User demoUser() throws AirlineException {
        return services.auth().login(SampleData.DEMO_USERNAME, SampleData.DEMO_PASSWORD);
    }

    public LocalDate today() {
        return clock.today();
    }

    /** target 페이지 입장 조건을 만족하도록 미리 채운 세션 */
    public BookingSession sessionFor(SessionFixtures.Fixture fixture, Screen target) throws AirlineException {
        return SessionFixtures.prepare(services, demoUser(), fixture, target);
    }
}
