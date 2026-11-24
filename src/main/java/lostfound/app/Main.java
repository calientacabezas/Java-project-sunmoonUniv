package lostfound.app;

import lostfound.llm.LlmClient;
import lostfound.llm.LlmClientFactory;
import lostfound.matching.LlmSemanticReRanker;
import lostfound.matching.SemanticReRanker;
import lostfound.parsing.LlmQueryParser;
import lostfound.parsing.QueryParser;
import lostfound.repository.ItemRepository;
import lostfound.repository.RepositoryFactory;
import lostfound.service.LostFoundService;
import lostfound.ui.MainFrame;
import lostfound.ui.Theme;

import javax.swing.SwingUtilities;

/**
 * 프로그램 시작점: 객체를 조립하고 메인 창을 띄운다.
 * 저장소는 MySQL(환경변수 LF_DB_URL 설정 시)을 쓰고, 연결할 수 없으면 텍스트 파일(data/items.txt)로 저장한다.
 * 처음 실행이라 데이터가 없으면 샘플 30건(data/sample/found_items.txt)으로 채운다.
 */
public class Main {
    public static void main(String[] args) {
        LlmClient llm = LlmClientFactory.create();
        QueryParser llmParser = new LlmQueryParser(llm);
        SemanticReRanker reRanker = new LlmSemanticReRanker(llm);
        ItemRepository repository = RepositoryFactory.create();   // MySQL → 실패 시 텍스트 파일

        LostFoundService service = new LostFoundService(repository, llm, llmParser, reRanker);

        SwingUtilities.invokeLater(() -> {
            Theme.install();
            MainFrame frame = new MainFrame(service);
            frame.setVisible(true);
        });
    }

}
