package lostfound.repository;

import lostfound.domain.BagClothingItem;
import lostfound.domain.Building;
import lostfound.domain.ElectronicsItem;
import lostfound.domain.GeneralItem;
import lostfound.domain.Item;
import lostfound.domain.ItemColor;
import lostfound.domain.WalletCardItem;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SampleDataLoaderTest {

    @Test void 종류별로_알맞은_하위_클래스를_만든다() {
        List<Item> items = SampleDataLoader.parse(List.of(
                "# 주석은 건너뜀",
                "",
                "A-1|ELECTRONICS|WHITE|2026-11-22|ENGINEERING|3|버즈|삼성|버즈2|케이스|스티커",
                "A-2|WALLET_CARD|BLACK|2026-11-22|LIBRARY|2|반지갑|반지갑|Y|가죽|학생증",
                "A-3|BAG_CLOTHING|NAVY|2026-11-22|STUDENT_HALL|1|후드|후드집업|면|휴게실|립밤",
                "A-4|GENERAL|GRAY|2026-11-22|MAIN_HALL|-1|우산|우산||자동|이름표"));
        assertEquals(4, items.size());
        assertInstanceOf(ElectronicsItem.class, items.get(0));
        assertInstanceOf(WalletCardItem.class, items.get(1));
        assertTrue(((WalletCardItem) items.get(1)).isIncludesIdCard());
        assertInstanceOf(BagClothingItem.class, items.get(2));
        assertInstanceOf(GeneralItem.class, items.get(3));
        assertEquals(-1, items.get(3).getFloor());
        assertEquals(Building.ENGINEERING, items.get(0).getBuilding());
        assertEquals(ItemColor.WHITE, items.get(0).getColor());
    }

    @Test void 형식이_틀린_줄은_건너뛰고_나머지는_읽는다() {
        List<Item> items = SampleDataLoader.parse(List.of(
                "B-1|ELECTRONICS|WHITE|2026-11-22|ENGINEERING|3|버즈|삼성|버즈2|케이스|스티커",
                "B-2|ELECTRONICS|없는색|2026-11-22|ENGINEERING|3|버즈|삼성|버즈2|케이스|스티커",
                "B-3|항목이|모자람",
                "B-4|SPACESHIP|WHITE|2026-11-22|ENGINEERING|3|x|x|x|x|x"));
        assertEquals(1, items.size());
        assertEquals("B-1", items.get(0).getId());
    }

    @Test void 없는_파일이면_빈_목록을_돌려준다() {
        assertTrue(SampleDataLoader.loadOrEmpty(Path.of("no-such-dir", "missing.txt")).isEmpty());
    }

    @Test void 저장소의_실제_샘플_파일은_30건이고_ID가_겹치지_않는다() {
        List<Item> items = SampleDataLoader.loadOrEmpty(SampleDataLoader.DEFAULT_PATH);
        assertEquals(30, items.size());
        Set<String> ids = new HashSet<>();
        for (Item item : items) assertTrue(ids.add(item.getId()), "중복 ID: " + item.getId());
    }
}
