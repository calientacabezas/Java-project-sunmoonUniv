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
    protected final String title;
    protected final String publicDetail;
    protected final String secret;
    protected ItemStatus status = ItemStatus.STORED;

    protected Item(String id, Category category, ItemColor color,
                   LocalDate foundDate, Building building, int floor) {
        this(id, category, color, foundDate, building, floor, category.label(), "", "");
    }

    protected Item(String id, Category category, ItemColor color,
                   LocalDate foundDate, Building building, int floor,
                   String title, String publicDetail, String secret) {
        this.id = id;
        this.category = category;
        this.color = color;
        this.foundDate = foundDate;
        this.building = building;
        this.floor = floor;
        this.title = (title == null || title.isBlank()) ? category.label() : title;
        this.publicDetail = (publicDetail == null) ? "" : publicDetail;
        this.secret = (secret == null) ? "" : secret;
    }

    /** 종류별 세부 특징 점수 (0~10). */
    public abstract int detailScore(SearchQuery query);

    /** 하위 클래스 고유 속성 요약 문자열 (UI 표시용). */
    public abstract String specificSummary();

    protected int keywordMatchScore(SearchQuery query, String... candidates) {
        if (query == null || query.detailText() == null || query.detailText().isBlank()) {
            return 0;
        }
        String q = query.detailText().toLowerCase();
        int hits = 0;
        for (String c : candidates) {
            if (c != null && !c.isBlank() && q.contains(c.trim().toLowerCase())) {
                hits++;
            }
        }
        return Math.min(10, hits * 5);
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public void markReturned() {
        this.status = ItemStatus.RETURNED;
    }

    public String getId() { return id; }
    public Category getCategory() { return category; }
    public ItemColor getColor() { return color; }
    public LocalDate getFoundDate() { return foundDate; }
    public Building getBuilding() { return building; }
    public int getFloor() { return floor; }
    public String getTitle() { return title; }
    public String getPublicDetail() { return publicDetail; }
    public String getSecret() { return secret; }
    public ItemStatus getStatus() { return status; }
}