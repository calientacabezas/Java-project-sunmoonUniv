package lostfound.ui;

import lostfound.domain.Building;
import lostfound.domain.Category;
import lostfound.domain.ItemColor;
import lostfound.matching.SearchQuery;
import lostfound.service.LostFoundService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 메인 프레임: 상단 장애 대응(온라인/오프라인) 상태 표시 및 3개 핵심 화면 탭 관리.
 */
public class MainFrame extends JFrame {

    private final LostFoundService service;
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JLabel modeBadge = new JLabel();

    public MainFrame(LostFoundService service) {
        this.service = service;

        setTitle("교내 분실물 회수 지원 시스템 - 선문대학교");
        setSize(980, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 상단 타이틀 및 장애 대응 상태 표시줄
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(242, 245, 250));
        topBar.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel appTitle = new JLabel("교내 분실물 회수 지원 시스템");
        appTitle.setFont(new Font("맑은 고딕", Font.BOLD, 17));
        topBar.add(appTitle, BorderLayout.WEST);

        JPanel rightStatus = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightStatus.setOpaque(false);

        JButton toggleModeBtn = new JButton("장애대응(오프라인/온라인) 전환");
        toggleModeBtn.addActionListener(e -> {
            service.setForceOfflineOverride(service.isOnlineMode());
            refreshModeLabel();
        });

        rightStatus.add(modeBadge);
        rightStatus.add(toggleModeBtn);
        topBar.add(rightStatus, BorderLayout.EAST);
        refreshModeLabel();

        // 3개 핵심 패널 연결
        MatchResultPanel resultPanel = new MatchResultPanel(service, this::reloadAllStoredItems);

        SearchMatchPanel searchPanel = new SearchMatchPanel(service, results -> {
            resultPanel.updateResults(results);
            tabbedPane.setSelectedIndex(2);
        });

        ItemRegisterPanel registerPanel = new ItemRegisterPanel(service, this::reloadAllStoredItems);

        tabbedPane.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        tabbedPane.addTab("1. 습득물 등록", registerPanel);
        tabbedPane.addTab("2. 분실물 검색 (자연어/조건)", searchPanel);
        tabbedPane.addTab("3. 매칭 결과 및 회수 승인", resultPanel);

        reloadAllStoredItems();

        add(topBar, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void reloadAllStoredItems() {
        SearchQuery allQuery = new SearchQuery(
                Category.UNKNOWN, ItemColor.UNKNOWN, null, Building.UNKNOWN, null, "", "");
        Component comp = tabbedPane.getComponentAt(2);
        if (comp instanceof MatchResultPanel panel) {
            panel.updateResults(service.searchMatches(allQuery));
        }
    }

    private void refreshModeLabel() {
        if (service.isOnlineMode()) {
            modeBadge.setText("● 온라인 모드 (LLM 조건 추출 + 하이브리드 매칭 활성)");
            modeBadge.setForeground(new Color(25, 110, 60));
        } else {
            modeBadge.setText("▲ 오프라인 장애대응 모드 (선택형 입력 + 규칙 점수 동작)");
            modeBadge.setForeground(new Color(190, 95, 0));
        }
        modeBadge.setFont(new Font("맑은 고딕", Font.BOLD, 12));
    }
}
