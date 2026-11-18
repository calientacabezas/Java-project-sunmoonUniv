package lostfound.ui;

import lostfound.domain.Building;
import lostfound.domain.Category;
import lostfound.domain.ItemColor;
import lostfound.matching.MatchResult;
import lostfound.matching.SearchQuery;
import lostfound.service.LostFoundService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Consumer;

/**
 * 2번 탭: 자연어 입력 → 개인정보 마스킹 & 조건 추출 → 사용자 확인 → 하이브리드 매칭 실행.
 */
public class SearchMatchPanel extends JPanel {

    private static final Integer[] FLOOR_OPTIONS = {0, 1, 2, 3, 4, 5, 6, 7, -1};

    private final LostFoundService service;
    private final Consumer<List<MatchResult>> onMatchCompleted;

    private final JTextArea naturalInputArea = new JTextArea(3, 42);
    private final JLabel maskedPreviewLabel = new JLabel("개인정보 보호: 외부 전송 전 전화번호·학번·카드번호·이름을 [MASKED]로 자동 치환합니다.");

    private final JComboBox<Category> categoryBox = new JComboBox<>(Category.values());
    private final JComboBox<ItemColor> colorBox = new JComboBox<>(ItemColor.values());
    private final JComboBox<Building> buildingBox = new JComboBox<>(Building.values());
    private final JComboBox<Integer> floorBox = new JComboBox<>(FLOOR_OPTIONS);
    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 12);
    private final JTextField detailField = new JTextField(22);

    private String lastMaskedRawText = "";

    public SearchMatchPanel(LostFoundService service, Consumer<List<MatchResult>> onMatchCompleted) {
        this.service = service;
        this.onMatchCompleted = onMatchCompleted;

        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(18, 24, 18, 24));

        configureRenderers();

        // 1단계: 자연어 입력 및 마스킹 확인 패널
        JPanel nlpPanel = new JPanel(new BorderLayout(8, 8));
        nlpPanel.setBorder(new TitledBorder("1단계: 자연어 분실 설명 입력 (LLM 조건 추출 및 개인정보 자동 마스킹)"));

        naturalInputArea.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        naturalInputArea.setLineWrap(true);
        naturalInputArea.setText("어제 공대 3층에서 하얀 버즈 잃어버렸어요 (연락처 010-1234-5678)");

        JButton parseBtn = new JButton("조건 자동 추출");
        parseBtn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        parseBtn.addActionListener(e -> handleParse());

        maskedPreviewLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        maskedPreviewLabel.setForeground(new Color(45, 85, 145));

        nlpPanel.add(new JScrollPane(naturalInputArea), BorderLayout.CENTER);
        nlpPanel.add(parseBtn, BorderLayout.EAST);
        nlpPanel.add(maskedPreviewLabel, BorderLayout.SOUTH);

        // 2단계: 추출 조건 사용자 확인 및 선택형 입력 패널
        JPanel confirmPanel = new JPanel(new GridBagLayout());
        confirmPanel.setBorder(new TitledBorder("2단계: 추출 조건 확인 및 수정 (오프라인 장애 시 직접 선택 가능)"));

        categoryBox.setSelectedItem(Category.UNKNOWN);
        colorBox.setSelectedItem(ItemColor.UNKNOWN);
        buildingBox.setSelectedItem(Building.UNKNOWN);
        floorBox.setSelectedItem(0);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 8, 7, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addPair(confirmPanel, gbc, 0, "물품 종류", categoryBox, "주요 색상", colorBox);
        addPair(confirmPanel, gbc, 1, "분실 건물", buildingBox, "분실 층수", floorBox);
        addPair(confirmPanel, gbc, 2, "분실 날짜", dateField, "세부 특징 키워드", detailField);

        // 3단계: 실행 버튼
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton allListBtn = new JButton("전체 미반환 목록 보기");
        JButton matchBtn = new JButton("유사도 매칭 실행");
        matchBtn.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        matchBtn.setBackground(new Color(25, 90, 180));
        matchBtn.setForeground(Color.WHITE);
        matchBtn.setFocusPainted(false);

        allListBtn.addActionListener(e -> {
            SearchQuery allQuery = new SearchQuery(
                    Category.UNKNOWN, ItemColor.UNKNOWN, null, Building.UNKNOWN, null, "", "");
            onMatchCompleted.accept(service.searchMatches(allQuery));
        });
        matchBtn.addActionListener(e -> handleSearch());

        actionPanel.add(allListBtn);
        actionPanel.add(matchBtn);

        add(nlpPanel, BorderLayout.NORTH);
        add(confirmPanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
    }

    private void configureRenderers() {
        categoryBox.setRenderer((list, val, idx, sel, focus) ->
                new DefaultListCellRenderer().getListCellRendererComponent(
                        list, val == null ? "" : val.label(), idx, sel, focus));
        colorBox.setRenderer((list, val, idx, sel, focus) ->
                new DefaultListCellRenderer().getListCellRendererComponent(
                        list, val == null ? "" : val.label(), idx, sel, focus));
        buildingBox.setRenderer((list, val, idx, sel, focus) ->
                new DefaultListCellRenderer().getListCellRendererComponent(
                        list, val == null ? "" : val.label(), idx, sel, focus));
        floorBox.setRenderer((list, val, idx, sel, focus) -> {
            String text = (val == null || val == 0) ? "모름" : (val == -1 ? "지하 1층" : val + "층");
            return new DefaultListCellRenderer().getListCellRendererComponent(list, text, idx, sel, focus);
        });
    }

    private void addPair(JPanel panel, GridBagConstraints gbc, int row,
                         String l1, JComponent c1, String l2, JComponent c2) {
        gbc.gridy = row;
        gbc.gridx = 0; gbc.weightx = 0.15;
        panel.add(new JLabel(l1), gbc);
        gbc.gridx = 1; gbc.weightx = 0.35;
        panel.add(c1, gbc);

        gbc.gridx = 2; gbc.weightx = 0.15;
        panel.add(new JLabel(l2), gbc);
        gbc.gridx = 3; gbc.weightx = 0.35;
        panel.add(c2, gbc);
    }

    private void handleParse() {
        String raw = naturalInputArea.getText().trim();
        if (raw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "분실 내용을 입력해 주세요.", "안내", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LostFoundService.ParseOutcome outcome = service.parseNaturalQuery(raw);
        SearchQuery q = outcome.query();
        lastMaskedRawText = outcome.maskedText();

        String engineTag = outcome.usedLlm()
                ? "[LLM API 추출 완료]"
                : "[오프라인 규칙 추출 - 사유: " + outcome.errorMessage() + "]";
        maskedPreviewLabel.setText(engineTag + " | 개인정보 " + outcome.maskedCount() + "건 마스킹");

        if (q.category() != null) categoryBox.setSelectedItem(q.category());
        if (q.color() != null) colorBox.setSelectedItem(q.color());
        if (q.building() != null) buildingBox.setSelectedItem(q.building());
        floorBox.setSelectedItem(q.floor() == null ? 0 : q.floor());
        if (q.lostDate() != null) dateField.setText(q.lostDate().toString());
        if (q.detailText() != null) detailField.setText(q.detailText());
    }

    private void handleSearch() {
        LocalDate lostDate = null;
        String dateStr = dateField.getText().trim();
        if (!dateStr.isEmpty()) {
            try {
                lostDate = LocalDate.parse(dateStr);
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this,
                        "날짜 형식을 YYYY-MM-DD로 입력하거나 비워두세요.",
                        "날짜 형식 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Integer selectedFloor = (Integer) floorBox.getSelectedItem();
        Integer floor = (selectedFloor == null || selectedFloor == 0) ? null : selectedFloor;

        SearchQuery query = new SearchQuery(
                (Category) categoryBox.getSelectedItem(),
                (ItemColor) colorBox.getSelectedItem(),
                lostDate,
                (Building) buildingBox.getSelectedItem(),
                floor,
                detailField.getText().trim(),
                lastMaskedRawText
        );

        List<MatchResult> results = service.searchMatches(query);
        onMatchCompleted.accept(results);
    }
}
