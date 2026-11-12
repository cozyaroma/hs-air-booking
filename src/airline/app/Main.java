package airline.app;

import airline.service.AppServices;
import airline.ui.Application;

/**
 * 프로그램 시작점: 서비스를 조립하고(AppFactory) 로그인 창부터 띄운다(Application).
 */
public class Main {

    public static void main(String[] args) {
        AppServices services = AppFactory.createDefault();
        String hint = "시연용 계정: " + SampleData.DEMO_USERNAME + " / " + SampleData.DEMO_PASSWORD;
        System.out.println("[" + hint + "]");
        Application.launch(services, hint);
    }
}
