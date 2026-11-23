package lostfound.ui;

import lostfound.domain.Building;
import lostfound.domain.Category;
import lostfound.domain.ItemColor;
import lostfound.matching.MatchResult;
import lostfound.matching.SearchQuery;
import lostfound.service.LostFoundService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Consumer;

/**
 * 2번 탭: 자연어 입력 → 개인정보 마스킹 & 조건 추출 → 사용자 확인 → 하이브리드 매칭 실행.
 * LLM 호출은 SwingWorker로 화면 스레드 밖에서 실행한다.
 */
public class SearchMatchPanel extends JPanel {

    private static final Integer[] FLOOR_OPTIONS = {0, 1, 2, 3, 4, 5, 6, 7, -1};

    private final LostFoundService service;
    private final Consumer<List<MatchResult>> onMatchCompleted;

    private final JTextArea naturalInputArea = new JTextArea(4, 42);
    private final JLabel maskedPreviewLabel = new JLabel("개인정보 보호: 전화번호·학번·카드번호·이메일은 AI에 보내기 전에 [MASKED]로 가립니다.");

    private final JComboBox<Category> categoryBox = new JComboBox<>(Category.values());
    private final JComboBox<ItemColor> colorBox = new JComboBox<>(ItemColor.values());
    private final JComboBox<Building> buildingBox = new JComboBox<>(Building.values());
    private final JComboBox<Integer> floorBox = new JComboBox<>(FLOOR_OPTIONS);
    private final JTextField dateField = new JTextField(12);
    private final JTextField detailField = new JTextField(22);

    private final JButton parseBtn = Theme.primaryButton("AI로 조건 찾기");
    private final JButton allListBtn = Theme.ghostButton("보관 중인 물건 전체 보기");
    private final JButton matchBtn = Theme.primaryButton("이 조건으로 찾기");

    /** 시연·안내용 예시 문장 (누르면 입력창에 채워짐). */
    private static final String[] EXAMPLES = {
            "어제 공대 3층에서 하얀 버즈 잃어버렸어요 (연락처 010-1234-5678)",
            "학관 1층 휴게실에 곤색 후드티 두고 왔어요",
            "I lost a black wallet with my student ID at the library, 2nd floor",
    };

    private String lastMaskedRawText = "";

    public SearchMatchPanel(LostFoundService service, Consumer<List<MatchResult>> onMatchCompleted) {
        this.service = service;
        this.onMatchCompleted = onMatchCompleted;

        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        configureRenderers();

        JPanel heading = new JPanel(new BorderLayout(0, 4));
        heading.setOpaque(false);
        JLabel title = new JLabel("무엇을 잃어버리셨나요?");
        title.setFont(Theme.HEADLINE);
        title.setForeground(Theme.TEXT);
        heading.add(title, BorderLayout.NORTH);
        heading.add(Theme.muted("평소 말하듯 적으면 AI가 종류·색상·장소를 찾아 줍니다. 한국어·영어 모두 가능해요."), BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        // 1단계: 자연어 입력 카드
        naturalInputArea.setFont(Theme.font(Font.PLAIN, 14));
        naturalInputArea.setLineWrap(true);
        naturalInputArea.setWrapStyleWord(true);
        naturalInputArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        naturalInputArea.setText(EXAMPLES[0]);
        JScrollPane inputScroll = new JScrollPane(naturalInputArea);
        inputScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        parseBtn.addActionListener(e -> handleParse());

        JPanel examples = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        examples.setOpaque(false);
        examples.add(Theme.muted("예시"));
        for (String ex : EXAMPLES) {
            Theme.Chip chip = Theme.chip(shorten(ex), Theme.NEUTRAL_SOFT, Theme.TEXT);
            chip.setFont(Theme.SMALL);
            chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chip.setToolTipText(ex);
            chip.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { naturalInputArea.setText(ex); }
            });
            examples.add(chip);
        }

        maskedPreviewLabel.setFont(Theme.SMALL);
        maskedPreviewLabel.setForeground(Theme.PRIMARY_DARK);

        JPanel inputRow = new JPanel(new BorderLayout(12, 0));
        inputRow.setOpaque(false);
        inputRow.add(inputScroll, BorderLayout.CENTER);
        JPanel parseWrap = new JPanel(new BorderLayout());
        parseWrap.setOpaque(false);
        parseWrap.add(parseBtn, BorderLayout.NORTH);
        inputRow.add(parseWrap, BorderLayout.EAST);

        JPanel inputBody = new JPanel(new BorderLayout(0, 10));
        inputBody.add(inputRow, BorderLayout.CENTER);
        JPanel under = new JPanel(new GridLayout(2, 1, 0, 6));
        under.setOpaque(false);
        under.add(examples);
        under.add(maskedPreviewLabel);
        inputBody.add(under, BorderLayout.SOUTH);
        JPanel inputCard = Theme.card("1  설명 입력", inputBody);

        // 2단계: 추출 조건 확인 카드
        categoryBox.setSelectedItem(Category.UNKNOWN);
        colorBox.setSelectedItem(ItemColor.UNKNOWN);
        buildingBox.setSelectedItem(Building.UNKNOWN);
        floorBox.setSelectedItem(0);
        dateField.putClientProperty("JTextField.placeholderText", "모르면 비워 두세요 (YYYY-MM-DD)");

        JPanel confirmGrid = new JPanel(new GridBagLayout());
        confirmGrid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        addPair(confirmGrid, gbc, 0, "물품 종류", categoryBox, "색상", colorBox);
        addPair(confirmGrid, gbc, 1, "건물", buildingBox, "층", floorBox);
        addPair(confirmGrid, gbc, 2, "날짜 (선택)", dateField, "세부 특징", detailField);

        JPanel confirmBody = new JPanel(new BorderLayout(0, 8));
        confirmBody.add(Theme.muted("AI가 이렇게 이해했어요. 틀린 부분이 있으면 직접 고친 뒤 찾기를 누르세요. 모르는 항목은 '모름'으로 두면 점수 계산에서 빠집니다."),
                BorderLayout.NORTH);
        confirmBody.add(confirmGrid, BorderLayout.CENTER);
        JPanel confirmCard = Theme.card("2  조건 확인", confirmBody);

        JPanel cards = new JPanel(new BorderLayout(0, 16));
        cards.setOpaque(false);
        cards.add(inputCard, BorderLayout.NORTH);
        cards.add(confirmCard, BorderLayout.CENTER);
        add(cards, BorderLayout.CENTER);

        // 실행 버튼
        allListBtn.addActionListener(e -> onMatchCompleted.accept(service.listAllMatchable()));
        matchBtn.addActionListener(e -> handleSearch());
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);
        actionPanel.add(allListBtn);
        actionPanel.add(matchBtn);
        add(actionPanel, BorderLayout.SOUTH);
    }

    private static String shorten(String text) {
        return text.length() > 22 ? text.substring(0, 21) + "…" : text;
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
        gbc.gridx = 0; gbc.weightx = 0;
        panel.add(Theme.label(l1), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        panel.add(c1, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(Theme.label(l2), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5;
        panel.add(c2, gbc);
    }

    private void handleParse() {
        String raw = naturalInputArea.getText().trim();
        if (raw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "분실 내용을 입력해 주세요.", "안내", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // LLM 호출은 최대 수십 초 걸릴 수 있으므로 화면 스레드(EDT)가 아닌 백그라운드에서 실행한다.
        setBusy(true, "조건을 분석하는 중입니다...");
        new SwingWorker<LostFoundService.ParseOutcome, Void>() {
            @Override
            protected LostFoundService.ParseOutcome doInBackground() {
                return service.parseNaturalQuery(raw);
            }

            @Override
            protected void done() {
                try {
                    applyParseOutcome(get());
                } catch (Exception ex) {
                    maskedPreviewLabel.setText("[조건 추출 실패] 아래 항목을 직접 선택해 주세요. (" + ex.getMessage() + ")");
                } finally {
                    setBusy(false, null);
                }
            }
        }.execute();
    }

    private void applyParseOutcome(LostFoundService.ParseOutcome outcome) {
        SearchQuery q = outcome.query();
        lastMaskedRawText = outcome.maskedText();

        String engineTag;
        if (outcome.usedLlm()) {
            engineTag = "AI가 조건을 찾았어요";
        } else if (service.isOnlineMode()) {
            engineTag = "AI 연결에 실패해 규칙으로 조건을 찾았어요 (" + outcome.errorMessage() + ")";
        } else {
            engineTag = "오프라인 모드라 규칙으로 조건을 찾았어요";
        }
        String privacy = outcome.maskedCount() > 0
                ? " · 개인정보 " + outcome.maskedCount() + "건은 가리고 보냈어요" : "";
        maskedPreviewLabel.setText(engineTag + privacy + ". 아래 조건을 확인해 주세요.");

        if (q.category() != null) categoryBox.setSelectedItem(q.category());
        if (q.color() != null) colorBox.setSelectedItem(q.color());
        if (q.building() != null) buildingBox.setSelectedItem(q.building());
        floorBox.setSelectedItem(q.floor() == null ? 0 : q.floor());
        dateField.setText(q.lostDate() != null ? q.lostDate().toString() : "");
        if (q.detailText() != null) detailField.setText(q.detailText());
    }

    /** 처리 중에는 버튼을 막아 중복 호출을 방지하고, 안내 문구를 보여 준다. */
    private void setBusy(boolean busy, String message) {
        parseBtn.setEnabled(!busy);
        matchBtn.setEnabled(!busy);
        allListBtn.setEnabled(!busy);
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
        if (busy && message != null) {
            maskedPreviewLabel.setText(message);
        }
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

        String safeRawText = lastMaskedRawText.isEmpty()
                ? service.maskText(naturalInputArea.getText().trim())
                : lastMaskedRawText;

        SearchQuery query = new SearchQuery(
                (Category) categoryBox.getSelectedItem(),
                (ItemColor) colorBox.getSelectedItem(),
                lostDate,
                (Building) buildingBox.getSelectedItem(),
                floor,
                detailField.getText().trim(),
                safeRawText
        );

        setBusy(true, "유사도를 계산하는 중입니다... (온라인 모드에서는 LLM 의미 보정 포함)");
        new SwingWorker<List<MatchResult>, Void>() {
            @Override
            protected List<MatchResult> doInBackground() {
                return service.searchMatches(query);
            }

            @Override
            protected void done() {
                try {
                    onMatchCompleted.accept(get());
                    maskedPreviewLabel.setText("매칭 완료: 3번 탭에서 결과를 확인하세요.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SearchMatchPanel.this,
                            "매칭 중 오류가 발생했습니다: " + ex.getMessage(),
                            "오류", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setBusy(false, null);
                }
            }
        }.execute();
    }
}