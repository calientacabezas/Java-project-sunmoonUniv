package lostfound.ui;

import lostfound.domain.Item;
import lostfound.matching.MatchResult;
import lostfound.matching.ScoreDetail;
import lostfound.service.LostFoundService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 3번 탭: 매칭 후보 목록(사진·점수 배지), 선택 물품 상세(큰 사진·규칙별 점수 막대), 관리자 회수 승인.
 * 비공개 특징은 목록·상세에 표시하지 않고 승인 확인 창에서만 관리자에게 보여 준다.
 * 담당: 김경린, 디자인 개편·비공개 특징 보호: 강채연.
 */
public class MatchResultPanel extends JPanel {

    private static final int THUMB = 52;
    private static final int PHOTO = 120;
    private static final String SECRET_HIDDEN_TEXT = "비공개 · 승인 버튼을 누르면 관리자 확인 창에만 표시됩니다";

    private final LostFoundService service;
    private final Runnable onApprovalCompleted;

    private final DefaultTableModel tableModel;
    private final JTable resultTable;
    private final JLabel summaryLabel = Theme.muted("");

    // 상세 영역
    private final JLabel photoLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel nameLabel = new JLabel(" ");
    private final JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private final JLabel publicDetailLabel = new JLabel(" ");
    private final JPanel scoreRows = new JPanel();
    private final JLabel reasonLabel = new JLabel(" ");
    private final JTextField verifierNoteField = new JTextField();
    private final Theme.RoundButton approveBtn = Theme.successButton("소유자 확인 후 반환 승인");

    private List<MatchResult> currentResults = new ArrayList<>();

    public MatchResultPanel(LostFoundService service, Runnable onApprovalCompleted) {
        this.service = service;
        this.onApprovalCompleted = onApprovalCompleted;

        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel heading = new JPanel(new BorderLayout(0, 4));
        heading.setOpaque(false);
        JLabel title = new JLabel("찾은 결과");
        title.setFont(Theme.HEADLINE);
        title.setForeground(Theme.TEXT);
        heading.add(title, BorderLayout.NORTH);
        heading.add(summaryLabel, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        // ── 목록 ──
        String[] cols = {"사진", "순위", "물품", "습득 위치", "점수", "추천 이유"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        resultTable = new JTable(tableModel);
        styleTable();
        resultTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) refreshSelectionDetail();
        });
        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);
        JPanel listCard = Theme.card(null, tableScroll);
        listCard.setBorder(new EmptyBorder(6, 6, 6, 6));

        add(listCard, BorderLayout.CENTER);
        add(buildDetailPanel(), BorderLayout.EAST);
    }

    // ── 목록 스타일 ─────────────────────────────────────

    private void styleTable() {
        resultTable.setRowHeight(THUMB + 16);
        resultTable.setShowGrid(false);
        resultTable.setIntercellSpacing(new Dimension(0, 0));
        resultTable.setFillsViewportHeight(true);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.setFont(Theme.BODY);

        JTableHeader header = resultTable.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(Theme.font(Font.BOLD, 12));
        header.setBackground(Color.WHITE);
        header.setForeground(Theme.MUTED);
        header.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER));
        header.setPreferredSize(new Dimension(0, 36));

        int[] widths = {THUMB + 20, 50, 210, 130, 80, 300};
        for (int i = 0; i < widths.length; i++) {
            resultTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        resultTable.getColumnModel().getColumn(0).setMaxWidth(THUMB + 24);
        resultTable.getColumnModel().getColumn(1).setMaxWidth(60);
        resultTable.getColumnModel().getColumn(4).setMaxWidth(90);

        resultTable.setDefaultRenderer(Object.class, new BaseRenderer());
        resultTable.getColumnModel().getColumn(0).setCellRenderer(new PhotoRenderer());
        resultTable.getColumnModel().getColumn(2).setCellRenderer(new ItemRenderer());
        resultTable.getColumnModel().getColumn(4).setCellRenderer(new ScoreRenderer());
    }

    /** 줄무늬 배경·선택 색·여백을 적용하는 기본 렌더러. */
    private static class BaseRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, row, col);
            l.setBorder(new EmptyBorder(0, 10, 0, 10));
            l.setForeground(col == 5 ? Theme.MUTED : Theme.TEXT);
            l.setFont(col == 5 ? Theme.SMALL : Theme.BODY);
            l.setBackground(sel ? Theme.PRIMARY_SOFT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFBFD)));
            l.setHorizontalAlignment(col == 1 ? CENTER : LEFT);
            return l;
        }
    }

    private class PhotoRenderer extends BaseRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, "", sel, focus, row, col);
            l.setHorizontalAlignment(CENTER);
            if (row < currentResults.size()) {
                l.setIcon(Images.thumbnail(currentResults.get(row).item(), THUMB, THUMB));
            }
            return l;
        }
    }

    /** 물품명(굵게) + 종류·색상(회색) 두 줄. */
    private class ItemRenderer extends BaseRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, focus, row, col);
            l.setIcon(null);
            if (row < currentResults.size()) {
                Item item = currentResults.get(row).item();
                l.setText("<html><b>" + esc(item.getTitle()) + "</b><br><span style='color:#6B7280'>"
                        + esc(item.getCategory().label() + " · " + item.getColor().label()) + "</span></html>");
            }
            return l;
        }
    }

    /** 점수를 둥근 배지로 표시 (80+ 초록, 50+ 주황, 그 밖 회색). 전체 목록은 '-'. */
    private class ScoreRenderer extends BaseRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(t, "", sel, focus, row, col);
            if (row >= currentResults.size()) return cell;
            MatchResult r = currentResults.get(row);
            boolean listing = r.details().isEmpty();
            Color[] c = Theme.scoreColors(r.finalScore());
            Theme.Chip chip = Theme.chip(listing ? "-" : r.finalScore() + "점",
                    listing ? Theme.NEUTRAL_SOFT : c[0], listing ? Theme.MUTED : c[1]);
            JPanel wrap = new JPanel(new GridBagLayout());
            wrap.setBackground(cell.getBackground());
            wrap.add(chip);
            return wrap;
        }
    }

    // ── 상세 영역 ──────────────────────────────────────

    private JComponent buildDetailPanel() {
        JPanel detail = new ViewportWidthPanel();
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        detail.setOpaque(false);

        photoLabel.setPreferredSize(new Dimension(PHOTO, PHOTO));
        photoLabel.setVerticalAlignment(SwingConstants.TOP);
        nameLabel.setFont(Theme.TITLE);
        nameLabel.setForeground(Theme.TEXT);
        chipRow.setOpaque(false);
        ((FlowLayout) chipRow.getLayout()).setHgap(4);
        publicDetailLabel.setFont(Theme.SMALL);
        publicDetailLabel.setForeground(Theme.MUTED);
        reasonLabel.setFont(Theme.SMALL);
        reasonLabel.setForeground(Theme.MUTED);
        scoreRows.setOpaque(false);
        scoreRows.setLayout(new GridBagLayout());

        // 위: 사진(왼쪽) + 이름·배지·공개 정보(오른쪽)
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(left(nameLabel));
        info.add(Box.createVerticalStrut(6));
        info.add(left(chipRow));
        info.add(Box.createVerticalStrut(6));
        info.add(left(publicDetailLabel));
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        top.add(photoLabel, BorderLayout.WEST);
        top.add(info, BorderLayout.CENTER);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, PHOTO + 10));

        detail.add(left(top));
        detail.add(Box.createVerticalStrut(16));
        detail.add(left(Theme.label("점수 근거")));
        detail.add(Box.createVerticalStrut(6));
        detail.add(left(scoreRows));
        detail.add(Box.createVerticalStrut(6));
        detail.add(left(reasonLabel));
        // 내용이 길면 카드 안에서 스크롤 (창이 작아도 점수 근거가 잘리지 않게)
        JScrollPane detailScroll = new JScrollPane(detail,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        detailScroll.setBorder(BorderFactory.createEmptyBorder());
        detailScroll.setOpaque(false);
        detailScroll.getViewport().setOpaque(false);
        detailScroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel detailCard = Theme.card(null, detailScroll);

        // 승인 카드
        JPanel approval = new JPanel(new GridBagLayout());
        approval.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.insets = new Insets(0, 0, 8, 0);
        JLabel lock = new JLabel(SECRET_HIDDEN_TEXT);
        lock.setFont(Theme.SMALL);
        lock.setForeground(Theme.DANGER);
        g.gridy = 0; approval.add(lock, g);
        verifierNoteField.putClientProperty("JTextField.placeholderText", "소유자가 말한 특징 (예: 강아지 스티커)");
        g.gridy = 1; approval.add(verifierNoteField, g);
        approveBtn.addActionListener(e -> handleApprove());
        g.gridy = 2; g.insets = new Insets(4, 0, 0, 0); approval.add(approveBtn, g);
        JPanel approvalCard = Theme.card("관리자 회수 승인", approval);

        JPanel column = new JPanel(new BorderLayout(0, 16));
        column.setOpaque(false);
        column.setPreferredSize(new Dimension(400, 0));
        column.add(detailCard, BorderLayout.CENTER);
        column.add(approvalCard, BorderLayout.SOUTH);
        return column;
    }

    /** 스크롤 창의 폭에 맞춰 늘어나고 줄어드는 패널 (가로로 잘리지 않게). */
    private static final class ViewportWidthPanel extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(LEFT_ALIGNMENT);
        return c;
    }

    public void updateResults(List<MatchResult> results) {
        this.currentResults = results;
        tableModel.setRowCount(0);
        boolean listing = !results.isEmpty() && results.get(0).details().isEmpty();

        for (int i = 0; i < results.size(); i++) {
            MatchResult r = results.get(i);
            Item item = r.item();
            tableModel.addRow(new Object[]{
                    "",
                    listing ? String.valueOf(i + 1) : (i + 1) + "위",
                    item.getTitle(),
                    item.getBuilding().label() + " " + floorText(item.getFloor()),
                    r.finalScore(),
                    r.reason()
            });
        }

        summaryLabel.setText(results.isEmpty() ? "조건에 맞는 보관 중인 물건이 없습니다. 조건을 '모름'으로 바꿔 넓게 찾아 보세요."
                : listing ? "보관 중인 물건 " + results.size() + "건 (최근 습득 순)"
                : "가능성이 높은 순서로 " + results.size() + "건을 찾았어요. 행을 누르면 오른쪽에 자세한 근거가 보입니다.");

        approveBtn.setEnabled(!results.isEmpty());
        if (!results.isEmpty()) {
            resultTable.setRowSelectionInterval(0, 0);
        } else {
            clearDetail();
        }
    }

    private void clearDetail() {
        photoLabel.setIcon(null);
        photoLabel.setText("선택 없음");
        nameLabel.setText(" ");
        chipRow.removeAll();
        publicDetailLabel.setText(" ");
        scoreRows.removeAll();
        reasonLabel.setText(" ");
        revalidate();
        repaint();
    }


    private void refreshSelectionDetail() {
        int idx = resultTable.getSelectedRow();
        if (idx < 0 || idx >= currentResults.size()) return;

        MatchResult r = currentResults.get(idx);
        Item item = r.item();

        ImageIcon big = Images.scaled(item.getPhotoPath(), PHOTO, PHOTO);
        photoLabel.setText(null);
        photoLabel.setIcon(big != null ? big : Images.placeholder(item, PHOTO, PHOTO));

        nameLabel.setText(item.getTitle());
        chipRow.removeAll();
        chipRow.add(Theme.chip(item.getCategory().label(), Theme.PRIMARY_SOFT, Theme.PRIMARY_DARK));
        chipRow.add(Theme.chip(item.getColor().label(), Theme.NEUTRAL_SOFT, Theme.TEXT));
        chipRow.add(Theme.chip(item.getBuilding().label() + " " + floorText(item.getFloor()), Theme.NEUTRAL_SOFT, Theme.TEXT));
        publicDetailLabel.setText(html(esc(item.getFoundDate() + " 습득 · " + item.specificSummary())
                + (item.getPublicDetail().isBlank() ? "" : "<br>" + esc(item.getPublicDetail())), 170));

        scoreRows.removeAll();
        if (r.details().isEmpty()) {
            scoreRows.add(Theme.muted("검색 조건 없이 전체 목록을 보고 있어요."));
            reasonLabel.setText(" ");
        } else {
            GridBagConstraints g = new GridBagConstraints();
            g.insets = new Insets(3, 0, 3, 8);
            g.anchor = GridBagConstraints.WEST;
            int row = 0;
            for (ScoreDetail d : r.details()) {
                g.gridy = row++;
                g.gridx = 0; g.weightx = 0; g.fill = GridBagConstraints.NONE;
                JLabel name = new JLabel(d.ruleName());
                name.setFont(Theme.SMALL);
                name.setForeground(Theme.TEXT);
                name.setPreferredSize(new Dimension(56, 18));
                scoreRows.add(name, g);
                g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
                Theme.ScoreBar bar = new Theme.ScoreBar(d.earned(), d.max(), d.comparable());
                bar.setPreferredSize(new Dimension(120, 14));
                scoreRows.add(bar, g);
                g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
                JLabel val = new JLabel(d.comparable() ? d.earned() + " / " + d.max() : "모름");
                val.setFont(Theme.SMALL);
                val.setForeground(d.comparable() ? Theme.TEXT : Theme.MUTED);
                scoreRows.add(val, g);
            }
            String semantic = r.semanticScore() > 0 && r.semanticScore() != r.ruleScore()
                    ? " · AI 의미 점수 " + r.semanticScore() : "";
            reasonLabel.setText(html("최종 " + r.finalScore() + "점 (규칙 " + r.ruleScore() + "점" + semantic + ")<br>"
                    + esc(r.reason()), 290));
        }
        revalidate();
        repaint();
    }

    private void handleApprove() {
        int idx = resultTable.getSelectedRow();
        if (idx < 0 || idx >= currentResults.size()) {
            JOptionPane.showMessageDialog(this, "승인할 물품을 목록에서 선택해 주세요.", "안내", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (verifierNoteField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "소유자가 말한 비공개 특징을 먼저 입력해 주세요.", "소유자 확인 필요", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Item selected = currentResults.get(idx).item();
        String ownerStatement = verifierNoteField.getText().trim();
        service.markItemChecking(selected.getId());

        // 관리자만 보는 확인 창에서 등록된 비공개 특징과 소유자 진술을 나란히 비교한다.
        int ans = JOptionPane.showConfirmDialog(this,
                "[" + selected.getTitle() + "] 소유자 확인\n\n"
                        + "등록된 비공개 특징: " + selected.getSecret() + "\n"
                        + "소유자 진술: " + ownerStatement + "\n\n"
                        + "두 내용이 일치합니까?\n"
                        + "[예] 반환 완료 처리 (이후 검색 결과에서 제외)   [아니요] 보관 중으로 되돌림",
                "관리자 소유자 확인 및 회수 승인", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (ans == JOptionPane.YES_OPTION) {
            service.approveRecovery(selected.getId());
            verifierNoteField.setText("");
            JOptionPane.showMessageDialog(this, "회수 승인이 완료되었습니다.", "승인 완료", JOptionPane.INFORMATION_MESSAGE);
            onApprovalCompleted.run();
        } else {
            service.cancelChecking(selected.getId());
            JOptionPane.showMessageDialog(this, "소유자 확인이 되지 않아 보관 중 상태로 되돌렸습니다.", "승인 보류", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static String floorText(int floor) {
        return floor < 0 ? "지하 " + (-floor) + "층" : floor + "층";
    }

    private static String html(String inner, int width) {
        return "<html><div style='width:" + width + "px'>" + inner + "</div></html>";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
