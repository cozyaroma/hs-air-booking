package airline.app;

import airline.domain.user.PasswordHasher;
import airline.repository.BookingRepository;
import airline.repository.FlightRepository;
import airline.repository.InMemoryBookingRepository;
import airline.repository.InMemoryFlightRepository;
import airline.repository.InMemoryUserRepository;
import airline.repository.UserRepository;
import airline.service.AppServices;
import airline.service.AuthService;
import airline.service.BookingCodeGenerator;
import airline.service.BookingService;
import airline.service.FareCalculator;
import airline.service.FlightSearchService;
import airline.util.AppClock;

import java.util.Random;

/**
 * 객체 조립 공장 (의존성 주입을 한 곳에서).
 * 저장소 → 서비스 순서로 만들고 생성자로 넘겨준다. 다른 클래스는 new로 저장소나 서비스를 만들지 않으므로,
 * 메모리 저장을 파일 저장으로 바꾸는 등 구현을 바꿀 때 이 파일만 고치면 된다.
 * 실제 실행(Main), 페이지 단독 실행(PageLauncher), 테스트(TestWorld)가 모두 이 공장을 쓴다.
 */
public final class AppFactory {

    /** 운항 스케줄을 만드는 기간 (오늘부터) */
    public static final int SCHEDULE_DAYS = 30;

    private AppFactory() {
    }

    /**
     * @param seed         좌석 채우기 난수 씨앗. 같은 값이면 매번 같은 좌석 상태가 나온다.
     * @param withDemoData 시연용 계정(demo / demo1234)과 그 계정의 예약을 만들지
     */
    public static AppServices create(AppClock clock, long seed, boolean withDemoData) {
        FlightRepository flightRepository = new InMemoryFlightRepository();
        BookingRepository bookingRepository = new InMemoryBookingRepository();
        UserRepository userRepository = new InMemoryUserRepository();
        FareCalculator fareCalculator = new FareCalculator();

        SampleData.loadFlights(flightRepository, clock.today(), SCHEDULE_DAYS, new Random(seed));

        AuthService auth = new AuthService(userRepository, new PasswordHasher(), clock);
        FlightSearchService search = new FlightSearchService(flightRepository, fareCalculator, clock);
        BookingService booking = new BookingService(bookingRepository, fareCalculator,
                new BookingCodeGenerator(new Random(seed + 1)), clock);
        AppServices services = new AppServices(auth, search, booking, clock);

        if (withDemoData) {
            SampleData.createDemoAccount(services);
        }
        return services;
    }

    /** 실제 실행용: 현재 시각 기준, 매번 같은 좌석 상태 */
    public static AppServices createDefault() {
        return create(new AppClock(), 20261112L, true);
    }
}
