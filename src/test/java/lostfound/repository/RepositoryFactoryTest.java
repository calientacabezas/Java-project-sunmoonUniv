package lostfound.repository;

import lostfound.domain.Building;
import lostfound.domain.GeneralItem;
import lostfound.domain.ItemColor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryFactoryTest {

    private static final List<GeneralItem> SEEDS = List.of(
            new GeneralItem("S-1", ItemColor.GRAY, LocalDate.of(2026, 11, 22), Building.MAIN_HALL, 2, "우산", "우산", "자동", "이름표"));

    @Test void DB_접속정보가_없으면_파일_저장소를_쓴다() throws IOException {
        Path file = Files.createTempDirectory("factory").resolve("items.txt");
        ItemRepository repo = RepositoryFactory.create(Map.of(), file, SEEDS);
        assertInstanceOf(TextFileItemRepository.class, repo);
        assertEquals(1, repo.findAll().size());
    }

    @Test void DB_연결에_실패하면_멈추지_않고_파일_저장소로_전환한다() throws IOException {
        Path file = Files.createTempDirectory("factory").resolve("items.txt");
        Map<String, String> env = Map.of(
                RepositoryFactory.ENV_URL, "jdbc:mysql://127.0.0.1:1/no_such_db?connectTimeout=500",
                RepositoryFactory.ENV_USER, "nobody",
                RepositoryFactory.ENV_PASSWORD, "wrong");
        ItemRepository repo = RepositoryFactory.create(env, file, SEEDS);
        assertInstanceOf(TextFileItemRepository.class, repo);
        assertTrue(repo.describe().contains("파일"));
    }
}
