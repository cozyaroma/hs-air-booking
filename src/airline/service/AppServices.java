package airline.service;

import airline.util.AppClock;

/**
 * 프로그램 전체가 함께 쓰는 서비스 묶음.
 * AppFactory가 한 번 만들어서 화면(MainFrame), 테스트, 개발 도구에 똑같이 넘겨준다.
 */
public record AppServices(AuthService auth, FlightSearchService search, BookingService booking, AppClock clock) {
}
