package lostfound.ui;

import lostfound.domain.Category;
import lostfound.domain.Item;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** 사진 축소본 만들기와 사진이 없을 때의 대체 그림. */
final class Images {
    private Images() {}

    private static final Map<String, ImageIcon> CACHE = new HashMap<>();

    /** 비율을 유지한 채 w×h 안에 맞춘 축소본. 읽을 수 없으면 null. */
    static ImageIcon scaled(String path, int w, int h) {
        if (path == null) return null;
        String key = path + "@" + w + "x" + h + "@" + new File(path).lastModified();
        if (CACHE.containsKey(key)) return CACHE.get(key);
        ImageIcon icon = null;
        try {
            BufferedImage src = ImageIO.read(new File(path));
            if (src != null) icon = new ImageIcon(fit(src, w, h));
        } catch (Exception ignored) {
            // 파일이 지워졌거나 손상된 경우 대체 그림 사용
        }
        CACHE.put(key, icon);
        return icon;
    }

    static ImageIcon scaled(File file, int w, int h) {
        return scaled(file.getAbsolutePath(), w, h);
    }

    private static Image fit(BufferedImage src, int w, int h) {
        double ratio = Math.min(w / (double) src.getWidth(), h / (double) src.getHeight());
        int nw = Math.max(1, (int) Math.round(src.getWidth() * ratio));
        int nh = Math.max(1, (int) Math.round(src.getHeight() * ratio));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    /** 사진이 없을 때: 종류별 색 배경에 종류 이름 첫 글자를 그린 둥근 사각형. */
    static Icon placeholder(Item item, int w, int h) {
        return placeholder(item.getCategory(), w, h);
    }

    static Icon placeholder(Category category, int w, int h) {
        Color bg = switch (category) {
            case ELECTRONICS -> new Color(0xE0EAFF);
            case WALLET_CARD -> new Color(0xFDEBD3);
            case BAG_CLOTHING -> new Color(0xE3F4E8);
            default -> new Color(0xEEE8FB);
        };
        String text = category.label().substring(0, 1);
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(x, y, w, h, 12, 12);
                g2.setColor(Theme.MUTED);
                g2.setFont(Theme.font(Font.BOLD, Math.max(12, Math.min(w, h) / 3f)));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
            @Override public int getIconWidth() { return w; }
            @Override public int getIconHeight() { return h; }
        };
    }

    /** 물품 사진 축소본, 없으면 대체 그림. */
    static Icon thumbnail(Item item, int w, int h) {
        ImageIcon icon = scaled(item.getPhotoPath(), w, h);
        return icon != null ? icon : placeholder(item, w, h);
    }
}
