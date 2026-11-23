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
    /** 습득물 사진 파일 경로 (data/photos 아래 상대 경로). 없으면 null. */
    protected String photoPath;

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

    /** 검색어 키워드와 물품 세부 속성 간 양방향 부분 일치 점수 계산 (0~10). */
    protected int keywordMatchScore(SearchQuery query, String... candidates) {
        if (query == null || query.detailText() == null || query.detailText().isBlank()) {
            return 0;
        }
        String q = query.detailText().trim().toLowerCase();
        String[] tokens = q.split("[\\s,/·()]+");
        int hits = 0;

        for (String c : candidates) {
            if (c == null || c.isBlank()) continue;
            String cLower = c.trim().toLowerCase();
            boolean matched = q.contains(cLower);
            if (!matched) {
                for (String token : tokens) {
                    if (token.length() >= 2 && cLower.contains(token)) {
                        matched = true;
                        break;
                    }
                }
            }
            if (matched) {
                hits++;
            }
        }
        return Math.min(10, hits * 5);
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public void markChecking() {
        this.status = ItemStatus.CHECKING;
    }

    public void markReturned() {
        this.status = ItemStatus.RETURNED;
    }

    public void markExpired() {
        this.status = ItemStatus.EXPIRED;
    }

    public void markDisposed() {
        this.status = ItemStatus.DISPOSED;
    }

    /** 검색 매칭 대상 여부 (보관 중·확인 중 상태만 검색에 노출). */
    public boolean isMatchable() {
        return status == ItemStatus.STORED || status == ItemStatus.CHECKING;
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
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) {
        this.photoPath = (photoPath == null || photoPath.isBlank()) ? null : photoPath;
    }
    public ItemStatus getStatus() { return status; }
}