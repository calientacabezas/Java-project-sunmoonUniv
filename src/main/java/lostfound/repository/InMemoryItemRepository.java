package lostfound.repository;

import lostfound.domain.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 메모리 저장소 구현체 (담당: 김경린, 화면·테스트용). */
public class InMemoryItemRepository implements ItemRepository {
    private final Map<String, Item> storage = new LinkedHashMap<>();

    public InMemoryItemRepository() {
        seedSampleItems();
    }

    private void seedSampleItems() {
        save(new ElectronicsItem(
                "ITEM-001", ItemColor.WHITE, LocalDate.now().minusDays(1),
                Building.ENGINEERING, 3, "갤럭시 버즈2 프로",
                "삼성", "버즈2 프로",
                "하얀색 무선이어폰 케이스, 전면 LED 정상 점등",
                "케이스 안쪽 뚜껑에 이니셜 스티커 부착"
        ));
        save(new WalletCardItem(
                "ITEM-002", ItemColor.NAVY, LocalDate.now(),
                Building.LIBRARY, 2, "남색 가죽 카드지갑",
                "카드지갑", true,
                "지퍼 포켓이 있는 남색 반지갑 형태",
                "선문대 학생증(학번 끝자리 4821) 및 체크카드 1장 수납"
        ));
        save(new BagClothingItem(
                "ITEM-003", ItemColor.BLACK, LocalDate.now().minusDays(2),
                Building.STUDENT_HALL, 1, "검정 백팩",
                "백팩", "나이키",
                "앞주머니에 열쇠고리 고리가 달린 검정 가방",
                "안쪽 주머니에 C언어 전공서적과 검정 필통 들어있음"
        ));
        save(new GeneralItem(
                "ITEM-004", ItemColor.GRAY, LocalDate.now().minusDays(1),
                Building.MAIN_HALL, 2, "회색 자동 장우산",
                "우산",
                "회색 원단, 손잡이 버튼식 자동우산",
                "손잡이 하단부에 이름표 테이프 부착"
        ));
    }

    @Override
    public void save(Item item) {
        storage.put(item.getId(), item);
    }

    @Override
    public List<Item> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public Optional<Item> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}
