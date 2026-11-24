package lostfound.repository;

import lostfound.domain.Item;
import java.util.List;
import java.util.Optional;

/** 저장소. 현재 구현체는 InMemoryItemRepository (파일·DB 저장은 이 인터페이스의 구현체를 추가해 확장). */
public interface ItemRepository {
    void save(Item item);
    List<Item> findAll();
    Optional<Item> findById(String id);
}

