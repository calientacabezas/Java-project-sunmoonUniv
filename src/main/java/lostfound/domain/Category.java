package lostfound.domain;

/** 물품 종류. 담당: 강채연. UNKNOWN = 사용자가 "모름"을 선택 (점수 분모에서 제외). */
public enum Category {
    ELECTRONICS("전자기기"),
    WALLET_CARD("지갑·카드"),
    BAG_CLOTHING("가방·의류"),
    GENERAL("기타"),
    UNKNOWN("모름");

    private final String label;
    Category(String label) { this.label = label; }
    public String label() { return label; }
}
