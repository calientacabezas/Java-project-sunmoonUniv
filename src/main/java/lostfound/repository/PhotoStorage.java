package lostfound.repository;

import lostfound.exception.LostFoundException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

/**
 * 습득물 사진을 data/photos/ 아래에 "물품ID.확장자"로 복사해 보관한다.
 * 원본 파일은 건드리지 않는다. data/ 폴더는 .gitignore 대상이라 사진은 저장소에 올라가지 않는다.
 */
public class PhotoStorage {

    public static final Path DEFAULT_DIR = Path.of("data", "photos");
    public static final long MAX_BYTES = 10L * 1024 * 1024;   // 10MB
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "bmp");

    private final Path dir;

    public PhotoStorage() {
        this(DEFAULT_DIR);
    }

    public PhotoStorage(Path dir) {
        this.dir = dir;
    }

    /**
     * 사진 파일이 등록 가능한지 검사한다. 문제가 있으면 사용자에게 보여 줄 문장을 담은 예외를 던진다.
     * 아이폰 기본 형식(HEIC)은 Java가 읽지 못하므로 JPG로 보내 달라고 안내한다.
     */
    public static void validate(Path source) {
        if (source == null || !Files.isRegularFile(source)) {
            throw new LostFoundException("사진 파일을 찾을 수 없습니다.");
        }
        String ext = extension(source);
        if (ext.equals("heic") || ext.equals("heif")) {
            throw new LostFoundException("HEIC 사진은 지원하지 않습니다. 휴대폰에서 JPG로 보내거나 변환해 주세요.");
        }
        if (!ALLOWED.contains(ext)) {
            throw new LostFoundException("JPG, PNG, GIF, BMP 사진만 등록할 수 있습니다.");
        }
        try {
            if (Files.size(source) > MAX_BYTES) {
                throw new LostFoundException("사진은 10MB 이하만 등록할 수 있습니다.");
            }
            BufferedImage img = ImageIO.read(source.toFile());
            if (img == null) {
                throw new LostFoundException("이미지로 읽을 수 없는 파일입니다.");
            }
        } catch (IOException e) {
            throw new LostFoundException("사진을 읽는 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /** 검사 후 복사하고, 저장된 경로(문자열)를 돌려준다. 같은 ID의 사진이 있으면 덮어쓴다. */
    public String store(String itemId, Path source) {
        validate(source);
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(itemId + "." + extension(source));
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new LostFoundException("사진을 저장하지 못했습니다: " + e.getMessage(), e);
        }
    }

    static String extension(Path p) {
        String name = p.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
