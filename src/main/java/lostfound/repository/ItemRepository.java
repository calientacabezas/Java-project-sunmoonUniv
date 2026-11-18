package lostfound.repository;

import lostfound.domain.Item;
import java.util.List;
import java.util.Optional;

/** 저장소. 구현체: InMemoryItemRepository(김경린, 화면·테스트용), TextFileItemRepository(김동민). */
public interface ItemRepository {
    void save(Item item);
    List<Item> findAll();
    Optional<Item> findById(String id);
}

