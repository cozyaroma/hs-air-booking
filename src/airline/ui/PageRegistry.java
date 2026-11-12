package airline.ui;

import airline.ui.page.passenger.PassengerPage;
import airline.ui.page.passenger.SeatPage;
import airline.ui.page.payment.ConfirmationPage;
import airline.ui.page.payment.MyBookingsPage;
import airline.ui.page.payment.PaymentPage;
import airline.ui.page.search.FlightSelectPage;
import airline.ui.page.search.SearchPage;

/**
 * Screen과 실제 페이지 클래스를 연결하는 곳 (공용 파일, 통합 담당만 수정).
 * switch가 모든 Screen을 다루지 않으면 컴파일 오류가 나므로, 페이지를 빠뜨릴 수 없다.
 */
public final class PageRegistry {

    private PageRegistry() {
    }

    public static BasePanel create(Screen screen, AppContext app) {
        return switch (screen) {
            case SEARCH -> new SearchPage(app);                 // A
            case FLIGHT_SELECT -> new FlightSelectPage(app);    // A
            case PASSENGER -> new PassengerPage(app);           // B
            case SEAT -> new SeatPage(app);                     // B
            case PAYMENT -> new PaymentPage(app);               // C
            case CONFIRMATION -> new ConfirmationPage(app);     // C
            case MY_BOOKINGS -> new MyBookingsPage(app);        // C
        };
    }
}
