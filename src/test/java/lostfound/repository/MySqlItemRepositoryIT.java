package lostfound.repository;

import lostfound.domain.Building;
import lostfound.domain.ElectronicsItem;
import lostfound.domain.Item;
import lostfound.domain.ItemColor;
import lostfound.domain.ItemStatus;
import lostfound.domain.WalletCardItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 실제 MySQL 연동 테스트. 환경변수 LF_DB_URL이 있을 때만 실행되고, 없으면 건너뛴다 (CI는 건너뜀).
 * 테스트용 ID(IT-로 시작)만 쓰므로 기존 데이터는 건드리지 않는다.
 */
class MySqlItemRepositoryIT {

    private static MySqlItemRepository repoOrSkip() {
        String url = System.getenv(RepositoryFactory.ENV_URL);
        assumeTrue(url != null && !url.isBlank(), "LF_DB_URL이 없어 MySQL 테스트를 건너뜁니다.");
        return new MySqlItemRepository(url,
                System.getenv().getOrDefault(RepositoryFactory.ENV_USER, ""),
                System.getenv().getOrDefault(RepositoryFactory.ENV_PASSWORD, ""));
    }

    @Test void 저장하고_다시_읽으면_같은_내용이다() {
        MySqlItemRepository repo = repoOrSkip();
        ElectronicsItem item = new ElectronicsItem("IT-1", ItemColor.WHITE, LocalDate.of(2026, 11, 22),
                Building.ENGINEERING, 3, "갤럭시 버즈", "삼성", "버즈2", "투명 케이스", "강아지 스티커");
        repo.save(item);

        ElectronicsItem read = (ElectronicsItem) repo.findById("IT-1").orElseThrow();
        assertEquals("갤럭시 버즈", read.getTitle());
        assertEquals("삼성", read.getBrand());
        assertEquals(LocalDate.of(2026, 11, 22), read.getFoundDate());
        assertEquals("강아지 스티커", read.getSecret());
    }

    @Test void 같은_ID로_저장하면_상태가_갱신된다() {
        MySqlItemRepository repo = repoOrSkip();
        WalletCardItem item = new WalletCardItem("IT-2", ItemColor.BLACK, LocalDate.of(2026, 11, 22),
                Building.LIBRARY, 2, "반지갑", "반지갑", true, "가죽", "학생증");
        repo.save(item);
        item.setStatus(ItemStatus.RETURNED);
        repo.save(item);

        Item read = repo.findById("IT-2").orElseThrow();
        assertEquals(ItemStatus.RETURNED, read.getStatus());
        assertTrue(((WalletCardItem) read).isIncludesIdCard());
    }

    @Test void 없는_ID는_빈_결과다() {
        assertTrue(repoOrSkip().findById("IT-없음").isEmpty());
    }
}
