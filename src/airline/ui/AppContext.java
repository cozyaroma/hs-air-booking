package airline.ui;

import airline.domain.user.User;
import airline.service.BookingService;
import airline.service.FlightSearchService;
import airline.util.AppClock;

/**
 * 페이지가 바깥 세상과 대화하는 유일한 창구.
 * 페이지는 MainFrame을 직접 알지 않고 이 인터페이스에만 의존한다.
 * 그래서 테스트(HeadlessAppContext)나 단독 실행(PageLauncher)에서 다른 구현을 넣어도 페이지 코드는 그대로다.
 */
public interface AppContext {

    FlightSearchService getSearchService();

    BookingService getBookingService();

    AppClock getClock();

    /** 진행 중인 예약 정보 (페이지 간 계약) */
    BookingSession getSession();

    /** 로그인한 회원. 페이지가 열려 있는 동안에는 항상 있다. */
    User getCurrentUser();

    /** 다른 페이지로 이동한다. 입장 조건(Screen.check)이 안 맞으면 이동하지 않고 안내한다. */
    void navigate(Screen screen);

    /** 진행 중인 예약을 버리고 검색 페이지로 돌아간다. */
    void startNewBooking();
}
