package lostfound.eval;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MetricsTest {
    @Test void 상위3개_포함률() {
        List<List<String>> ranked = List.of(
                List.of("F1", "F2", "F3"),        // 1위 적중
                List.of("F4", "F5", "F6", "F7"),  // 4위라 미포함
                List.of("F8", "F9"));             // 2위 적중
        List<String> answers = List.of("F1", "F7", "F9");
        assertEquals(2.0 / 3, Metrics.topKHitRate(ranked, answers, 3), 1e-9);
    }

    @Test void 평균역순위() {
        List<List<String>> ranked = List.of(List.of("A", "B"), List.of("C", "D"));
        assertEquals((1.0 + 0.5) / 2, Metrics.meanReciprocalRank(ranked, List.of("A", "D")), 1e-9);
    }

    @Test void 결과와_정답_수가_다르면_예외() {
        assertThrows(IllegalArgumentException.class,
                () -> Metrics.topKHitRate(List.of(List.of("A")), List.of(), 3));
    }
}
