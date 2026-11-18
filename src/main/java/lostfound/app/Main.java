package lostfound.app;

import lostfound.llm.LlmClient;
import lostfound.llm.LlmClientFactory;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** 프로그램 시작점: 객체를 조립하고 메인 창을 띄운다. 화면 담당: 김경린. */
public class Main {
    public static void main(String[] args) {
        LlmClient llm = LlmClientFactory.create();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("교내 분실물 회수 지원 시스템");
            String mode = llm.isOnline() ? "온라인" : "오프라인";
            frame.add(new JLabel("화면 준비 중 (LLM: " + mode + ")", SwingConstants.CENTER));
            frame.setSize(900, 600);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}

