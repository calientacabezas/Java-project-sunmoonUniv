package lostfound.repository;

import lostfound.domain.Item;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * data/sample/found_items.txt 를 읽어 Item 객체 목록으로 만든다.
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
        return ItemCodec.fromLine(line);
    }
}
