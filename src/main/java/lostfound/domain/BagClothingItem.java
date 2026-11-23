package lostfound.domain;

import lostfound.matching.SearchQuery;
import java.time.LocalDate;

public class BagClothingItem extends Item {
    private final String subType;
    private final String brandOrMaterial;

    public BagClothingItem(String id, ItemColor color, LocalDate foundDate,
                           Building building, int floor, String title,
                           String subType, String brandOrMaterial,
                           String publicDetail, String secret) {
        super(id, Category.BAG_CLOTHING, color, foundDate, building, floor, title, publicDetail, secret);
        this.subType = subType == null ? "" : subType;
        this.brandOrMaterial = brandOrMaterial == null ? "" : brandOrMaterial;
    }

    @Override
    public int detailScore(SearchQuery query) {
        return keywordMatchScore(query, subType, brandOrMaterial, title, publicDetail);
    }

    @Override
    public String specificSummary() {
        return "세부종류: " + subType + " / 브랜드·소재: " + brandOrMaterial;
    }

    public String getSubType() { return subType; }
    public String getBrandOrMaterial() { return brandOrMaterial; }
}