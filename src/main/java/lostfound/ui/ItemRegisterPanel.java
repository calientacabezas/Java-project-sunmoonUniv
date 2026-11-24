package lostfound.ui;

import lostfound.domain.*;
import lostfound.exception.LostFoundException;
import lostfound.service.LostFoundService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

/**
 * 1번 탭: 습득물 등록 화면. 종류에 따라 Item 하위 클래스 4종 중 하나를 만들고, 사진을 첨부할 수 있다.
 */
public class ItemRegisterPanel extends JPanel {

    private static final Category[] REGISTER_CATEGORIES = {
            Category.ELECTRONICS, Category.WALLET_CARD, Category.BAG_CLOTHING, Category.GENERAL
    };
    private static final ItemColor[] REGISTER_COLORS = Arrays.stream(ItemColor.values())
            .filter(c -> c != ItemColor.UNKNOWN)
            .toArray(ItemColor[]::new);
    private static final Building[] REGISTER_BUILDINGS = Arrays.stream(Building.values())
            .filter(b -> b != Building.UNKNOWN)
            .toArray(Building[]::new);
    private static final Integer[] FLOORS = {1, 2, 3, 4, 5, 6, 7, -1};

    private final LostFoundService service;
    private final Runnable onItemRegistered;

    private final JTextField titleField = new JTextField(22);
    private final JComboBox<Category> categoryBox = new JComboBox<>(REGISTER_CATEGORIES);
    private final JComboBox<ItemColor> colorBox = new JComboBox<>(REGISTER_COLORS);
    private final JComboBox<Building> buildingBox = new JComboBox<>(REGISTER_BUILDINGS);
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

    private final PhotoDropZone photoZone = new PhotoDropZone();

    public ItemRegisterPanel(LostFoundService service, Runnable onItemRegistered) {
        this.service = service;
        this.onItemRegistered = onItemRegistered;

        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel heading = new JPanel(new BorderLayout(0, 4));
        heading.setOpaque(false);
        JLabel title = new JLabel("습득물 등록");
        title.setFont(Theme.HEADLINE);
        title.setForeground(Theme.TEXT);
        heading.add(title, BorderLayout.NORTH);
        heading.add(Theme.muted("주운 물건의 사진과 정보를 목록에서 골라 표준화된 형태로 등록합니다. * 표시는 필수 항목입니다."),
                BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);

        configureEnumRenderers();
        buildSubtypeCards();

        // 왼쪽: 사진 카드
        JPanel photoCard = Theme.card("사진", photoZone);
        photoCard.setPreferredSize(new Dimension(320, 0));

        // 오른쪽: 정보 입력 카드
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, gbc, 0, "물품명 *", titleField);
        addRow(form, gbc, 1, "물품 종류 *", categoryBox);
        addRow(form, gbc, 2, "세부 정보", subtypePanel);
        addPair(form, gbc, 3, "색상 *", colorBox, "습득 날짜 *", dateField);
        addPair(form, gbc, 4, "습득 건물 *", buildingBox, "층 *", floorBox);
        addRow(form, gbc, 5, "공개 특징", publicDetailField);
        addRow(form, gbc, 6, "비공개 특징 *", secretField);
        gbc.gridx = 1; gbc.gridy = 7; gbc.gridwidth = 3;
        form.add(Theme.muted("비공개 특징은 검색 결과에 보이지 않고, 관리자가 소유자를 확인할 때만 사용됩니다."), gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 8; gbc.weighty = 1; gbc.gridwidth = 4;
        form.add(Box.createGlue(), gbc);   // 남는 공간을 아래로 밀어 양식을 위쪽에 붙임
        gbc.weighty = 0; gbc.gridwidth = 1;

        titleField.putClientProperty("JTextField.placeholderText", "예: 갤럭시 버즈2 프로");
        publicDetailField.putClientProperty("JTextField.placeholderText", "예: 투명 하드케이스, 강의실 앞 책상 위");
        secretField.putClientProperty("JTextField.placeholderText", "예: 케이스 안쪽 강아지 스티커");

        categoryBox.addActionListener(e -> {
            Category selected = (Category) categoryBox.getSelectedItem();
            if (selected != null) {
                subtypeCardLayout.show(subtypePanel, selected.name());
            }
        });

        JPanel formCard = Theme.card("물품 정보", form);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.add(photoCard, BorderLayout.WEST);
        body.add(formCard, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setOpaque(false);
        Theme.RoundButton resetBtn = Theme.ghostButton("입력 초기화");
        Theme.RoundButton saveBtn = Theme.primaryButton("습득물 등록");
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
                        list, val == null ? "" : val.label(), idx, sel, focus));
        buildingBox.setRenderer((list, val, idx, sel, focus) ->
                new DefaultListCellRenderer().getListCellRendererComponent(
                        list, val == null ? "" : val.label(), idx, sel, focus));
        floorBox.setRenderer((list, val, idx, sel, focus) ->
                new DefaultListCellRenderer().getListCellRendererComponent(
                        list, val == null ? "" : (val == -1 ? "지하 1층" : val + "층"), idx, sel, focus));
    }

    private void buildSubtypeCards() {
        subtypePanel.setOpaque(false);
        subtypePanel.add(fieldRow("브랜드", brandField, "모델명", modelField), Category.ELECTRONICS.name());
        JPanel walletCard = fieldRow("지갑 형태", walletTypeField, null, null);
        idCardCheck.setOpaque(false);
        walletCard.add(idCardCheck);
        subtypePanel.add(walletCard, Category.WALLET_CARD.name());
        subtypePanel.add(fieldRow("세부 종류", bagSubTypeField, "브랜드·소재", bagBrandField), Category.BAG_CLOTHING.name());
        subtypePanel.add(fieldRow("품목", generalSubField, null, null), Category.GENERAL.name());
    }

    private static JPanel fieldRow(String l1, JComponent c1, String l2, JComponent c2) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.add(Theme.muted(l1));
        p.add(c1);
        if (l2 != null) {
            p.add(Theme.muted(l2));
            p.add(c2);
        }
        return p;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        panel.add(Theme.label(labelText), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 3;
        panel.add(comp, gbc);
        gbc.gridwidth = 1;
    }

    private void addPair(JPanel panel, GridBagConstraints gbc, int row,
                         String l1, JComponent c1, String l2, JComponent c2) {
        gbc.gridy = row; gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.weightx = 0;
        panel.add(Theme.label(l1), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        panel.add(c1, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(Theme.label(l2), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5;
        panel.add(c2, gbc);
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
                    "날짜 형식을 YYYY-MM-DD (예: 2026-11-23)로 입력해 주세요.",
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

        try {
            service.registerItem(newItem, photoZone.selectedPath());
        } catch (LostFoundException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "등록 실패", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this,
                "[" + id + " / " + title + "] 등록이 완료되었습니다."
                        + (newItem.getPhotoPath() != null ? "\n사진도 함께 저장되었습니다." : ""),
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
        photoZone.clear();
    }
}