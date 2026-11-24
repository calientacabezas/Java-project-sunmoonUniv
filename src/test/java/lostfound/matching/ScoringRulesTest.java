package lostfound.matching;

import lostfound.domain.Building;
import lostfound.domain.Category;
import lostfound.domain.Item;
import lostfound.domain.ItemColor;
import lostfound.service.LostFoundService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static lostfound.TestItems.buds;
import static lostfound.TestItems.query;
import static org.junit.jupiter.api.Assertions.*;

/** 기본 점수 규칙 5개(종류 40 / 색상 25 / 건물 20 / 층 5 / 세부 10) 검증. */
class ScoringRulesTest {

    private static final List<ScoringRule> RULES = LostFoundService.defaultScoringRules();
    private static final Item WHITE_BUDS_ENG3 =
            buds("T-1", ItemColor.WHITE, Building.ENGINEERING, 3, LocalDate.of(2026, 11, 22));

    private static ScoreDetail eval(int ruleIndex, SearchQuery q) {
        return RULES.get(ruleIndex).evaluate(WHITE_BUDS_ENG3, q);
    }

    @Test void 규칙은_5개이고_만점_합계는_100점() {
        SearchQuery full = query(Category.ELECTRONICS, ItemColor.WHITE, Building.ENGINEERING, 3, "버즈");
        int maxSum = RULES.stream().mapToInt(r -> r.evaluate(WHITE_BUDS_ENG3, full).max()).sum();
        assertEquals(5, RULES.size());
        assertEquals(100, maxSum);
    }

    @Test void 종류가_같으면_40점_다르면_0점() {
        assertEquals(40, eval(0, query(Category.ELECTRONICS, null, null, null, null)).earned());
        ScoreDetail diff = eval(0, query(Category.WALLET_CARD, null, null, null, null));
        assertEquals(0, diff.earned());
        assertTrue(diff.comparable());
    }

    @Test void 색상이_같으면_25점_같은_계열이면_10점_다르면_0점() {
        assertEquals(25, eval(1, query(null, ItemColor.WHITE, null, null, null)).earned());
        assertEquals(10, eval(1, query(null, ItemColor.SILVER, null, null, null)).earned()); // 무채색 계열
        assertEquals(0, eval(1, query(null, ItemColor.RED, null, null, null)).earned());
    }

    @Test void 건물이_같으면_20점_인접하면_10점_멀면_0점() {
        assertEquals(20, eval(2, query(null, null, Building.ENGINEERING, null, null)).earned());
        assertEquals(10, eval(2, query(null, null, Building.INDUSTRY_ACADEMY, null, null)).earned());
        assertEquals(0, eval(2, query(null, null, Building.DORMITORY, null, null)).earned());
    }

    @Test void 층이_같으면_5점() {
        assertEquals(5, eval(3, query(null, null, null, 3, null)).earned());
        assertEquals(0, eval(3, query(null, null, null, 2, null)).earned());
    }

    @Test void 세부_특징_키워드가_맞으면_점수를_받는다() {
        assertTrue(eval(4, query(null, null, null, null, "갤럭시 버즈")).earned() > 0);
        assertEquals(0, eval(4, query(null, null, null, null, "노트북")).earned());
    }

    @Test void 모름인_항목은_비교_불가로_분모에서_빠진다() {
        SearchQuery unknown = query(Category.UNKNOWN, ItemColor.UNKNOWN, Building.UNKNOWN, null, "");
        for (int i = 0; i < RULES.size(); i++) {
            assertFalse(eval(i, unknown).comparable(), "규칙 " + i + "는 모름일 때 분모에서 빠져야 함");
        }
    }

    @Test void 인접_관계는_대칭이고_같은_건물과_모름은_인접이_아니다() {
        for (Building a : Building.values()) {
            for (Building b : Building.values()) {
                assertEquals(a.isAdjacent(b), b.isAdjacent(a), a + " ↔ " + b);
            }
            assertFalse(a.isAdjacent(a));
            assertFalse(a.isAdjacent(Building.UNKNOWN));
        }
    }
}
