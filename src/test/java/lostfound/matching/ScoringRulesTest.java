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

/** 기본 점수 규칙 6개(종류 35 / 색상 20 / 날짜 15 / 건물 15 / 층 5 / 세부 10) 검증. */
class ScoringRulesTest {

    private static final List<ScoringRule> RULES = LostFoundService.defaultScoringRules();
    private static final Item WHITE_BUDS_ENG3 =
            buds("T-1", ItemColor.WHITE, Building.ENGINEERING, 3, LocalDate.of(2026, 11, 22));

    private static ScoreDetail eval(int ruleIndex, SearchQuery q) {
        return RULES.get(ruleIndex).evaluate(WHITE_BUDS_ENG3, q);
    }

    private static final int KIND = 0, COLOR = 1, DATE = 2, BUILDING = 3, FLOOR = 4, DETAIL = 5;

    private static SearchQuery lostOn(LocalDate lostDate) {
        return new SearchQuery(null, null, lostDate, null, null, null, "");
    }

    @Test void 규칙은_6개이고_만점_합계는_100점() {
        SearchQuery full = new SearchQuery(Category.ELECTRONICS, ItemColor.WHITE, LocalDate.of(2026, 11, 22),
                Building.ENGINEERING, 3, "버즈", "버즈");
        int maxSum = RULES.stream().mapToInt(r -> r.evaluate(WHITE_BUDS_ENG3, full).max()).sum();
        assertEquals(6, RULES.size());
        assertEquals(100, maxSum);
    }

    @Test void 종류가_같으면_35점_다르면_0점() {
        assertEquals(35, eval(KIND, query(Category.ELECTRONICS, null, null, null, null)).earned());
        ScoreDetail diff = eval(KIND, query(Category.WALLET_CARD, null, null, null, null));
        assertEquals(0, diff.earned());
        assertTrue(diff.comparable());
    }

    @Test void 색상이_같으면_20점_같은_계열이면_10점_다르면_0점() {
        assertEquals(20, eval(COLOR, query(null, ItemColor.WHITE, null, null, null)).earned());
        assertEquals(10, eval(COLOR, query(null, ItemColor.SILVER, null, null, null)).earned()); // 무채색 계열
        assertEquals(0, eval(COLOR, query(null, ItemColor.RED, null, null, null)).earned());
    }

    @Test void 날짜는_하루_이틀_차이도_부분_점수를_준다() {
        // 물품 습득일은 2026-11-22
        assertEquals(15, eval(DATE, lostOn(LocalDate.of(2026, 11, 22))).earned()); // 같은 날
        assertEquals(15, eval(DATE, lostOn(LocalDate.of(2026, 11, 21))).earned()); // 다음 날 습득
        assertEquals(10, eval(DATE, lostOn(LocalDate.of(2026, 11, 23))).earned()); // 날짜를 하루 늦게 기억
        assertEquals(10, eval(DATE, lostOn(LocalDate.of(2026, 11, 20))).earned()); // 2일 뒤 습득
        assertEquals(5, eval(DATE, lostOn(LocalDate.of(2026, 11, 16))).earned());  // 6일 뒤 습득
        assertEquals(0, eval(DATE, lostOn(LocalDate.of(2026, 11, 10))).earned());  // 12일 뒤 습득
        assertEquals(0, eval(DATE, lostOn(LocalDate.of(2026, 11, 25))).earned());  // 잃어버리기 3일 전에 주움
    }

    @Test void 분실_날짜를_모르면_날짜는_분모에서_빠진다() {
        assertFalse(eval(DATE, lostOn(null)).comparable());
    }

    @Test void 건물이_같으면_15점_인접하면_8점_멀면_0점() {
        assertEquals(15, eval(BUILDING, query(null, null, Building.ENGINEERING, null, null)).earned());
        assertEquals(8, eval(BUILDING, query(null, null, Building.INDUSTRY_ACADEMY, null, null)).earned());
        assertEquals(0, eval(BUILDING, query(null, null, Building.DORMITORY, null, null)).earned());
    }

    @Test void 층이_같으면_5점() {
        assertEquals(5, eval(FLOOR, query(null, null, null, 3, null)).earned());
        assertEquals(0, eval(FLOOR, query(null, null, null, 2, null)).earned());
    }

    @Test void 세부_특징_키워드가_맞으면_점수를_받는다() {
        assertTrue(eval(DETAIL, query(null, null, null, null, "갤럭시 버즈")).earned() > 0);
        assertEquals(0, eval(DETAIL, query(null, null, null, null, "노트북")).earned());
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
