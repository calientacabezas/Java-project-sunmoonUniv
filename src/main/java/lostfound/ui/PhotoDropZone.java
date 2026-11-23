package lostfound.ui;

import lostfound.exception.LostFoundException;
import lostfound.repository.PhotoStorage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * 사진 첨부 영역: 클릭해서 파일을 고르거나, 탐색기에서 사진을 끌어다 놓으면 미리보기를 보여 준다. 담당: 강채연.
 * 휴대폰으로 찍은 사진을 PC로 옮긴 뒤 등록하는 흐름을 가정한다.
 */
class PhotoDropZone extends JPanel {

    private static final int PREVIEW_W = 260;
    private static final int PREVIEW_H = 200;

    private final JLabel preview = new JLabel("", SwingConstants.CENTER);
    private final JLabel hint = Theme.muted("");
    private final Theme.RoundButton removeBtn = Theme.ghostButton("사진 빼기");
    private File selected;
    private boolean dragging;

    PhotoDropZone() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));

        preview.setPreferredSize(new Dimension(PREVIEW_W, PREVIEW_H));
        preview.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        preview.setFont(Theme.BODY_BOLD);
        preview.setForeground(Theme.MUTED);
        preview.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { choose(); }
        });

        JPanel drop = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(dragging ? Theme.PRIMARY_SOFT : Theme.NEUTRAL_SOFT);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.ARC, Theme.ARC);
                g2.setColor(dragging ? Theme.PRIMARY : Theme.BORDER);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1f, new float[]{6f, 5f}, 0f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, Theme.ARC, Theme.ARC);
                g2.dispose();
            }
        };
        drop.setOpaque(false);
        drop.setBorder(new EmptyBorder(10, 10, 10, 10));
        drop.add(preview, BorderLayout.CENTER);
        drop.setTransferHandler(new FileDropHandler());
        preview.setTransferHandler(new FileDropHandler());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(hint, BorderLayout.CENTER);
        bottom.add(removeBtn, BorderLayout.EAST);
        removeBtn.addActionListener(e -> clear());

        add(drop, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        clear();
    }

    /** 고른 사진 파일 (없으면 null). */
    Path selectedPath() {
        return selected == null ? null : selected.toPath();
    }

    void clear() {
        selected = null;
        dragging = false;
        preview.setIcon(null);
        preview.setText("<html><div style='text-align:center;font-size:15px'>+<br>사진 추가</div></html>");
        hint.setText("클릭 또는 끌어다 놓기 · JPG/PNG 10MB 이하");
        removeBtn.setVisible(false);
    }

    private void choose() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("습득물 사진 선택");
        chooser.setFileFilter(new FileNameExtensionFilter("사진 (JPG, PNG, GIF, BMP)", "jpg", "jpeg", "png", "gif", "bmp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            accept(chooser.getSelectedFile());
        }
    }

    private void accept(File file) {
        try {
            PhotoStorage.validate(file.toPath());
        } catch (LostFoundException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "사진을 등록할 수 없습니다", JOptionPane.WARNING_MESSAGE);
            return;
        }
        selected = file;
        preview.setText("");
        preview.setIcon(Images.scaled(file, PREVIEW_W, PREVIEW_H));
        hint.setText(file.getName());
        removeBtn.setVisible(true);
    }

    private class FileDropHandler extends TransferHandler {
        @Override public boolean canImport(TransferSupport support) {
            boolean ok = support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            if (dragging != ok) { dragging = ok; repaint(); }
            return ok;
        }

        @Override @SuppressWarnings("unchecked")
        public boolean importData(TransferSupport support) {
            dragging = false;
            repaint();
            try {
                List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                if (!files.isEmpty()) accept(files.get(0));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
