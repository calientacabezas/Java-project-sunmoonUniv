package lostfound.service;

import lostfound.domain.Building;
import lostfound.domain.Category;
import lostfound.domain.Item;
import lostfound.domain.ItemColor;
import lostfound.domain.ItemStatus;
import lostfound.llm.FakeLlmClient;
import lostfound.matching.MatchResult;
import lostfound.parsing.KeywordQueryParser;
import lostfound.repository.InMemoryItemRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static lostfound.TestItems.*;
import static org.junit.jupiter.api.Assertions.*;

class LostFoundServiceTest {

    private static LostFoundService serviceWith(Item... items) {
        return new LostFoundService(new InMemoryItemRepository(List.of(items)), new FakeLlmClient(),
                null, new KeywordQueryParser(), null, LostFoundService.defaultScoringRules());
    }

    @Test void 조건이_가장_잘_맞는_물품이_1위다() {
        LostFoundService service = serviceWith(
                umbrella("U", ItemColor.GRAY, Building.MAIN_HALL, 2),
                wallet("W", ItemColor.BLACK, Building.LIBRARY, 2),
                buds("B", ItemColor.WHITE, Building.ENGINEERING, 3, LocalDate.of(2026, 11, 22)));
        List<MatchResult> results = service.searchRuleOnly(
                query(Category.ELECTRONICS, ItemColor.WHITE, Building.ENGINEERING, 3, "버즈"));
        assertEquals("B", results.get(0).item().getId());
        assertEquals(100, results.get(0).finalScore());
    }

    @Test void 동점이면_최근에_습득한_물품이_먼저다() {
        LostFoundService service = serviceWith(
                buds("OLD", ItemColor.WHITE, Building.ENGINEERING, 3, LocalDate.of(2026, 11, 18)),
                buds("NEW", ItemColor.WHITE, Building.ENGINEERING, 3, LocalDate.of(2026, 11, 23)));
        List<MatchResult> results = service.searchRuleOnly(
                query(Category.ELECTRONICS, ItemColor.WHITE, Building.ENGINEERING, 3, ""));
        assertEquals(results.get(0).finalScore(), results.get(1).finalScore());
        assertEquals("NEW", results.get(0).item().getId());
    }

    @Test void 반환_완료_물품은_검색과_전체_목록에서_빠진다() {
        Item returned = wallet("R", ItemColor.BLACK, Building.LIBRARY, 2);
        LostFoundService service = serviceWith(returned, wallet("S", ItemColor.BLACK, Building.LIBRARY, 2));
        service.approveRecovery("R");

        assertEquals(ItemStatus.RETURNED, returned.getStatus());
        assertTrue(service.searchRuleOnly(query(Category.WALLET_CARD, null, null, null, ""))
                .stream().noneMatch(r -> r.item().getId().equals("R")));
        assertEquals(1, service.listAllMatchable().size());
    }

    @Test void 소유자_확인을_취소하면_다시_보관_중이_된다() {
        Item item = wallet("C", ItemColor.BLACK, Building.LIBRARY, 2);
        LostFoundService service = serviceWith(item);
        service.markItemChecking("C");
        assertEquals(ItemStatus.CHECKING, item.getStatus());
        service.cancelChecking("C");
        assertEquals(ItemStatus.STORED, item.getStatus());
    }

    @Test void 결과는_최대_10개까지만_돌려준다() {
        Item[] many = new Item[15];
        for (int i = 0; i < many.length; i++) many[i] = umbrella("U" + i, ItemColor.GRAY, Building.MAIN_HALL, 2);
        LostFoundService service = serviceWith(many);
        assertEquals(LostFoundService.TOP_K, service.searchRuleOnly(query(Category.GENERAL, null, null, null, "")).size());
        assertEquals(15, service.listAllMatchable().size(), "전체 목록은 개수 제한 없이 보여 줌");
    }

    @Test void 오프라인이면_LLM_대신_대체_파서를_쓰고_개인정보를_가린다() {
        LostFoundService service = serviceWith(buds("B", ItemColor.WHITE, Building.ENGINEERING, 3, LocalDate.of(2026, 11, 22)));
        LostFoundService.ParseOutcome outcome = service.parseNaturalQuery("공학관 흰색 이어폰, 연락처 010-1234-5678");
        assertFalse(outcome.usedLlm());
        assertEquals(1, outcome.maskedCount());
        assertEquals(Building.ENGINEERING, outcome.query().building());
        assertFalse(outcome.maskedText().contains("010-1234-5678"));
    }

    @Test void 사진이_잘못되면_등록하지_않는다() throws Exception {
        LostFoundService service = serviceWith();
        java.nio.file.Path bad = java.nio.file.Files.createTempFile("bad", ".jpg");
        Item item = wallet("NEW", ItemColor.BLACK, Building.LIBRARY, 2);
        assertThrows(lostfound.exception.LostFoundException.class, () -> service.registerItem(item, bad));
        assertTrue(service.listAllMatchable().isEmpty());
    }

    @Test void 사진_없이도_등록할_수_있다() {
        LostFoundService service = serviceWith();
        service.registerItem(wallet("NEW", ItemColor.BLACK, Building.LIBRARY, 2), null);
        assertEquals(1, service.listAllMatchable().size());
        assertNull(service.listAllMatchable().get(0).item().getPhotoPath());
    }
}
