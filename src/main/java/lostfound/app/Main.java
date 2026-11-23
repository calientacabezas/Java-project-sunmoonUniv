package lostfound.app;

import lostfound.domain.*;
import lostfound.llm.LlmClient;
import lostfound.llm.LlmClientFactory;
import lostfound.parsing.LlmQueryParser;
import lostfound.parsing.QueryParser;
import lostfound.repository.InMemoryItemRepository;
import lostfound.repository.ItemRepository;
import lostfound.service.LostFoundService;
import lostfound.ui.MainFrame;

import javax.swing.SwingUtilities;
import java.time.LocalDate;

/** 프로그램 시작점: 객체를 조립하고 초기 샘플 데이터를 적재한 뒤 메인 창을 띄운다. 담당: 김경린. */
public class Main {
    public static void main(String[] args) {
        LlmClient llm = LlmClientFactory.create();
        QueryParser llmParser = new LlmQueryParser(llm);
        ItemRepository repository = new InMemoryItemRepository();
        seedSampleItems(repository);

        LostFoundService service = new LostFoundService(repository, llm, llmParser, null);

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(service);
            frame.setVisible(true);
        });
    }

    /** 저장소가 비어 있을 때 선문대 교내 습득물 초기 샘플 데이터를 적재한다. */
    public static void seedSampleItems(ItemRepository repository) {
        if (!repository.findAll().isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        repository.save(new ElectronicsItem(
                "ITEM-001", ItemColor.WHITE, today.minusDays(1), Building.ENGINEERING, 3,
                "갤럭시 버즈2 프로", "삼성", "버즈2 프로",
                "투명 하드케이스 장착, 공학관 3층 강의실 앞 책상 위 습득",
                "케이스 내부 뚜껑에 강아지 스티커 부착"));
        repository.save(new WalletCardItem(
                "ITEM-002", ItemColor.BLACK, today.minusDays(1), Building.LIBRARY, 2,
                "검정 반지갑", "반지갑", true,
                "도서관 2층 열람실 출입구 인근 습득",
                "선문대 컴퓨터공학부 학생증 및 체크카드 2장 포함"));
        repository.save(new BagClothingItem(
                "ITEM-003", ItemColor.NAVY, today.minusDays(2), Building.STUDENT_HALL, 1,
                "네이비 후드집업", "후드집업", "무신사 스탠다드",
                "학생회관 1층 휴게실 의자 위 습득",
                "오른쪽 주머니에 검은색 립밤 들어있음"));
        repository.save(new GeneralItem(
                "ITEM-004", ItemColor.SILVER, today, Building.WONHWA, 4,
                "스탠리 텀블러", "텀블러",
                "원화관 4층 복도 정수기 옆 습득",
                "바닥면에 이니셜 KKL 각인 있음"));
    }
}