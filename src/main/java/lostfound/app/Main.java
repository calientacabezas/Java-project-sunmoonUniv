package lostfound.app;

import lostfound.domain.Item;
import lostfound.llm.LlmClient;
import lostfound.llm.LlmClientFactory;
import lostfound.matching.LlmSemanticReRanker;
import lostfound.matching.SemanticReRanker;
import lostfound.parsing.LlmQueryParser;
import lostfound.parsing.QueryParser;
import lostfound.repository.InMemoryItemRepository;
import lostfound.repository.ItemRepository;
import lostfound.repository.SampleDataLoader;
import lostfound.service.LostFoundService;
import lostfound.ui.MainFrame;
import lostfound.ui.Theme;

import javax.swing.SwingUtilities;
import java.util.List;

/**
 * 프로그램 시작점: 객체를 조립하고 메인 창을 띄운다. 담당: 김경린(화면), 강채연(조립·데이터).
 * 샘플 파일(data/sample/found_items.txt)이 있으면 그 30건을, 없으면 기본 샘플 4건을 쓴다.
 */
public class Main {
    public static void main(String[] args) {
        LlmClient llm = LlmClientFactory.create();
        QueryParser llmParser = new LlmQueryParser(llm);
        SemanticReRanker reRanker = new LlmSemanticReRanker(llm);
        ItemRepository repository = createRepository();

        LostFoundService service = new LostFoundService(repository, llm, llmParser, reRanker);

        SwingUtilities.invokeLater(() -> {
            Theme.install();
            MainFrame frame = new MainFrame(service);
            frame.setVisible(true);
        });
    }

    static ItemRepository createRepository() {
        List<Item> items = SampleDataLoader.loadOrEmpty(SampleDataLoader.DEFAULT_PATH);
        if (items.isEmpty()) {
            System.err.println("[샘플] 기본 샘플 4건으로 실행합니다.");
            return new InMemoryItemRepository();
        }
        System.err.println("[샘플] 습득물 " + items.size() + "건을 불러왔습니다.");
        return new InMemoryItemRepository(items);
    }
}
