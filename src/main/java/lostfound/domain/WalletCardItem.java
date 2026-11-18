package lostfound.domain;

import lostfound.matching.SearchQuery;
import java.time.LocalDate;

public class WalletCardItem extends Item {
    private final String walletType;
    private final boolean includesIdCard;

    public WalletCardItem(String id, ItemColor color, LocalDate foundDate,
                          Building building, int floor, String title,
                          String walletType, boolean includesIdCard,
                          String publicDetail, String secret) {
        super(id, Category.WALLET_CARD, color, foundDate, building, floor, title, publicDetail, secret);
        this.walletType = walletType == null ? "" : walletType;
        this.includesIdCard = includesIdCard;
    }

    @Override
    public int detailScore(SearchQuery query) {
        String idKeyword = includesIdCard ? "학생증 신분증 카드" : "";
        return keywordMatchScore(query, walletType, idKeyword, title, publicDetail);
    }

    @Override
    public String specificSummary() {
        return "형태: " + walletType + (includesIdCard ? " (신분증/학생증 포함)" : " (신분증 미포함)");
    }

    public String getWalletType() { return walletType; }
    public boolean isIncludesIdCard() { return includesIdCard; }
}
