package lostfound.domain;

import lostfound.matching.SearchQuery;
import java.time.LocalDate;

public class ElectronicsItem extends Item {
    private final String brand;
    private final String modelName;

    public ElectronicsItem(String id, ItemColor color, LocalDate foundDate,
                           Building building, int floor, String title,
                           String brand, String modelName,
                           String publicDetail, String secret) {
        super(id, Category.ELECTRONICS, color, foundDate, building, floor, title, publicDetail, secret);
        this.brand = brand == null ? "" : brand;
        this.modelName = modelName == null ? "" : modelName;
    }

    @Override
    public int detailScore(SearchQuery query) {
        return keywordMatchScore(query, brand, modelName, title, publicDetail);
    }

    @Override
    public String specificSummary() {
        return "브랜드: " + brand + " / 모델: " + modelName;
    }

    public String getBrand() { return brand; }
    public String getModelName() { return modelName; }
}
