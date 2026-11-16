package lostfound.domain;

import lostfound.matching.SearchQuery;
import java.time.LocalDate;

/**
 * 습득물 추상 클래스. 담당: 김경린.
 * 하위 클래스 4개(ElectronicsItem, WalletCardItem, BagClothingItem, GeneralItem)가
 * detailScore()를 재정의해 종류별 세부 특징을 비교한다.
 * 주의: secret(비공개 특징)은 검색 결과 객체와 LLM 요청에 절대 넣지 않는다.
 */
public abstract class Item {
    protected final String id;
    protected final Category category;
    protected final ItemColor color;
    protected final LocalDate foundDate;
    protected final Building building;
    protected final int floor;
    protected ItemStatus status = ItemStatus.STORED;

    protected Item(String id, Category category, ItemColor color,
                   LocalDate foundDate, Building building, int floor) {
        this.id = id;
        this.category = category;
        this.color = color;
        this.foundDate = foundDate;
        this.building = building;
        this.floor = floor;
    }

    /** 종류별 세부 특징 점수 (0~10). */
    public abstract int detailScore(SearchQuery query);

    public String getId() { return id; }
    public Category getCategory() { return category; }
    public ItemColor getColor() { return color; }
    public LocalDate getFoundDate() { return foundDate; }
    public Building getBuilding() { return building; }
    public int getFloor() { return floor; }
    public ItemStatus getStatus() { return status; }
}
