package airline;

import airline.dev.SessionFixtures.Fixture;
import airline.domain.user.User;
import airline.ui.BasePanel;
import airline.ui.BookingSession;
import airline.ui.PageRegistry;
import airline.ui.Screen;

import java.awt.Dimension;

import static airline.TestSupport.check;
import static airline.TestSupport.equal;
import static airline.TestSupport.section;
import static airline.TestSupport.test;

/**
 * [공용] 모든 페이지가 모든 개발용 데이터(편도/왕복/국제선/유아)에서 오류 없이 만들어지고 그려지는지 확인한다.
 * 화면 없이(headless) 실행하므로 페이지의 onShow()에서 알림창을 띄우면 이 테스트가 실패한다.
 *
 * 내 페이지를 고친 뒤 이 테스트가 깨지면: 보통 왕복(오는 편)이나 유아(좌석 없음) 처리를 빠뜨린 것이다.
 */
public final class PageSmokeTest {

    private PageSmokeTest() {
    }

    public static void run() {
        section("공용: 페이지 스모크 테스트 (모든 페이지 × 모든 데이터)");
        for (Screen screen : Screen.values()) {
            test(screen + " 페이지 (담당 " + screen.getOwner().getCode() + ")", () -> {
                TestWorld world = new TestWorld();
                User user = world.demoUser();
                for (Fixture fixture : Fixture.values()) {
                    BookingSession session = world.sessionFor(fixture, screen);
                    HeadlessAppContext context = new HeadlessAppContext(world.services, user, session);
                    BasePanel page;
                    try {
                        page = PageRegistry.create(screen, context);
                        equal(screen, page.getScreen(), "페이지가 자기 Screen을 BasePanel에 넘겼는지");
                        page.refresh();
                        page.refresh();   // 두 번 보여도 문제없어야 한다 (이전 → 다음 왕복)
                        page.setSize(new Dimension(1200, 800));
                        page.doLayout();
                        page.getPreferredSize();
                    } catch (RuntimeException e) {
                        throw new AssertionError(fixture + " 데이터에서 실패: " + e, e);
                    }
                    check(context.navigations.isEmpty(), fixture + ": 화면을 그리기만 했는데 다른 페이지로 이동함 "
                            + context.navigations);
                }
            });
        }
    }
}
