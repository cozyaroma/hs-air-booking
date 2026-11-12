package airline.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 색상·글꼴·공통 컴포넌트 모음 (공용 파일). 페이지마다 색을 따로 정하지 말고 여기 값을 쓴다.
 * 새 공용 스타일이 필요하면 통합 담당에게 요청한다.
 */
public final class UiTheme {

    public static final Color PRIMARY = new Color(0x1D4F91);
    public static final Color PRIMARY_DARK = new Color(0x143A6B);
    public static final Color PRIMARY_LIGHT = new Color(0xE6EEF8);
    public static final Color BACKGROUND = new Color(0xF3F5F8);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xD8DDE4);
    public static final Color TEXT = new Color(0x1F2633);
    public static final Color MUTED = new Color(0x6A7381);
    public static final Color DANGER = new Color(0xB8322A);
    public static final Color SUCCESS = new Color(0x1E7F52);
    public static final Color WARNING = new Color(0xC9701F);

    public static final Color SEAT_TAKEN = new Color(0xCDD2DA);
    public static final Color SEAT_EXIT = new Color(0xFDEBD8);
    public static final Color SEAT_OTHER_CABIN = new Color(0xF1F2F4);

    private static String fontFamily = Font.DIALOG;

    private UiTheme() {
    }

    /** 프로그램 시작 시 한 번 호출: 운영체제에 상관없이 같은 모양이 나오도록 설정한다. */
    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // 기본 모양을 그대로 사용
        }
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        fontFamily = pickKoreanFont();
        FontUIResource base = new FontUIResource(fontFamily, Font.PLAIN, 14);
        for (Object key : new ArrayList<>(UIManager.getLookAndFeelDefaults().keySet())) {
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, base);
            }
        }
        UIManager.put("ToolTip.font", new FontUIResource(fontFamily, Font.PLAIN, 13));
    }

    private static String pickKoreanFont() {
        Set<String> available = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String candidate : List.of("맑은 고딕", "Malgun Gothic", "Apple SD Gothic Neo",
                "Noto Sans CJK KR", "NanumGothic", "나눔고딕")) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.DIALOG;
    }

    public static Font font(int style, int size) {
        return new Font(fontFamily, style, size);
    }

    public static JButton primaryButton(String text) {
        return button(text, PRIMARY, Color.WHITE, PRIMARY);
    }

    public static JButton secondaryButton(String text) {
        return button(text, Color.WHITE, PRIMARY, BORDER);
    }

    public static JButton dangerButton(String text) {
        return button(text, Color.WHITE, DANGER, new Color(0xE3B5B1));
    }

    private static JButton button(String text, Color background, Color foreground, Color border) {
        JButton button = new JButton(text);
        button.setFont(font(Font.BOLD, 14));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border), BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JPanel card(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(CARD);
        panel.setBorder(cardBorder());
        return panel;
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(16, 18, 16, 18));
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font(Font.BOLD, 22));
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font(Font.BOLD, 16));
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font(Font.PLAIN, 13));
        label.setForeground(MUTED);
        return label;
    }


    /** 작은 꼬리표 라벨. 예) [가는 편], [국제선] */
    public static JLabel tag(String text, Color background, Color foreground) {
        JLabel label = new JLabel(text);
        label.setFont(font(Font.BOLD, 12));
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(foreground);
        label.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        return label;
    }

    /** 세로로 스크롤되는 영역 (가로 스크롤 없음) */
    public static JScrollPane scroll(JComponent content) {
        JScrollPane pane = new JScrollPane(content,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.getViewport().setBackground(BACKGROUND);
        pane.getVerticalScrollBar().setUnitIncrement(16);
        return pane;
    }

    /** 줄바꿈(\n)이 있는 문장을 JLabel에서 여러 줄로 보이게 한다. */
    public static String html(String text) {
        return "<html>" + text.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>") + "</html>";
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(32);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(PRIMARY_LIGHT);
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        table.setBackground(CARD);
        JTableHeader header = table.getTableHeader();
        header.setFont(font(Font.BOLD, 13));
        header.setBackground(new Color(0xEEF1F5));
        header.setForeground(TEXT);
        header.setReorderingAllowed(false);
    }
}
