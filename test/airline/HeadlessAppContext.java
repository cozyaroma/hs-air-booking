package airline;

import airline.domain.user.User;
import airline.service.AppServices;
import airline.service.BookingService;
import airline.service.FlightSearchService;
import airline.ui.AppContext;
import airline.ui.BookingSession;
import airline.ui.Screen;
import airline.util.AppClock;

import java.util.ArrayList;
import java.util.List;

/**
 * 화면 없이 페이지를 시험하기 위한 AppContext (MainFrame 대신 넣는다).
 * navigate()가 불리면 이동하는 대신 기록만 해 두므로, 테스트에서 "어디로 가려 했는지" 확인할 수 있다.
 */
public final class HeadlessAppContext implements AppContext {

    private final AppServices services;
    private final User user;
    private final BookingSession session;
    public final List<Screen> navigations = new ArrayList<>();

    public HeadlessAppContext(AppServices services, User user, BookingSession session) {
        this.services = services;
        this.user = user;
        this.session = session;
    }

    @Override
    public FlightSearchService getSearchService() {
        return services.search();
    }

    @Override
    public BookingService getBookingService() {
        return services.booking();
    }

    @Override
    public AppClock getClock() {
        return services.clock();
    }

    @Override
    public BookingSession getSession() {
        return session;
    }

    @Override
    public User getCurrentUser() {
        return user;
    }

    @Override
    public void navigate(Screen screen) {
        navigations.add(screen);
    }

    @Override
    public void startNewBooking() {
        session.reset();
        navigations.add(Screen.SEARCH);
    }
}
