package lostfound;

import lostfound.domain.Building;
import lostfound.domain.ElectronicsItem;
import lostfound.domain.GeneralItem;
import lostfound.domain.ItemColor;
import lostfound.domain.WalletCardItem;
import lostfound.matching.SearchQuery;
import lostfound.domain.Category;

import java.time.LocalDate;

/** 테스트용 물품·검색 조건을 짧게 만드는 도우미. */
public final class TestItems {
    private TestItems() {}

    public static ElectronicsItem buds(String id, ItemColor color, Building building, int floor, LocalDate date) {
        return new ElectronicsItem(id, color, date, building, floor, "갤럭시 버즈2 프로",
                "삼성", "버즈2 프로", "투명 하드케이스", "케이스 안쪽 강아지 스티커");
    }

    public static WalletCardItem wallet(String id, ItemColor color, Building building, int floor) {
        return new WalletCardItem(id, color, LocalDate.of(2026, 11, 20), building, floor, "가죽 반지갑",
                "반지갑", true, "열람실 앞", "학생증과 체크카드");
    }

    public static GeneralItem umbrella(String id, ItemColor color, Building building, int floor) {
        return new GeneralItem(id, color, LocalDate.of(2026, 11, 20), building, floor, "자동 장우산",
                "우산", "버튼식", "손잡이 이름표");
    }

    public static SearchQuery query(Category category, ItemColor color, Building building, Integer floor, String detail) {
        return new SearchQuery(category, color, null, building, floor, detail, detail == null ? "" : detail);
    }
}
