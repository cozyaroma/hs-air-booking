package airline.ui.login;

import airline.domain.user.User;
import airline.exception.AuthException;
import airline.service.AuthService;
import airline.ui.UiTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.function.Consumer;

/**
 * 회원가입/로그인 창 (공용 파일). 프로그램을 켜면 가장 먼저 나온다.
 *
 *  - 로그인: 아이디 + 비밀번호
 *  - 회원가입: 아이디(중복 확인) + 비밀번호 + 비밀번호 확인 → 가입과 동시에 로그인되어 메인 창으로 들어간다
 *  - 오류는 알림창 대신 입력칸 아래 빨간 글씨로 보여준다
 */
public class LoginFrame extends JFrame {

    private static final String LOGIN = "login";
    private static final String SIGN_UP = "signUp";

    private final AuthService auth;
    private Consumer<User> onSuccess = user -> { };

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final JButton loginTab = tabButton("로그인");
    private final JButton signUpTab = tabButton("회원가입");

    private final JTextField loginId = new JTextField();
    private final JPasswordField loginPassword = new JPasswordField();
    private final JLabel loginMessage = messageLabel();
    private final JButton loginButton = UiTheme.primaryButton("로그인");

    private final JTextField signUpId = new JTextField();
    private final JPasswordField signUpPassword = new JPasswordField();
    private final JPasswordField signUpConfirm = new JPasswordField();
    private final JLabel signUpMessage = messageLabel();
    private final JButton signUpButton = UiTheme.primaryButton("가입하고 시작하기");

    /** @param hint 로그인 폼 아래에 보여줄 안내 (예: 시연용 계정). 없으면 null */
    public LoginFrame(AuthService auth, String hint) {
        super("HS AIR 로그인");
        this.auth = auth;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 560);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(UiTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        root.add(createBrand(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        JPanel tabs = new JPanel(new GridLayout(1, 2, 0, 0));
        tabs.add(loginTab);
        tabs.add(signUpTab);
        center.add(tabs, BorderLayout.NORTH);
        cards.setOpaque(false);
        cards.add(createLoginForm(hint), LOGIN);
        cards.add(createSignUpForm(), SIGN_UP);
        center.add(cards, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        setContentPane(root);

        loginTab.addActionListener(e -> showTab(LOGIN));
        signUpTab.addActionListener(e -> showTab(SIGN_UP));
        loginButton.addActionListener(e -> doLogin());
        signUpButton.addActionListener(e -> doSignUp());
        showTab(LOGIN);
    }

    /** 로그인(또는 가입) 성공 시 할 일. Application이 메인 창을 여는 동작을 넣는다. */
    public void setOnSuccess(Consumer<User> onSuccess) {
        this.onSuccess = onSuccess;
    }

    private JComponent createBrand() {
        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setOpaque(false);
        JLabel logo = new JLabel("HS AIR");
        logo.setFont(UiTheme.font(Font.BOLD, 30));
        logo.setForeground(UiTheme.PRIMARY_DARK);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = UiTheme.muted("항공권 예매 시스템");
        subtitle.setFont(UiTheme.font(Font.PLAIN, 14));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        brand.add(logo);
        brand.add(Box.createVerticalStrut(4));
        brand.add(subtitle);
        return brand;
    }

    private JComponent createLoginForm(String hint) {
        JPanel form = formPanel();
        addField(form, "아이디", loginId);
        addField(form, "비밀번호", loginPassword);
        form.add(loginMessage);
        form.add(Box.createVerticalStrut(8));
        addWide(form, loginButton);
        if (hint != null) {
            form.add(Box.createVerticalStrut(14));
            JLabel hintLabel = UiTheme.muted(hint);
            hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(hintLabel);
        }
        return form;
    }

    private JComponent createSignUpForm() {
        JPanel form = formPanel();
        JButton checkButton = UiTheme.secondaryButton("중복 확인");
        checkButton.addActionListener(e -> checkUsername());
        JPanel idRow = new JPanel(new BorderLayout(8, 0));
        idRow.setOpaque(false);
        idRow.add(signUpId, BorderLayout.CENTER);
        idRow.add(checkButton, BorderLayout.EAST);
        addField(form, "아이디  (" + AuthService.USERNAME_RULE + ")", idRow);
        addField(form, "비밀번호  (" + AuthService.PASSWORD_RULE + ")", signUpPassword);
        addField(form, "비밀번호 확인", signUpConfirm);
        form.add(signUpMessage);
        form.add(Box.createVerticalStrut(8));
        addWide(form, signUpButton);
        return form;
    }

    private void showTab(String name) {
        cardLayout.show(cards, name);
        boolean login = LOGIN.equals(name);
        styleTab(loginTab, login);
        styleTab(signUpTab, !login);
        getRootPane().setDefaultButton(login ? loginButton : signUpButton);   // Enter 키로 제출
        loginMessage.setText(" ");
        signUpMessage.setText(" ");
        (login ? loginId : signUpId).requestFocusInWindow();
    }

    private void doLogin() {
        try {
            User user = auth.login(loginId.getText(), new String(loginPassword.getPassword()));
            onSuccess.accept(user);
        } catch (AuthException e) {
            showMessage(loginMessage, e.getMessage(), UiTheme.DANGER);
            loginPassword.setText("");
        }
    }

    private void doSignUp() {
        try {
            User user = auth.signUp(signUpId.getText(), new String(signUpPassword.getPassword()),
                    new String(signUpConfirm.getPassword()));
            onSuccess.accept(user);   // 가입과 동시에 로그인 → 바로 메인 창으로
        } catch (AuthException e) {
            showMessage(signUpMessage, e.getMessage(), UiTheme.DANGER);
        }
    }

    private void checkUsername() {
        if (auth.isUsernameAvailable(signUpId.getText())) {
            showMessage(signUpMessage, "사용할 수 있는 아이디입니다.", UiTheme.SUCCESS);
        } else {
            showMessage(signUpMessage, "사용할 수 없는 아이디입니다. (" + AuthService.USERNAME_RULE + ", 중복 불가)", UiTheme.DANGER);
        }
    }

    private static void showMessage(JLabel label, String text, Color color) {
        label.setForeground(color);
        label.setText(UiTheme.html(text));
    }

    // ===== 배치 도우미 =====

    private static JPanel formPanel() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(UiTheme.CARD);
        form.setBorder(UiTheme.cardBorder());
        return form;
    }

    private static void addField(JPanel form, String label, JComponent field) {
        JLabel title = new JLabel(label);
        title.setFont(UiTheme.font(Font.BOLD, 13));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setPreferredSize(new Dimension(300, 34));
        form.add(title);
        form.add(Box.createVerticalStrut(4));
        form.add(field);
        form.add(Box.createVerticalStrut(12));
    }

    private static void addWide(JPanel form, JButton button) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        wrap.add(button, BorderLayout.CENTER);
        form.add(wrap);
    }

    private static JLabel messageLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(UiTheme.font(Font.PLAIN, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JButton tabButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UiTheme.font(Font.BOLD, 15));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        return button;
    }

    private static void styleTab(JButton tab, boolean selected) {
        tab.setBackground(selected ? UiTheme.PRIMARY : Color.WHITE);
        tab.setForeground(selected ? Color.WHITE : UiTheme.MUTED);
        tab.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? UiTheme.PRIMARY : UiTheme.BORDER),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)));
    }
}
