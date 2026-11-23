package lostfound.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * 화면 공통 디자인(색·글꼴·카드·버튼·배지). 담당: 강채연.
 * 버튼·카드·배지는 직접 그리므로 운영체제 테마와 관계없이 같은 모양으로 보인다.
 * FlatLaf가 있으면 입력칸·스크롤바·탭도 현대적인 모양으로 바뀌고, 없으면 기본 테마로 동작한다.
 */
public final class Theme {
    private Theme() {}

    // ── 색상 ──────────────────────────────────────────
    public static final Color BG = new Color(0xF4F6FA);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xE3E8EF);
    public static final Color TEXT = new Color(0x1F2937);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color PRIMARY = new Color(0x2563EB);
    public static final Color PRIMARY_DARK = new Color(0x1D4ED8);
    public static final Color PRIMARY_SOFT = new Color(0xEAF1FF);
    public static final Color SUCCESS = new Color(0x15803D);
    public static final Color SUCCESS_SOFT = new Color(0xE7F6EC);
    public static final Color WARNING = new Color(0xB45309);
    public static final Color WARNING_SOFT = new Color(0xFEF3E2);
    public static final Color DANGER = new Color(0xB91C1C);
    public static final Color DANGER_SOFT = new Color(0xFDECEC);
    public static final Color NEUTRAL_SOFT = new Color(0xF1F3F6);

    public static final int ARC = 14;

    // ── 글꼴 ──────────────────────────────────────────
    private static final String FAMILY = pickFamily();

    public static Font font(int style, float size) {
        return new Font(FAMILY, style, Math.round(size));
    }

    public static final Font BODY = font(Font.PLAIN, 13);
    public static final Font BODY_BOLD = font(Font.BOLD, 13);
    public static final Font SMALL = font(Font.PLAIN, 12);
    public static final Font TITLE = font(Font.BOLD, 16);
    public static final Font HEADLINE = font(Font.BOLD, 20);

    private static String pickFamily() {
        List<String> available = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String f : List.of("맑은 고딕", "Malgun Gothic", "Apple SD Gothic Neo", "NanumGothic", "Noto Sans CJK KR")) {
            if (available.contains(f)) return f;
        }
        return Font.DIALOG;
    }

    /** 프로그램 시작 시 한 번 호출: 테마와 기본 글꼴을 적용한다. */
    public static void install() {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            UIManager.put("Component.arc", 10);
            UIManager.put("Button.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("TabbedPane.tabHeight", 40);
            UIManager.put("TabbedPane.selectedBackground", CARD);
            UIManager.put("TabbedPane.underlineColor", PRIMARY);
            UIManager.put("Component.focusColor", PRIMARY);
        } catch (Exception e) {
            // FlatLaf가 없으면 운영체제 기본 테마 사용 (직접 그리는 요소는 동일하게 보임)
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // 기본 테마 그대로
            }
        }
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof String k && k.endsWith(".font")) {
                UIManager.put(k, BODY);
            }
        }
        UIManager.put("Table.selectionBackground", PRIMARY_SOFT);
        UIManager.put("Table.selectionForeground", TEXT);
    }

    // ── 구성 요소 ─────────────────────────────────────

    /** 흰 배경에 둥근 모서리와 옅은 테두리가 있는 카드. title이 null이면 제목 없음. */
    public static JPanel card(String title, JComponent content) {
        RoundedPanel card = new RoundedPanel(CARD, BORDER);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        if (title != null) {
            JLabel t = new JLabel(title);
            t.setFont(TITLE);
            t.setForeground(TEXT);
            card.add(t, BorderLayout.NORTH);
        }
        if (content != null) {
            content.setOpaque(false);
            card.add(content, BorderLayout.CENTER);
        }
        return card;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(SMALL);
        l.setForeground(MUTED);
        return l;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BODY_BOLD);
        l.setForeground(TEXT);
        return l;
    }

    public static RoundButton primaryButton(String text) {
        return new RoundButton(text, PRIMARY, PRIMARY_DARK, Color.WHITE, null);
    }

    public static RoundButton successButton(String text) {
        return new RoundButton(text, SUCCESS, SUCCESS.darker(), Color.WHITE, null);
    }

    public static RoundButton ghostButton(String text) {
        return new RoundButton(text, CARD, NEUTRAL_SOFT, TEXT, BORDER);
    }

    public static Chip chip(String text, Color bg, Color fg) {
        return new Chip(text, bg, fg);
    }

    /** 점수 구간별 색: 80 이상 초록, 50 이상 주황, 그 밖은 회색. */
    public static Color[] scoreColors(int score) {
        if (score >= 80) return new Color[]{SUCCESS_SOFT, SUCCESS};
        if (score >= 50) return new Color[]{WARNING_SOFT, WARNING};
        return new Color[]{NEUTRAL_SOFT, MUTED};
    }

    // ── 직접 그리는 컴포넌트 ───────────────────────────

    public static class RoundedPanel extends JPanel {
        private final Color fill;
        private final Color line;

        public RoundedPanel(Color fill, Color line) {
            this.fill = fill;
            this.line = line;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            if (line != null) {
                g2.setColor(line);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class RoundButton extends JButton {
        private final Color bg;
        private final Color hover;
        private final Color line;
        private final Color fg;

        RoundButton(String text, Color bg, Color hover, Color fg, Color line) {
            super(text);
            this.bg = bg;
            this.hover = hover;
            this.line = line;
            this.fg = fg;
            setForeground(fg);
            setFont(BODY_BOLD);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 18, 10, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = !isEnabled() ? NEUTRAL_SOFT
                    : (getModel().isRollover() || getModel().isPressed()) ? hover : bg;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            if (line != null) {
                g2.setColor(line);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setForeground(enabled ? fg : MUTED);
        }
    }

    public static class Chip extends JLabel {
        private Color bg;

        Chip(String text, Color bg, Color fg) {
            super(text);
            setFont(font(Font.BOLD, 12));
            setColors(bg, fg);
            setBorder(new EmptyBorder(4, 10, 4, 10));
            setOpaque(false);
        }

        public void setColors(Color bg, Color fg) {
            this.bg = bg;
            setForeground(fg);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 규칙별 점수 막대 (획득/만점). 비교 불가 항목은 회색 빗금 대신 옅은 막대로 표시. */
    public static class ScoreBar extends JComponent {
        private final int earned;
        private final int max;
        private final boolean comparable;

        public ScoreBar(int earned, int max, boolean comparable) {
            this.earned = earned;
            this.max = max;
            this.comparable = comparable;
            setPreferredSize(new Dimension(120, 8));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = 8;
            int y = (getHeight() - h) / 2;
            g2.setColor(NEUTRAL_SOFT);
            g2.fillRoundRect(0, y, getWidth(), h, h, h);
            if (comparable && max > 0 && earned > 0) {
                int w = Math.max(h, (int) Math.round(getWidth() * (earned / (double) max)));
                g2.setColor(earned == max ? SUCCESS : PRIMARY);
                g2.fillRoundRect(0, y, w, h, h, h);
            }
            g2.dispose();
        }
    }
}
