package airline.ui;

import airline.domain.user.User;
import airline.service.AppServices;
import airline.ui.login.LoginFrame;

import javax.swing.SwingUtilities;

/**
 * 창 전환 담당 (공용 파일): 로그인 창 ↔ 메인 창.
 *
 *   프로그램 시작 → 로그인 창
 *   로그인 성공 또는 회원가입 성공 → 로그인 창 닫고 메인 창 열기 (회원가입은 바로 로그인 상태)
 *   메인 창에서 로그아웃 → 메인 창 닫고 새 로그인 창 열기 (진행 중이던 예약 정보는 버림)
 */
public final class Application {

    private final AppServices services;
    private final String loginHint;
    private final boolean devMode;

    private Application(AppServices services, String loginHint, boolean devMode) {
        this.services = services;
        this.loginHint = loginHint;
        this.devMode = devMode;
    }

    /** 일반 실행: 로그인 창부터 */
    public static void launch(AppServices services, String loginHint) {
        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
            new Application(services, loginHint, false).showLogin();
        });
    }

    /** 개발용 실행(PageLauncher): 이미 로그인한 상태로 특정 페이지부터, 세션 검사 결과를 콘솔에 출력 */
    public static void launchAt(AppServices services, User user, BookingSession session, Screen start) {
        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
            new Application(services, null, true).openMain(user, session, start);
        });
    }

    private void showLogin() {
        LoginFrame loginFrame = new LoginFrame(services.auth(), loginHint);
        loginFrame.setOnSuccess(user -> {
            loginFrame.dispose();
            openMain(user, new BookingSession(), Screen.SEARCH);
        });
        loginFrame.setVisible(true);
    }

    private void openMain(User user, BookingSession session, Screen start) {
        MainFrame frame = new MainFrame(services, user, session, devMode);
        frame.setOnLogout(() -> {
            services.auth().logout();
            frame.dispose();
            showLogin();
        });
        frame.setVisible(true);
        frame.navigate(start);
    }
}
