package lostfound.repository;

import lostfound.domain.BagClothingItem;
import lostfound.domain.Building;
import lostfound.domain.ElectronicsItem;
import lostfound.domain.GeneralItem;
import lostfound.domain.Item;
import lostfound.domain.ItemColor;
import lostfound.domain.ItemStatus;
import lostfound.domain.WalletCardItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 계획서의 "재실행해도 등록된 분실물 정보가 날아가지 않도록" 요구를 검증한다. */
class TextFileItemRepositoryTest {

    private static final LocalDate D = LocalDate.of(2026, 11, 22);

    private static List<Item> fourKinds() {
        return List.of(
                new ElectronicsItem("A-1", ItemColor.WHITE, D, Building.ENGINEERING, 3, "갤럭시 버즈", "삼성", "버즈2", "투명 케이스", "강아지 스티커"),
                new WalletCardItem("A-2", ItemColor.BLACK, D, Building.LIBRARY, 2, "반지갑", "반지갑", true, "가죽", "학생증"),
                new BagClothingItem("A-3", ItemColor.NAVY, D, Building.STUDENT_HALL, 1, "후드집업", "후드집업", "면", "휴게실", "립밤"),
                new GeneralItem("A-4", ItemColor.GRAY, D, Building.MAIN_HALL, -1, "우산", "우산", "자동", "이름표"));
    }

    @Test void 파일이_없으면_샘플로_채우고_파일을_만든다() throws IOException {
        Path file = Files.createTempDirectory("repo").resolve("items.txt");
        TextFileItemRepository repo = new TextFileItemRepository(file, fourKinds());
        assertTrue(Files.exists(file));
        assertEquals(4, repo.findAll().size());
    }

    @Test void 다시_열어도_네_종류의_고유_속성이_그대로_남는다() throws IOException {
        Path file = Files.createTempDirectory("repo").resolve("items.txt");
        new TextFileItemRepository(file, fourKinds());

        TextFileItemRepository reopened = new TextFileItemRepository(file, List.of());
        ElectronicsItem e = (ElectronicsItem) reopened.findById("A-1").orElseThrow();
        WalletCardItem w = (WalletCardItem) reopened.findById("A-2").orElseThrow();
        BagClothingItem b = (BagClothingItem) reopened.findById("A-3").orElseThrow();
        GeneralItem g = (GeneralItem) reopened.findById("A-4").orElseThrow();

        assertEquals("삼성", e.getBrand());
        assertEquals("버즈2", e.getModelName());
        assertEquals("강아지 스티커", e.getSecret());
        assertTrue(w.isIncludesIdCard());
        assertEquals("면", b.getBrandOrMaterial());
        assertEquals(-1, g.getFloor());
        assertEquals(D, g.getFoundDate());
    }

    @Test void 새로_등록한_물건과_반환_상태가_재시작_후에도_유지된다() throws IOException {
        Path file = Files.createTempDirectory("repo").resolve("items.txt");
        TextFileItemRepository repo = new TextFileItemRepository(file, fourKinds());
        GeneralItem added = new GeneralItem("ITEM-101", ItemColor.BLUE, D, Building.LIBRARY, 3, "필통", "필통", "지퍼", "샤프");
        added.setPhotoPath("data/photos/ITEM-101.jpg");
        repo.save(added);
        Item returned = repo.findById("A-2").orElseThrow();
        returned.setStatus(ItemStatus.RETURNED);
        repo.save(returned);

        TextFileItemRepository reopened = new TextFileItemRepository(file, List.of());
        assertEquals(5, reopened.findAll().size());
        assertEquals("data/photos/ITEM-101.jpg", reopened.findById("ITEM-101").orElseThrow().getPhotoPath());
        assertEquals(ItemStatus.RETURNED, reopened.findById("A-2").orElseThrow().getStatus());
        assertEquals(ItemStatus.STORED, reopened.findById("A-1").orElseThrow().getStatus());
    }

    @Test void 값에_구분자나_줄바꿈이_있어도_파일_형식이_깨지지_않는다() throws IOException {
        Path file = Files.createTempDirectory("repo").resolve("items.txt");
        TextFileItemRepository repo = new TextFileItemRepository(file, List.of());
        repo.save(new GeneralItem("B-1", ItemColor.GRAY, D, Building.MAIN_HALL, 1, "우산|장우산", "우산", "첫 줄\n둘째 줄", "비밀|특징"));

        Item read = new TextFileItemRepository(file, List.of()).findById("B-1").orElseThrow();
        assertEquals("우산/장우산", read.getTitle());
        assertEquals("비밀/특징", read.getSecret());
        assertEquals(1L, Files.readAllLines(file).stream().filter(l -> l.startsWith("B-1")).count());
    }

    @Test void 깨진_줄은_건너뛰고_나머지는_읽는다() throws IOException {
        Path file = Files.createTempDirectory("repo").resolve("items.txt");
        Files.writeString(file, String.join("\n",
                "# 주석",
                "C-1|GENERAL|GRAY|2026-11-22|MAIN_HALL|2|우산|우산||자동|이름표||STORED",
                "C-2|GENERAL|없는색|2026-11-22|MAIN_HALL|2|우산|우산||자동|이름표||STORED",
                "엉망인 줄"), StandardCharsets.UTF_8);
        TextFileItemRepository repo = new TextFileItemRepository(file, List.of());
        assertEquals(1, repo.findAll().size());
        assertTrue(repo.findById("C-1").isPresent());
    }

    @Test void 저장_후_임시_파일이_남지_않는다() throws IOException {
        Path dir = Files.createTempDirectory("repo");
        new TextFileItemRepository(dir.resolve("items.txt"), fourKinds());
        assertFalse(Files.exists(dir.resolve("items.txt.tmp")));
    }

    @Test void 샘플_파일_형식_11개_항목도_읽는다() {
        Item item = ItemCodec.fromLine("S-1|GENERAL|GRAY|2026-11-22|MAIN_HALL|2|우산|우산||자동|이름표");
        assertEquals(ItemStatus.STORED, item.getStatus());
        assertNull(item.getPhotoPath());
    }
}
