package airline.ui;

import airline.domain.user.User;
import airline.service.AppServices;
import airline.service.BookingService;
import airline.service.FlightSearchService;
import airline.util.AppClock;
import airline.util.Formats;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 메인 창 (공용 파일). 상단 헤더와 페이지 영역(CardLayout)으로 이루어진다.
 *
 * 헤더: 로고 · [항공권 예매] [내 예약] 탭 · 현재 시각 · 시각 이동(시연용) · 아이디 · [로그아웃]
 * 페이지 이동(navigate)은 항상 여기를 거친다:
 *   1) 이동할 페이지의 입장 조건(Screen.check)을 검사한다
 *   2) 조건이 안 맞으면 안내하고, 열 수 있는 가장 가까운 앞 페이지로 보낸다
 *   3) 페이지를 처음 열 때 만들고(PageRegistry), 매번 refresh()로 다시 그린다
 * 한 페이지에서 예외가 나도 프로그램 전체가 멈추지 않도록 막아 두었다 (세 명이 동시에 개발하므로).
 */
public class MainFrame extends JFrame implements AppContext {

    private final AppServices services;
    private final User user;
    private final BookingSession session;
    private final boolean devMode;
    private Runnable onLogout = () -> { };

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final Map<Screen, BasePanel> pages = new EnumMap<>(Screen.class);
    private Screen currentScreen;

    private final JLabel clockLabel = new JLabel();
    private final JButton bookingTab = headerButton("항공권 예매");
    private final JButton myBookingsTab = headerButton("내 예약");
    private final Runnable clockListener = this::onClockChanged;
    private final Timer clockTimer = new Timer(30_000, e -> updateClockLabel());

    public MainFrame(AppServices services, User user, BookingSession session, boolean devMode) {
        super("HS AIR 항공권 예매 시스템" + (devMode ? "  [개발 모드]" : ""));
        this.services = services;
        this.user = user;
        this.session = session;
        this.devMode = devMode;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 860);
        setMinimumSize(new Dimension(1080, 720));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UiTheme.BACKGROUND);
        add(createHeader(), BorderLayout.NORTH);
        add(cards, BorderLayout.CENTER);

        services.clock().addChangeListener(clockListener);
        clockTimer.start();   // 실제 시간이 흐르는 것도 반영
        updateClockLabel();
    }

    /** 로그아웃 버튼을 눌렀을 때 할 일 (Application이 로그인 창으로 돌아가는 동작을 넣는다) */
    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    @Override
    public void dispose() {
        services.clock().removeChangeListener(clockListener);
        clockTimer.stop();
        super.dispose();
    }

    // ===== 헤더 =====

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiTheme.PRIMARY_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(10, 32, 10, 24));

        JLabel logo = new JLabel("HS AIR");
        logo.setFont(UiTheme.font(Font.BOLD, 22));
        logo.setForeground(Color.WHITE);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(logo);
        left.add(Box.createHorizontalStrut(36));
        left.add(bookingTab);
        left.add(myBookingsTab);
        bookingTab.addActionListener(e -> {
            if (currentScreen == null || !Screen.BOOKING_FLOW.contains(currentScreen)) {
                navigate(Screen.SEARCH);
            }
        });
        myBookingsTab.addActionListener(e -> navigate(Screen.MY_BOOKINGS));

        clockLabel.setForeground(new Color(0xD5E0EE));
        clockLabel.setFont(UiTheme.font(Font.PLAIN, 13));
        JButton clockButton = headerButton("시각 이동");
        JPopupMenu clockMenu = createClockMenu();
        clockButton.addActionListener(e -> clockMenu.show(clockButton, 0, clockButton.getHeight()));

        JLabel userLabel = new JLabel(user.getUsername() + " 님");
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(UiTheme.font(Font.BOLD, 14));
        JButton logoutButton = headerButton("로그아웃");
        logoutButton.addActionListener(e -> confirmLogout());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(clockLabel);
        right.add(clockButton);
        if (devMode) {
            JButton sessionButton = headerButton("세션 보기");
            sessionButton.addActionListener(e -> showSessionDump());
            right.add(sessionButton);
        }
        right.add(Box.createHorizontalStrut(10));
        right.add(userLabel);
        right.add(logoutButton);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    /** 시연용: 체크인 시간과 취소 수수료 변화를 보여주기 위해 시각을 앞으로 옮긴다. */
    private JPopupMenu createClockMenu() {
        JPopupMenu menu = new JPopupMenu();
        addClockItem(menu, "1시간 뒤로 이동", Duration.ofHours(1));
        addClockItem(menu, "6시간 뒤로 이동", Duration.ofHours(6));
        addClockItem(menu, "1일 뒤로 이동", Duration.ofDays(1));
        addClockItem(menu, "7일 뒤로 이동", Duration.ofDays(7));
        menu.addSeparator();
        JMenuItem reset = new JMenuItem("실제 시각으로 되돌리기");
        reset.addActionListener(e -> services.clock().reset());
        menu.add(reset);
        return menu;
    }

    private void addClockItem(JPopupMenu menu, String text, Duration amount) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> services.clock().advance(amount));
        menu.add(item);
    }

    private void confirmLogout() {
        int answer = JOptionPane.showConfirmDialog(this,
                "로그아웃하시겠습니까?\n진행 중인 예매 정보는 저장되지 않습니다.", "로그아웃",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            onLogout.run();
        }
    }

    private void showSessionDump() {
        JTextArea text = new JTextArea(session.describeForDebug(), 16, 70);
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JOptionPane.showMessageDialog(this, new JScrollPane(text), "BookingSession 내용 (개발용)",
                JOptionPane.PLAIN_MESSAGE);
    }

    private static JButton headerButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UiTheme.font(Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(UiTheme.PRIMARY_DARK);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void onClockChanged() {
        updateClockLabel();
        if (currentScreen != null) {
            refreshSafely(currentScreen, pages.get(currentScreen));
        }
    }

    private void updateClockLabel() {
        String text = "현재 시각  " + Formats.dateTime(services.clock().now());
        if (services.clock().isShifted()) {
            text += "  (시연용 이동)";
        }
        clockLabel.setText(text);
    }

    private void updateTabs() {
        boolean myBookings = currentScreen == Screen.MY_BOOKINGS;
        myBookingsTab.setBackground(myBookings ? UiTheme.PRIMARY : UiTheme.PRIMARY_DARK);
        bookingTab.setBackground(myBookings ? UiTheme.PRIMARY_DARK : UiTheme.PRIMARY);
    }

    // ===== 페이지 이동 =====

    @Override
    public void navigate(Screen target) {
        if (target == null) {
            return;
        }
        List<String> missing = target.check(session);
        if (devMode) {
            System.out.println("[세션 검사] " + (currentScreen == null ? "시작" : currentScreen) + " → " + target + " : "
                    + (missing.isEmpty() ? "통과" : "막힘 " + missing));
        }
        Screen destination = target;
        if (!missing.isEmpty()) {
            destination = nearestOpenScreen(target);
            JOptionPane.showMessageDialog(this,
                    "먼저 완료해야 할 단계가 있습니다.\n- " + String.join("\n- ", missing)
                            + "\n\n" + destination.getTitle() + " 페이지로 이동합니다.",
                    "이전 단계 확인", JOptionPane.INFORMATION_MESSAGE);
        }
        show(destination);
    }

    /** 입장 조건이 안 맞을 때 갈 곳: 예약 흐름에서 target보다 앞이면서 열 수 있는 가장 가까운 페이지 */
    private Screen nearestOpenScreen(Screen target) {
        int index = Screen.BOOKING_FLOW.indexOf(target);
        if (index < 0) {
            return Screen.SEARCH;
        }
        for (int i = index - 1; i >= 0; i--) {
            Screen candidate = Screen.BOOKING_FLOW.get(i);
            if (candidate.check(session).isEmpty()) {
                return candidate;
            }
        }
        return Screen.SEARCH;
    }

    private void show(Screen screen) {
        BasePanel page = pages.computeIfAbsent(screen, this::createPageSafely);
        currentScreen = screen;
        refreshSafely(screen, page);
        cardLayout.show(cards, screen.name());
        updateTabs();
    }

    /** 페이지 생성 중 예외가 나도 프로그램이 멈추지 않도록 대신 오류 안내 페이지를 넣는다. */
    private BasePanel createPageSafely(Screen screen) {
        BasePanel page;
        try {
            page = PageRegistry.create(screen, this);
        } catch (RuntimeException e) {
            e.printStackTrace();
            page = new BrokenPage(this, screen, e);
        }
        cards.add(page, screen.name());
        return page;
    }

    private void refreshSafely(Screen screen, BasePanel page) {
        try {
            page.refresh();
        } catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    screen.getTitle() + " 페이지를 그리는 중 오류가 발생했습니다.\n담당: "
                            + screen.getOwner().getMemberName() + "\n\n" + e,
                    "페이지 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== AppContext 구현 =====

    @Override
    public void startNewBooking() {
        session.reset();
        navigate(Screen.SEARCH);
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

    /** 페이지를 만들다 실패했을 때 대신 보여주는 페이지 */
    private static final class BrokenPage extends BasePanel {

        BrokenPage(AppContext app, Screen screen, RuntimeException error) {
            super(app, screen);
            JTextArea text = new JTextArea("이 페이지를 만드는 중 오류가 발생했습니다. (담당: "
                    + screen.getOwner().getMemberName() + ")\n\n" + error);
            text.setEditable(false);
            text.setLineWrap(true);
            text.setForeground(UiTheme.DANGER);
            setContent(text);
            addBackButton();
        }

        @Override
        protected void onShow() {
            // 보여줄 내용 없음
        }
    }
}
