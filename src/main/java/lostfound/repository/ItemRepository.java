package lostfound.repository;

import lostfound.domain.Item;
import java.util.List;
import java.util.Optional;

/**
 * 습득물 저장소 (저장소 패턴). 서비스는 이 인터페이스에만 의존하므로 저장 방식을 바꿔도 다른 코드는 그대로다.
 * 구현체: MySqlItemRepository(JDBC), TextFileItemRepository(텍스트 파일), InMemoryItemRepository(테스트·실험용).
 * 어떤 구현체를 쓸지는 RepositoryFactory가 시작할 때 정한다.
 */
public interface ItemRepository {
    void save(Item item);
    List<Item> findAll();
    Optional<Item> findById(String id);

    /** 화면 상단에 보여 줄 저장 방식 설명. */
    default String describe() {
        return "메모리 저장 (종료하면 사라짐)";
    }
}
