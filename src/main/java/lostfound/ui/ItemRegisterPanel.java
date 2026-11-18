package lostfound.ui;

import lostfound.domain.*;
import lostfound.service.LostFoundService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * 1번 탭: 습득물 표준화 등록 화면 (Item 하위 클래스 4종 동적 생성).
 */
public class ItemRegisterPanel extends JPanel {

    private static final Category[] REGISTER_CATEGORIES = {
            Category.ELECTRONICS, Category.WALLET_CARD, Category.BAG_CLOTHING, Category.GENERAL
    };
    private static final Integer[] FLOORS = {1, 2, 3, 4, 5, -1};

    private final LostFoundService service;
    private final Runnable onItemRegistered;

    private final JTextField titleField = new JTextField(22);
    private final JComboBox<Category> categoryBox = new JComboBox<>(REGISTER_CATEGORIES);
    private final JComboBox<ItemColor> colorBox = new JComboBox<>(ItemColor.values());
    private final JComboBox<Building> buildingBox = new JComboBox<>(Building.values());
    private final JComboBox<Integer> floorBox = new JComboBox<>(FLOORS);
    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 12);
    private final JTextField publicDetailField = new JTextField(28);
    private final JTextField secretField = new JTextField(28);

    // 종류별 하위 클래스 전용 입력 필드 (CardLayout)
    private final CardLayout subtypeCardLayout = new CardLayout();
    private final JPanel subtypePanel = new JPanel(subtypeCardLayout);

    private final JTextField brandField = new JTextField(15);
    private final JTextField modelField = new JTextField(15);
    private final JTextField walletTypeField = new JTextField(15);
    private final JCheckBox idCardCheck = new JCheckBox("학생증·신분증 포함");
    private final JTextField bagSubTypeField = new JTextField(15);
    private final JTextField bagBrandField = new JTextField(15);
    private final JTextField generalSubField = new JTextField(20);

    public ItemRegisterPanel(LostFoundService service, Runnable onItemRegistered) {
        this.service = service;
        this.onItemRegistered = onItemRegistered;

        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(18, 24, 18, 24));

        JLabel header = new JLabel("습득물 표준화 등록 (종류별 세부 속성 자동 전환)");
        header.setFont(new Font("맑은 고딕", Font.BOLD, 17));
        add(header, BorderLayout.NORTH);

        configureEnumRenderers();
        buildSubtypeCards();

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow(formPanel, gbc, 0, "물품명 *", titleField);
        addRow(formPanel, gbc, 1, "물품 종류 *", categoryBox);
        addRow(formPanel, gbc, 2, "종류별 세부 속성", subtypePanel);
        addRow(formPanel, gbc, 3, "주요 색상 *", colorBox);
        addRow(formPanel, gbc, 4, "습득 건물 *", buildingBox);
        addRow(formPanel, gbc, 5, "습득 층수 *", floorBox);
        addRow(formPanel, gbc, 6, "습득 날짜 (YYYY-MM-DD) *", dateField);
        addRow(formPanel, gbc, 7, "공개 특징 (외관 설명)", publicDetailField);
        addRow(formPanel, gbc, 8, "비공개 특징 (소유자 검증용) *", secretField);

        categoryBox.addActionListener(e -> {
            Category selected = (Category) categoryBox.getSelectedItem();
            if (selected != null) {
                subtypeCardLayout.show(subtypePanel, selected.name());
            }
        });

        add(formPanel, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton resetBtn = new JButton("입력 초기화");
        JButton saveBtn = new JButton("습득물 등록");
        saveBtn.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        saveBtn.setBackground(new Color(33, 115, 70));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);

        resetBtn.addActionListener(e -> resetForm());
        saveBtn.addActionListener(e -> handleSave());

        bottomBar.add(resetBtn);
        bottomBar.add(saveBtn);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private void configureEnumRenderers() {
        categoryBox.setRenderer((list, val, idx, sel, focus) ->
                new DefaultListCellRenderer().getListCellRendererComponent(
                        list, val == null ? "" : val.label(), idx, sel, focus));
        colorBox.setRenderer((list, val, idx, sel, focus) ->
                new DefaultListCellRenderer().getListCellRendererComponent(
                        list, val == null ? "" : val.label() + " (" + val.group().name() + ")", idx, sel, focus));
        buildingBox.setRenderer((list, val, idx, sel, focus) ->
                new DefaultListCellRenderer().getListCellRendererComponent(
                        list, val == null ? "" : val.label(), idx, sel, focus));
        floorBox.setRenderer((list, val, idx, sel, focus) ->
                new DefaultListCellRenderer().getListCellRendererComponent(
                        list, val == null ? "" : (val == -1 ? "지하 1층" : val + "층"), idx, sel, focus));
    }

    private void buildSubtypeCards() {
        subtypePanel.setBorder(new TitledBorder("선택된 물품 종류 전용 필드"));

        // 1. 전자기기
        JPanel elecCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        elecCard.add(new JLabel("브랜드:"));
        elecCard.add(brandField);
        elecCard.add(new JLabel("모델명:"));
        elecCard.add(modelField);

        // 2. 지갑·카드
        JPanel walletCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        walletCard.add(new JLabel("지갑 형태:"));
        walletCard.add(walletTypeField);
        walletCard.add(idCardCheck);

        // 3. 가방·의류
        JPanel bagCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        bagCard.add(new JLabel("세부 종류:"));
        bagCard.add(bagSubTypeField);
        bagCard.add(new JLabel("브랜드·소재:"));
        bagCard.add(bagBrandField);

        // 4. 기타
        JPanel generalCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        generalCard.add(new JLabel("세부 품목명:"));
        generalCard.add(generalSubField);

        subtypePanel.add(elecCard, Category.ELECTRONICS.name());
        subtypePanel.add(walletCard, Category.WALLET_CARD.name());
        subtypePanel.add(bagCard, Category.BAG_CLOTHING.name());
        subtypePanel.add(generalCard, Category.GENERAL.name());
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        panel.add(label, gbc);

        gbc.gridx = 1; gbc.weightx = 0.75;
        panel.add(comp, gbc);
    }

    private void handleSave() {
        String title = titleField.getText().trim();
        String secret = secretField.getText().trim();
        if (title.isEmpty() || secret.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "물품명과 비공개 특징(소유자 검증용)은 필수입니다.",
                    "입력 확인", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate foundDate;
        try {
            foundDate = LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "날짜 형식을 YYYY-MM-DD (예: 2026-11-15)로 입력해 주세요.",
                    "날짜 형식 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Category category = (Category) categoryBox.getSelectedItem();
        ItemColor color = (ItemColor) colorBox.getSelectedItem();
        Building building = (Building) buildingBox.getSelectedItem();
        int floor = (Integer) floorBox.getSelectedItem();
        String publicDetail = publicDetailField.getText().trim();
        String id = service.nextItemId();

        Item newItem = switch (category) {
            case ELECTRONICS -> new ElectronicsItem(
                    id, color, foundDate, building, floor, title,
                    brandField.getText().trim(), modelField.getText().trim(), publicDetail, secret);
            case WALLET_CARD -> new WalletCardItem(
                    id, color, foundDate, building, floor, title,
                    walletTypeField.getText().trim(), idCardCheck.isSelected(), publicDetail, secret);
            case BAG_CLOTHING -> new BagClothingItem(
                    id, color, foundDate, building, floor, title,
                    bagSubTypeField.getText().trim(), bagBrandField.getText().trim(), publicDetail, secret);
            default -> new GeneralItem(
                    id, color, foundDate, building, floor, title,
                    generalSubField.getText().trim(), publicDetail, secret);
        };

        service.registerItem(newItem);
        JOptionPane.showMessageDialog(this,
                "[" + id + " / " + title + "] 등록이 완료되었습니다.",
                "등록 완료", JOptionPane.INFORMATION_MESSAGE);
        resetForm();
        onItemRegistered.run();
    }

    private void resetForm() {
        titleField.setText("");
        categoryBox.setSelectedIndex(0);
        colorBox.setSelectedIndex(0);
        buildingBox.setSelectedIndex(0);
        floorBox.setSelectedIndex(0);
        dateField.setText(LocalDate.now().toString());
        publicDetailField.setText("");
        secretField.setText("");
        brandField.setText("");
        modelField.setText("");
        walletTypeField.setText("");
        idCardCheck.setSelected(false);
        bagSubTypeField.setText("");
        bagBrandField.setText("");
        generalSubField.setText("");
    }
}
