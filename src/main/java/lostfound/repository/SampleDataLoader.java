package lostfound.repository;

import lostfound.domain.BagClothingItem;
import lostfound.domain.Building;
import lostfound.domain.ElectronicsItem;
import lostfound.domain.GeneralItem;
import lostfound.domain.Item;
import lostfound.domain.ItemColor;
import lostfound.domain.WalletCardItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * data/sample/found_items.txt 를 읽어 Item 객체 목록으로 만든다. 담당: 강채연.
 *
 * <pre>
 * 형식: id|type|color|foundDate|building|floor|title|attr1|attr2|publicDetail|secret[|photo]
 *   ELECTRONICS  attr1=브랜드     attr2=모델명
 *   WALLET_CARD  attr1=지갑 형태  attr2=신분증 포함 여부(Y/N)
 *   BAG_CLOTHING attr1=세부 종류  attr2=브랜드·소재
 *   GENERAL      attr1=품목 구분  attr2=(비움)
 *   photo(선택)  사진 파일 경로 (예: data/sample/photos/ITEM-001.jpg)
 * '#'으로 시작하는 줄과 빈 줄은 건너뛴다. 형식이 틀린 줄은 경고만 남기고 건너뛴다.
 * </pre>
 */
public final class SampleDataLoader {

    public static final Path DEFAULT_PATH = Path.of("data", "sample", "found_items.txt");
    private static final int FIELD_COUNT = 11;

    private SampleDataLoader() {}

    /** 파일이 없거나 읽을 수 없으면 빈 목록을 돌려준다 (프로그램이 멈추지 않도록). */
    public static List<Item> loadOrEmpty(Path path) {
        if (!Files.exists(path)) {
            System.err.println("[샘플] 파일이 없습니다: " + path.toAbsolutePath());
            return List.of();
        }
        try {
            return parse(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("[샘플] 파일을 읽지 못했습니다: " + e.getMessage());
            return List.of();
        }
    }

    public static List<Item> parse(List<String> lines) {
        List<Item> items = new ArrayList<>();
        int lineNo = 0;
        for (String line : lines) {
            lineNo++;
            if (line.isBlank() || line.startsWith("#")) continue;
            try {
                items.add(parseLine(line));
            } catch (RuntimeException e) {
                System.err.printf("[샘플] %d번째 줄 건너뜀 (%s): %s%n", lineNo, e.getMessage(), line);
            }
        }
        return items;
    }

    static Item parseLine(String line) {
        String[] f = line.split("\\|", -1);
        if (f.length != FIELD_COUNT && f.length != FIELD_COUNT + 1) {
            throw new IllegalArgumentException("항목 수 " + f.length + "개 (필요: " + FIELD_COUNT + "개, 사진 포함 시 "
                    + (FIELD_COUNT + 1) + "개)");
        }
        for (int i = 0; i < f.length; i++) f[i] = f[i].trim();

        String id = f[0];
        String type = f[1];
        ItemColor color = ItemColor.valueOf(f[2]);
        LocalDate date = LocalDate.parse(f[3]);
        Building building = Building.valueOf(f[4]);
        int floor = Integer.parseInt(f[5]);
        String title = f[6];
        String attr1 = f[7];
        String attr2 = f[8];
        String publicDetail = f[9];
        String secret = f[10];

        String photo = f.length > FIELD_COUNT ? f[FIELD_COUNT] : null;

        Item item = switch (type) {
            case "ELECTRONICS" -> new ElectronicsItem(id, color, date, building, floor, title,
                    attr1, attr2, publicDetail, secret);
            case "WALLET_CARD" -> new WalletCardItem(id, color, date, building, floor, title,
                    attr1, "Y".equalsIgnoreCase(attr2), publicDetail, secret);
            case "BAG_CLOTHING" -> new BagClothingItem(id, color, date, building, floor, title,
                    attr1, attr2, publicDetail, secret);
            case "GENERAL" -> new GeneralItem(id, color, date, building, floor, title,
                    attr1, publicDetail, secret);
            default -> throw new IllegalArgumentException("알 수 없는 종류: " + type);
        };
        item.setPhotoPath(photo);
        return item;
    }
}
