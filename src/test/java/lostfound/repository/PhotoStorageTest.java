package lostfound.repository;

import lostfound.exception.LostFoundException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PhotoStorageTest {

    private static Path tempImage(Path dir, String name) throws IOException {
        Path p = dir.resolve(name);
        ImageIO.write(new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB), "png", p.toFile());
        return p;
    }

    @Test void 사진을_물품ID_이름으로_복사하고_원본은_그대로_둔다() throws IOException {
        Path work = Files.createTempDirectory("photo-test");
        Path source = tempImage(work, "my-photo.png");
        PhotoStorage storage = new PhotoStorage(work.resolve("photos"));

        String stored = storage.store("ITEM-101", source);

        assertTrue(stored.endsWith("ITEM-101.png"));
        assertTrue(Files.exists(Path.of(stored)));
        assertTrue(Files.exists(source));
    }

    @Test void HEIC_사진은_JPG로_보내라고_안내한다() throws IOException {
        Path work = Files.createTempDirectory("photo-test");
        Path heic = Files.writeString(work.resolve("IMG_0001.HEIC"), "x");
        LostFoundException e = assertThrows(LostFoundException.class, () -> PhotoStorage.validate(heic));
        assertTrue(e.getMessage().contains("JPG"));
    }

    @Test void 이미지가_아닌_파일과_없는_파일은_거부한다() throws IOException {
        Path work = Files.createTempDirectory("photo-test");
        Path fake = Files.writeString(work.resolve("fake.jpg"), "이미지가 아님");
        assertThrows(LostFoundException.class, () -> PhotoStorage.validate(fake));
        assertThrows(LostFoundException.class, () -> PhotoStorage.validate(work.resolve("missing.png")));
        Path txt = Files.writeString(work.resolve("note.txt"), "x");
        assertThrows(LostFoundException.class, () -> PhotoStorage.validate(txt));
    }

    @Test void 샘플_파일의_사진_열은_선택_항목이다() {
        var items = SampleDataLoader.parse(List.of(
                "P-1|GENERAL|GRAY|2026-11-22|MAIN_HALL|2|우산|우산||자동|이름표",
                "P-2|GENERAL|GRAY|2026-11-22|MAIN_HALL|2|우산|우산||자동|이름표|data/photos/P-2.jpg"));
        assertNull(items.get(0).getPhotoPath());
        assertEquals("data/photos/P-2.jpg", items.get(1).getPhotoPath());
    }
}
