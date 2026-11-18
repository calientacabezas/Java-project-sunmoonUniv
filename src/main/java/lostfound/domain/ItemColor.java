package lostfound.domain;

/** 색상. 값 목록은 권도영의 색상 계열·동의어 표를 보고 강채연이 확정. */
public enum ItemColor {
    WHITE("흰색", ColorGroup.ACHROMATIC),
    BLACK("검정", ColorGroup.ACHROMATIC),
    GRAY("회색", ColorGroup.ACHROMATIC),
    SILVER("은색", ColorGroup.ACHROMATIC),
    RED("빨강", ColorGroup.WARM),
    PINK("분홍", ColorGroup.WARM),
    ORANGE("주황", ColorGroup.WARM),
    YELLOW("노랑", ColorGroup.WARM),
    BLUE("파랑", ColorGroup.COOL),
    NAVY("남색", ColorGroup.COOL),
    GREEN("초록", ColorGroup.COOL),
    PURPLE("보라", ColorGroup.COOL),
    BROWN("갈색", ColorGroup.EARTH),
    BEIGE("베이지", ColorGroup.EARTH),
    UNKNOWN("모름", ColorGroup.NONE);

    private final String label;
    private final ColorGroup group;
    ItemColor(String label, ColorGroup group) { this.label = label; this.group = group; }
    public String label() { return label; }
    public ColorGroup group() { return group; }
}
