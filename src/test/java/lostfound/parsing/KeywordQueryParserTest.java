package lostfound.parsing;

import lostfound.domain.Building;
import lostfound.domain.Category;
import lostfound.domain.ItemColor;
import lostfound.matching.SearchQuery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 실험 기준선 파서: 표준 이름만 인식하고 동의어는 인식하지 않아야 한다. */
class KeywordQueryParserTest {

    private final KeywordQueryParser parser = new KeywordQueryParser();

    @Test void 표준_이름은_인식한다() {
        SearchQuery q = parser.parse("공학관 3층에서 흰색 전자기기를 잃어버렸어요");
        assertEquals(Category.ELECTRONICS, q.category());
        assertEquals(ItemColor.WHITE, q.color());
        assertEquals(Building.ENGINEERING, q.building());
        assertEquals(3, q.floor());
    }

    @Test void 동의어와_약칭은_인식하지_않는다() {
        SearchQuery q = parser.parse("공대 3층에서 하얀 버즈 잃어버렸어요");
        assertEquals(Category.UNKNOWN, q.category());
        assertEquals(ItemColor.UNKNOWN, q.color());
        assertEquals(Building.UNKNOWN, q.building());
    }

    @Test void 외국어는_인식하지_않는다() {
        SearchQuery q = parser.parse("I lost white earbuds in the engineering building");
        assertEquals(ItemColor.UNKNOWN, q.color());
        assertEquals(Building.UNKNOWN, q.building());
        assertNull(q.floor());
    }

    @Test void 지갑_카드처럼_라벨의_일부_단어도_표준어로_본다() {
        assertEquals(Category.WALLET_CARD, parser.parse("검정 지갑 분실").category());
        assertEquals(Category.BAG_CLOTHING, parser.parse("가방을 두고 왔어요").category());
    }

    @Test void 보건의료관과_평생교육원은_같은_건물이다() {
        assertEquals(Building.HEALTH_MEDICAL, parser.parse("평생교육원 2층").building());
        assertEquals(Building.HEALTH_MEDICAL, parser.parse("보건의료관 2층").building());
    }

    @Test void 지하층과_범위_밖_층수를_처리한다() {
        assertEquals(-1, parser.parse("도서관 지하 1층").floor());
        assertNull(parser.parse("도서관 99층").floor());
        assertNull(parser.parse("층수 모름").floor());
    }

    @Test void 날짜는_추출하지_않고_원문을_세부_특징으로_쓴다() {
        SearchQuery q = parser.parse("어제 도서관에서 노트북 분실");
        assertNull(q.lostDate());
        assertEquals("어제 도서관에서 노트북 분실", q.detailText());
    }

    @Test void null_입력도_안전하다() {
        SearchQuery q = parser.parse(null);
        assertEquals(Category.UNKNOWN, q.category());
        assertEquals("", q.rawText());
    }
}
