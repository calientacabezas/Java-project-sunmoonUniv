package lostfound.repository;

import lostfound.domain.Item;
import lostfound.exception.LostFoundException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 텍스트 파일 저장소. 등록·상태 변경 때마다 파일 전체를 다시 쓰고, 시작할 때 불러온다.
 * MySQL에 연결할 수 없을 때 자동으로 쓰이는 대체 저장소이기도 하다.
 *
 * <ul>
 *   <li>임시 파일(.tmp)에 먼저 쓴 뒤 바꿔치기하므로, 저장 도중 꺼져도 기존 파일이 깨지지 않는다.</li>
 *   <li>형식이 깨진 줄은 경고만 남기고 건너뛴다 (나머지 데이터는 그대로 읽음).</li>
 *   <li>파일이 없으면 처음 실행으로 보고 샘플 데이터로 채운다.</li>
 * </ul>
 */
public class TextFileItemRepository implements ItemRepository {

    public static final Path DEFAULT_PATH = Path.of("data", "items.txt");
    private static final String HEADER = "# version=1\n"
            + "# id|type|color|foundDate|building|floor|title|attr1|attr2|publicDetail|secret|photo|status\n";

    private final Path file;
    private final Map<String, Item> items = new LinkedHashMap<>();

    public TextFileItemRepository(Path file, List<? extends Item> seedIfMissing) {
        this.file = file;
        if (Files.exists(file)) {
            load();
        } else {
            for (Item item : seedIfMissing) items.put(item.getId(), item);
            writeAll();
        }
    }

    private void load() {
        try {
            int lineNo = 0;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                lineNo++;
                if (line.isBlank() || line.startsWith("#")) continue;
                try {
                    Item item = ItemCodec.fromLine(line);
                    items.put(item.getId(), item);
                } catch (RuntimeException e) {
                    System.err.printf("[파일 저장소] %d번째 줄 건너뜀 (%s)%n", lineNo, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new LostFoundException("저장 파일을 읽지 못했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void save(Item item) {
        items.put(item.getId(), item);
        writeAll();
    }

    @Override
    public synchronized List<Item> findAll() {
        return new ArrayList<>(items.values());
    }

    @Override
    public synchronized Optional<Item> findById(String id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public String describe() {
        return "파일 저장 (" + file.getFileName() + ")";
    }

    private void writeAll() {
        StringBuilder sb = new StringBuilder(HEADER);
        for (Item item : items.values()) sb.append(ItemCodec.toLine(item)).append('\n');
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new LostFoundException("저장 파일에 쓰지 못했습니다: " + e.getMessage(), e);
        }
    }
}
