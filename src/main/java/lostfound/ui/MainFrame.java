package lostfound.ui;

import lostfound.service.LostFoundService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * 메인 프레임: 상단 헤더(서비스 이름·온라인/오프라인 상태)와 3개 핵심 화면 탭.
 * 담당: 김경린, 디자인 개편: 강채연.
 */
public class MainFrame extends JFrame {

    private final LostFoundService service;
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final Theme.Chip modeChip = Theme.chip("", Theme.NEUTRAL_SOFT, Theme.MUTED);
    private final Theme.RoundButton toggleModeBtn = Theme.ghostButton("");

    public MainFrame(LostFoundService service) {
        this.service = service;

        setTitle("선문 분실물 찾기 - 교내 분실물 회수 지원 시스템");
        setSize(1180, 780);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        MatchResultPanel resultPanel = new MatchResultPanel(service, this::reloadAllStoredItems);
        SearchMatchPanel searchPanel = new SearchMatchPanel(service, results -> {
            resultPanel.updateResults(results);
            tabbedPane.setSelectedIndex(2);
        });
        ItemRegisterPanel registerPanel = new ItemRegisterPanel(service, this::reloadAllStoredItems);

        tabbedPane.setFont(Theme.BODY_BOLD);
        tabbedPane.setBackground(Theme.BG);
        tabbedPane.addTab("  습득물 등록  ", registerPanel);
        tabbedPane.addTab("  분실물 찾기  ", searchPanel);
        tabbedPane.addTab("  결과 · 회수 승인  ", resultPanel);
        tabbedPane.setSelectedIndex(1);   // 사용자가 가장 많이 쓰는 검색 화면부터

        reloadAllStoredItems();
        add(tabbedPane, BorderLayout.CENTER);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(14, 24, 14, 24)));

        // 왼쪽: 로고 + 이름
        JLabel logo = new JLabel(new LogoIcon());
        JLabel name = new JLabel("선문 분실물 찾기");
        name.setFont(Theme.font(Font.BOLD, 18));
        name.setForeground(Theme.TEXT);
        JLabel sub = Theme.muted("LLM과 하이브리드 유사도로 주인을 찾아 드려요");
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(name);
        titles.add(sub);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(logo);
        left.add(titles);

        // 오른쪽: 상태 배지 + 전환 버튼
        toggleModeBtn.setFont(Theme.SMALL);
        toggleModeBtn.addActionListener(e -> {
            service.setForceOfflineOverride(service.isOnlineMode());
            refreshModeLabel();
        });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        right.setOpaque(false);
        right.add(modeChip);
        right.add(toggleModeBtn);
        refreshModeLabel();

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void reloadAllStoredItems() {
        Component comp = tabbedPane.getComponentAt(2);
        if (comp instanceof MatchResultPanel panel) {
            panel.updateResults(service.listAllMatchable());
        }
    }

    private void refreshModeLabel() {
        if (service.isOnlineMode()) {
            modeChip.setText("● 온라인 · AI 조건 추출과 의미 보정 사용 중");
            modeChip.setColors(Theme.SUCCESS_SOFT, Theme.SUCCESS);
            toggleModeBtn.setText("오프라인으로 전환 (장애 시연)");
        } else {
            modeChip.setText("● 오프라인 · 규칙 점수로 동작 중");
            modeChip.setColors(Theme.WARNING_SOFT, Theme.WARNING);
            toggleModeBtn.setText("온라인으로 전환");
        }
        modeChip.revalidate();
    }

    /** 파란 둥근 사각형 안에 돋보기 모양을 그린 로고. */
    private static final class LogoIcon implements Icon {
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.PRIMARY);
            g2.fillRoundRect(x, y, 40, 40, 12, 12);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(x + 10, y + 9, 15, 15);
            g2.drawLine(x + 23, y + 22, x + 30, y + 29);
            g2.dispose();
        }
        @Override public int getIconWidth() { return 40; }
        @Override public int getIconHeight() { return 40; }
    }
}
