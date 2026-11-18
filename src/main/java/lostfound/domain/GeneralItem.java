package lostfound.domain;

import lostfound.matching.SearchQuery;
import java.time.LocalDate;

public class GeneralItem extends Item {
    private final String subCategory;

    public GeneralItem(String id, ItemColor color, LocalDate foundDate,
                       Building building, int floor, String title,
                       String subCategory, String publicDetail, String secret) {
        super(id, Category.GENERAL, color, foundDate, building, floor, title, publicDetail, secret);
        this.subCategory = subCategory == null ? "" : subCategory;
    }

    @Override
    public int detailScore(SearchQuery query) {
        return keywordMatchScore(query, subCategory, title, publicDetail);
    }

    @Override
    public String specificSummary() {
        return "품목 구분: " + subCategory;
    }

    public String getSubCategory() { return subCategory; }
}
