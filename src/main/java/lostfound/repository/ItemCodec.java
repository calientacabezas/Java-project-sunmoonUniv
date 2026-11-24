package lostfound.repository;

import lostfound.domain.BagClothingItem;
import lostfound.domain.Building;
import lostfound.domain.ElectronicsItem;
import lostfound.domain.GeneralItem;
import lostfound.domain.Item;
import lostfound.domain.ItemColor;
import lostfound.domain.ItemStatus;
import lostfound.domain.WalletCardItem;

import java.time.LocalDate;

/**
 * Item ↔ 저장용 문자열 필드 변환. 텍스트 파일 저장소, MySQL 저장소, 샘플 로더가 함께 쓴다.
 * 하위 클래스마다 다른 고유 속성은 attr1·attr2 두 칸으로 맞춘다.
 *
 * <pre>
 * 필드 순서: id, type, color, foundDate, building, floor, title, attr1, attr2, publicDetail, secret, photo, status
 *   ELECTRONICS  attr1=브랜드     attr2=모델명
 *   WALLET_CARD  attr1=지갑 형태  attr2=신분증 포함 여부(Y/N)
 *   BAG_CLOTHING attr1=세부 종류  attr2=브랜드·소재
 *   GENERAL      attr1=품목 구분  attr2=(비움)
 * </pre>
 */
public final class ItemCodec {

    public static final int REQUIRED_FIELDS = 11;   // photo, status는 선택
    public static final int ALL_FIELDS = 13;

    private ItemCodec() {}

    /** 저장할 13개 필드. 값이 없으면 빈 문자열. */
    public static String[] fields(Item item) {
        String type;
        String attr1;
        String attr2;
        if (item instanceof ElectronicsItem e) {
            type = "ELECTRONICS"; attr1 = e.getBrand(); attr2 = e.getModelName();
        } else if (item instanceof WalletCardItem w) {
            type = "WALLET_CARD"; attr1 = w.getWalletType(); attr2 = w.isIncludesIdCard() ? "Y" : "N";
        } else if (item instanceof BagClothingItem b) {
            type = "BAG_CLOTHING"; attr1 = b.getSubType(); attr2 = b.getBrandOrMaterial();
        } else if (item instanceof GeneralItem g) {
            type = "GENERAL"; attr1 = g.getSubCategory(); attr2 = "";
        } else {
            throw new IllegalArgumentException("저장할 수 없는 물품 종류: " + item.getClass().getSimpleName());
        }
        return new String[]{
                item.getId(), type, item.getColor().name(), item.getFoundDate().toString(),
                item.getBuilding().name(), String.valueOf(item.getFloor()), nz(item.getTitle()),
                nz(attr1), nz(attr2), nz(item.getPublicDetail()), nz(item.getSecret()),
                nz(item.getPhotoPath()), item.getStatus().name()
        };
    }

    /** 필드 11~13개로 Item을 만든다. 형식이 틀리면 IllegalArgumentException. */
    public static Item fromFields(String[] raw) {
        if (raw.length < REQUIRED_FIELDS || raw.length > ALL_FIELDS) {
            throw new IllegalArgumentException("항목 수 " + raw.length + "개 (필요: "
                    + REQUIRED_FIELDS + "~" + ALL_FIELDS + "개)");
        }
        String[] f = new String[raw.length];
        for (int i = 0; i < raw.length; i++) f[i] = raw[i] == null ? "" : raw[i].trim();

        String id = f[0];
        if (id.isEmpty()) throw new IllegalArgumentException("ID가 비어 있음");
        ItemColor color = ItemColor.valueOf(f[2]);
        LocalDate date = LocalDate.parse(f[3]);
        Building building = Building.valueOf(f[4]);
        int floor = Integer.parseInt(f[5]);
        String title = f[6], attr1 = f[7], attr2 = f[8], publicDetail = f[9], secret = f[10];

        Item item = switch (f[1]) {
            case "ELECTRONICS" -> new ElectronicsItem(id, color, date, building, floor, title,
                    attr1, attr2, publicDetail, secret);
            case "WALLET_CARD" -> new WalletCardItem(id, color, date, building, floor, title,
                    attr1, "Y".equalsIgnoreCase(attr2), publicDetail, secret);
            case "BAG_CLOTHING" -> new BagClothingItem(id, color, date, building, floor, title,
                    attr1, attr2, publicDetail, secret);
            case "GENERAL" -> new GeneralItem(id, color, date, building, floor, title,
                    attr1, publicDetail, secret);
            default -> throw new IllegalArgumentException("알 수 없는 종류: " + f[1]);
        };
        if (f.length > 11) item.setPhotoPath(f[11]);
        if (f.length > 12 && !f[12].isEmpty()) item.setStatus(ItemStatus.valueOf(f[12]));
        return item;
    }

    /** 텍스트 파일 한 줄 ('|' 구분). 값 안의 '|'와 줄바꿈은 저장 전에 바꿔 형식이 깨지지 않게 한다. */
    public static String toLine(Item item) {
        String[] f = fields(item);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < f.length; i++) {
            if (i > 0) sb.append('|');
            sb.append(clean(f[i]));
        }
        return sb.toString();
    }

    public static Item fromLine(String line) {
        return fromFields(line.split("\\|", -1));
    }

    static String clean(String value) {
        return value.replace('|', '/').replace("\r", " ").replace("\n", " ");
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
