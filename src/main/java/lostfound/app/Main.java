package lostfound.app;

import lostfound.llm.LlmClient;
import lostfound.llm.LlmClientFactory;
import lostfound.parsing.LlmQueryParser;
import lostfound.parsing.QueryParser;
import lostfound.repository.InMemoryItemRepository;
import lostfound.repository.ItemRepository;
import lostfound.service.LostFoundService;
import lostfound.ui.MainFrame;

import javax.swing.SwingUtilities;

/** 프로그램 시작점: 객체를 조립하고 메인 창을 띄운다. 화면 담당: 김경린. */
public class Main {
    public static void main(String[] args) {
        LlmClient llm = LlmClientFactory.create();
        QueryParser parser = new LlmQueryParser(llm);
        ItemRepository repository = new InMemoryItemRepository();
        LostFoundService service = new LostFoundService(repository, llm, parser, null);

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(service);
            frame.setVisible(true);
        });
    }
}
