package airline;

/**
 * 모든 테스트 실행: test.bat (macOS/Linux: ./test.sh)
 * 실패가 하나라도 있으면 종료 코드 1로 끝난다.
 *
 *   CoreTest          [공용] 로그인, 페이지 간 세션 계약, 개발용 데이터
 *   SearchTest        [A] 검색·항공권 선택·나이 계산
 *   PassengerSeatTest [B] 승객 규칙·좌석 규칙
 *   PaymentBookingTest[C] 결제 정보·예약·취소·체크인
 *   PageSmokeTest     [공용] 모든 페이지가 모든 데이터 조합에서 오류 없이 그려지는지
 */
public final class AllTests {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");   // 화면 없이 Swing 페이지 시험
        CoreTest.run();
        SearchTest.run();
        PassengerSeatTest.run();
        PaymentBookingTest.run();
        PageSmokeTest.run();
        int failures = TestSupport.finish();
        System.exit(failures == 0 ? 0 : 1);
    }
}
