package lostfound.repository;

import lostfound.domain.Item;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 시작할 때 저장소를 고른다.
 * <ol>
 *   <li>환경변수 LF_DB_URL이 있고 MySQL 연결에 성공하면 MySQL 저장소 (DB가 비어 있으면 샘플로 채움)</li>
 *   <li>접속 정보가 없거나 연결에 실패하면 텍스트 파일 저장소 (data/items.txt)</li>
 * </ol>
 * 발표장 PC에서 MySQL이 꺼져 있어도 프로그램이 멈추지 않도록 하기 위한 대체 경로다 (LLM 장애 대응과 같은 원칙).
 */
public final class RepositoryFactory {

    public static final String ENV_URL = "LF_DB_URL";
    public static final String ENV_USER = "LF_DB_USER";
    public static final String ENV_PASSWORD = "LF_DB_PASSWORD";

    private RepositoryFactory() {}

    public static ItemRepository create() {
        List<Item> seeds = SampleDataLoader.loadOrEmpty(SampleDataLoader.DEFAULT_PATH);
        if (seeds.isEmpty()) seeds = new InMemoryItemRepository().findAll();   // 샘플 파일도 없으면 기본 4건
        return create(System.getenv(), TextFileItemRepository.DEFAULT_PATH, seeds);
    }

    /** 테스트에서 환경변수와 파일 위치를 바꿔 넣을 수 있게 분리한 버전. */
    static ItemRepository create(Map<String, String> env, Path textFile, List<? extends Item> seeds) {
        String url = env.get(ENV_URL);
        if (url != null && !url.isBlank()) {
            try {
                MySqlItemRepository db = new MySqlItemRepository(url,
                        env.getOrDefault(ENV_USER, ""), env.getOrDefault(ENV_PASSWORD, ""));
                if (db.findAll().isEmpty()) {
                    for (Item item : seeds) db.save(item);
                    System.err.println("[저장소] MySQL이 비어 있어 샘플 " + seeds.size() + "건을 넣었습니다.");
                }
                System.err.println("[저장소] MySQL 저장소를 사용합니다.");
                return db;
            } catch (RuntimeException e) {
                System.err.println("[저장소] " + e.getMessage() + " → 파일 저장으로 실행합니다.");
            }
        }
        System.err.println("[저장소] 파일 저장소를 사용합니다: " + textFile.toAbsolutePath());
        return new TextFileItemRepository(textFile, seeds);
    }
}
