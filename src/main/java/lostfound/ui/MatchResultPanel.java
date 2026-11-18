package lostfound.ui;

import lostfound.domain.Item;
import lostfound.matching.MatchResult;
import lostfound.matching.ScoreDetail;
import lostfound.service.LostFoundService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 3번 탭: 매칭 후보 목록, ScoreDetail 점수 근거, 비공개 특징 대조 및 관리자 회수 승인.
 */
public class MatchResultPanel extends JPanel {

    private final LostFoundService service;
    private final Runnable onApprovalCompleted;

    private final DefaultTableModel tableModel;
    private final JTable resultTable;
    private final JTextArea scoreDetailArea = new JTextArea(4, 40);
    private final JLabel secretInfoLabel = new JLabel("후보 물품을 선택하면 비공개 특징이 표시됩니다.");
    private final JTextField verifierNoteField = new JTextField(24);

    private List<MatchResult> currentResults = new ArrayList<>();

    public MatchResultPanel(LostFoundService service, Runnable onApprovalCompleted) {
        this.service = service;
        this.onApprovalCompleted = onApprovalCompleted;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("매칭 후보 목록 및 점수 근거 (반환 완료 물품 자동 제외)");
        title.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        add(title, BorderLayout.NORTH);

        String[] cols = {"순위", "ID", "물품명", "종류/색상", "습득 위치", "세부 속성", "최종점수", "추천 이유"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultTable = new JTable(tableModel);
        resultTable.setRowHeight(26);
        resultTable.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        resultTable.getColumnModel().getColumn(5).setPreferredWidth(160);
        resultTable.getColumnModel().getColumn(6).setPreferredWidth(60);
        resultTable.getColumnModel().getColumn(7).setPreferredWidth(230);

        resultTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshSelectionDetail();
            }
        });

        // 하단: 점수 근거 상세 + 관리자 회수 승인 영역
        JPanel bottomContainer = new JPanel(new BorderLayout(8, 8));

        scoreDetailArea.setEditable(false);
        scoreDetailArea.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        scoreDetailArea.setBackground(new Color(248, 250, 252));
        JScrollPane detailScroll = new JScrollPane(scoreDetailArea);
        detailScroll.setBorder(new TitledBorder("선택 물품 규칙별 점수 근거 (ScoreDetail)"));
        bottomContainer.add(detailScroll, BorderLayout.CENTER);

        JPanel approvalPanel = new JPanel(new GridBagLayout());
        approvalPanel.setBorder(new TitledBorder("소유자 확인 및 관리자 회수 승인"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        approvalPanel.add(new JLabel("등록된 비공개 특징:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.8;
        secretInfoLabel.setForeground(new Color(180, 40, 40));
        secretInfoLabel.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        approvalPanel.add(secretInfoLabel, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2;
        approvalPanel.add(new JLabel("소유자 진술 확인:"), gbc);

        gbc.gridx = 1; gbc.weightx = 0.8;
        JPanel actionLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton approveBtn = new JButton("관리자 회수 승인 (반환 완료)");
        approveBtn.setBackground(new Color(33, 115, 70));
        approveBtn.setForeground(Color.WHITE);
        approveBtn.setFocusPainted(false);
        approveBtn.addActionListener(e -> handleApprove());

        actionLine.add(verifierNoteField);
        actionLine.add(approveBtn);
        approvalPanel.add(actionLine, gbc);

        bottomContainer.add(approvalPanel, BorderLayout.SOUTH);

        add(new JScrollPane(resultTable), BorderLayout.CENTER);
        add(bottomContainer, BorderLayout.SOUTH);
    }

    public void updateResults(List<MatchResult> results) {
        this.currentResults = results;
        tableModel.setRowCount(0);

        for (int i = 0; i < results.size(); i++) {
            MatchResult r = results.get(i);
            Item item = r.item();
            String floorStr = item.getFloor() == -1 ? "지하1층" : item.getFloor() + "층";
            tableModel.addRow(new Object[]{
                    (i + 1) + "위",
                    item.getId(),
                    item.getTitle(),
                    item.getCategory().label() + " / " + item.getColor().label(),
                    item.getBuilding().label() + " " + floorStr,
                    item.specificSummary(),
                    r.finalScore() + "점",
                    r.reason()
            });
        }

        if (!results.isEmpty()) {
            resultTable.setRowSelectionInterval(0, 0);
        } else {
            scoreDetailArea.setText("조건에 일치하는 미반환 습득물이 없습니다.");
            secretInfoLabel.setText("-");
        }
    }

    private void refreshSelectionDetail() {
        int idx = resultTable.getSelectedRow();
        if (idx < 0 || idx >= currentResults.size()) return;

        MatchResult r = currentResults.get(idx);
        Item item = r.item();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("최종 점수: %d점 (규칙: %d점, 의미재평가: %d점) | 공개 특징: %s%n",
                r.finalScore(), r.ruleScore(), r.semanticScore(), item.getPublicDetail()));
        for (ScoreDetail d : r.details()) {
            sb.append(String.format(" - [%s] %d/%d점 (비교가능=%s) : %s%n",
                    d.ruleName(), d.earned(), d.max(), d.comparable() ? "O" : "X", d.reason()));
        }
        scoreDetailArea.setText(sb.toString());
        secretInfoLabel.setText("[관리자 확인용] " + item.getSecret());
    }

    private void handleApprove() {
        int idx = resultTable.getSelectedRow();
        if (idx < 0 || idx >= currentResults.size()) {
            JOptionPane.showMessageDialog(this, "승인할 물품을 목록에서 선택해 주세요.", "안내", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (verifierNoteField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "소유자가 일치하게 진술한 비공개 특징 메모를 입력해 주세요.", "소유자 확인 필요", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Item selected = currentResults.get(idx).item();
        int ans = JOptionPane.showConfirmDialog(this,
                "[" + selected.getTitle() + "] 물품을 반환 완료(RETURNED) 처리하시겠습니까?\n승인 후에는 검색 결과에서 제외됩니다.",
                "관리자 최종 회수 승인", JOptionPane.YES_NO_OPTION);

        if (ans == JOptionPane.YES_OPTION) {
            service.approveRecovery(selected.getId());
            verifierNoteField.setText("");
            JOptionPane.showMessageDialog(this, "회수 승인이 완료되었습니다.", "승인 완료", JOptionPane.INFORMATION_MESSAGE);
            onApprovalCompleted.run();
        }
    }
}
