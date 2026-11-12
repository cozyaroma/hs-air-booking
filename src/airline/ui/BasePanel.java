package airline.ui;

import airline.exception.AirlineException;
import airline.ui.common.TicketSummaryBar;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * 모든 페이지의 공통 틀 (공용 파일). 페이지는 이 클래스를 상속해서 본문만 만든다.
 *
 *   ┌ 단계 표시줄 (1. 항공권 검색 > 2. 항공권 선택 > ...)
 *   ├ 제목 / 부제목                        ← Screen에 정의된 값
 *   ├ 선택한 항공권 요약 (승객 정보~결제)    ← TicketSummaryBar, 자동으로 갱신
 *   ├ 본문                                 ← setContent()로 각 페이지가 채움
 *   └ 하단 버튼 (왼쪽: 이전 / 오른쪽: 다음) ← addBackButton(), addFooterButton()
 *
 * [상속] 화면마다 반복되는 배치와 알림창 코드를 여기 한 번만 작성했다.
 * [추상성] 페이지가 보일 때 무엇을 새로 그릴지(onShow)는 각 페이지가 반드시 구현한다.
 * [템플릿 메서드] MainFrame은 refresh()만 부르고, refresh()가 요약 바 갱신 후 onShow()를 부른다.
 */
public abstract class BasePanel extends JPanel {

    protected final AppContext app;
    private final Screen screen;
    private final JLabel subtitleLabel;
    private final TicketSummaryBar summaryBar;
    private final JPanel body = new JPanel(new BorderLayout());
    private final JPanel footerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    private final JPanel footerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

    protected BasePanel(AppContext app, Screen screen) {
        super(new BorderLayout(0, 14));
        this.app = app;
        this.screen = screen;
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 32, 18, 32));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        if (screen.getStep() > 0) {
            top.add(createStepIndicator(screen.getStep()));
            top.add(Box.createVerticalStrut(10));
        }
        JLabel titleLabel = UiTheme.title(screen.getTitle());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel = UiTheme.muted(screen.getSubtitle());
        subtitleLabel.setFont(UiTheme.font(Font.PLAIN, 14));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(titleLabel);
        top.add(Box.createVerticalStrut(4));
        top.add(subtitleLabel);
        if (screen.showsTicketSummary()) {
            summaryBar = new TicketSummaryBar();
            summaryBar.setAlignmentX(Component.LEFT_ALIGNMENT);
            top.add(Box.createVerticalStrut(12));
            top.add(summaryBar);
        } else {
            summaryBar = null;
        }

        body.setOpaque(false);
        footerLeft.setOpaque(false);
        footerRight.setOpaque(false);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(footerLeft, BorderLayout.WEST);
        footer.add(footerRight, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private JComponent createStepIndicator(int currentStep) {
        JPanel steps = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        steps.setOpaque(false);
        steps.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Screen flowScreen : Screen.BOOKING_FLOW) {
            int stepNumber = flowScreen.getStep();
            JLabel label = new JLabel(stepNumber + ". " + flowScreen.getTitle());
            if (stepNumber == currentStep) {
                label.setFont(UiTheme.font(Font.BOLD, 13));
                label.setForeground(UiTheme.PRIMARY);
            } else {
                label.setFont(UiTheme.font(Font.PLAIN, 13));
                label.setForeground(stepNumber < currentStep ? UiTheme.TEXT : UiTheme.MUTED);
            }
            steps.add(label);
            if (stepNumber < Screen.BOOKING_FLOW.size()) {
                JLabel arrow = new JLabel("   >   ");
                arrow.setForeground(UiTheme.BORDER);
                steps.add(arrow);
            }
        }
        return steps;
    }

    // ===== MainFrame이 부르는 부분 =====

    /** 페이지가 보일 때마다 MainFrame이 호출한다. 요약 바를 갱신하고 onShow()를 부른다. */
    public final void refresh() {
        if (summaryBar != null) {
            summaryBar.update(app.getSession());
        }
        onShow();
    }

    public final Screen getScreen() {
        return screen;
    }

    // ===== 각 페이지가 구현·사용하는 부분 =====

    /**
     * 페이지가 보일 때마다 호출된다. 세션(BookingSession)의 최신 내용으로 다시 그린다.
     * 주의: 여기서 알림창(JOptionPane)을 띄우지 않는다. (자동 테스트가 화면 없이 이 메서드를 부른다)
     */
    protected abstract void onShow();

    protected BookingSession session() {
        return app.getSession();
    }

    /** 본문을 바꾼다 */
    protected void setContent(JComponent content) {
        body.removeAll();
        body.add(content, BorderLayout.CENTER);
        body.revalidate();
        body.repaint();
    }

    /** 본문이 길 때: 세로 스크롤 안에 넣어서 바꾼다. 내용은 위쪽부터 쌓이고 남는 공간은 비워 둔다. */
    protected void setScrollableContent(JComponent content) {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(content, BorderLayout.NORTH);
        setContent(UiTheme.scroll(top));
    }

    protected void setSubtitle(String text) {
        subtitleLabel.setText(text);
    }

    /** 하단 오른쪽 버튼 (다음, 결제하기 등). 추가한 순서대로 왼쪽→오른쪽에 놓인다. */
    protected void addFooterButton(JButton button) {
        footerRight.add(button);
    }

    /** 하단 왼쪽에 "이전" 버튼을 만든다. 누르면 단계 표시줄의 앞 페이지로 이동한다. */
    protected JButton addBackButton() {
        JButton back = UiTheme.secondaryButton("이전");
        Screen previous = screen.previous();
        back.setEnabled(previous != null);
        back.addActionListener(e -> app.navigate(previous));
        footerLeft.add(back);
        return back;
    }

    protected void addFooterLeft(JComponent component) {
        footerLeft.add(component);
    }

    /** 업무 규칙 위반: 예외 종류마다 제목이 다르다 (AirlineException.getTitle 오버라이딩) */
    protected void showError(AirlineException e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), e.getTitle(), JOptionPane.WARNING_MESSAGE);
    }

    protected void showWarning(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    protected void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    protected boolean confirm(String title, String message) {
        return JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
