package lostfound.eval;

import lostfound.app.Main;
import lostfound.domain.Item;
import lostfound.llm.LlmClient;
import lostfound.llm.LlmClientFactory;
import lostfound.matching.MatchResult;
import lostfound.matching.SearchQuery;
import lostfound.parsing.LlmQueryParser;
import lostfound.repository.InMemoryItemRepository;
import lostfound.repository.ItemRepository;
import lostfound.service.LostFoundService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 규칙만 vs 하이브리드 비교 실험 실행기. 실행: ./gradlew evaluate
 *  - 규칙만:   KeywordQueryParser → 규칙 5개 점수 → 정렬
 *  - 하이브리드: PiiMasker → LlmQueryParser → 규칙 5개 점수 → 상위 10개 SemanticReRanker
 * 담당: 강채연(코드), 권도영(평가 데이터·실행 기록).
 */
public class AccuracyEvaluator {
    static final Path CASES = Path.of("data", "eval", "eval_cases.txt");

    public static void main(String[] args) throws IOException {
        if (!Files.exists(CASES)) {
            System.err.println("[평가] 평가 데이터 파일이 없습니다: " + CASES.toAbsolutePath());
            return;
        }

        List<EvalCase> cases = load(CASES);
        System.out.printf("평가 케이스 %d건 로드%n", cases.size());
        cases.stream()
             .collect(java.util.stream.Collectors.groupingBy(EvalCase::type,
                      java.util.TreeMap::new, java.util.stream.Collectors.counting()))
             .forEach((t, n) -> System.out.printf("  %-8s %d건%n", t, n));

        ItemRepository repository = new InMemoryItemRepository();
        Main.seedSampleItems(repository);

        LlmClient llm = LlmClientFactory.create();
        LostFoundService service = new LostFoundService(
                repository, llm, new LlmQueryParser(llm), null);

        System.out.println("\n=== [1] 규칙만 (Rule-Only) 평가 결과 ===");
        evaluateMode(cases, service, false);

        if (service.isOnlineMode()) {
            System.out.println("\n=== [2] 하이브리드 (Hybrid: PiiMasker + LLM) 평가 결과 ===");
            evaluateMode(cases, service, true);
        } else {
            System.out.println("\n[안내] LLM_API_KEY가 없어 하이브리드 평가는 건너뜁니다.");
        }
    }

    private static void evaluateMode(List<EvalCase> cases, LostFoundService service, boolean hybrid) {
        List<List<String>> allRanked = new ArrayList<>();
        List<String> allAnswers = new ArrayList<>();

        Map<String, List<List<String>>> rankedByType = new LinkedHashMap<>();
        Map<String, List<String>> answersByType = new LinkedHashMap<>();

        for (EvalCase c : cases) {
            List<String> rankedIds = hybrid
                    ? runHybrid(service, c.query())
                    : runRuleOnly(service, c.query());

            allRanked.add(rankedIds);
            allAnswers.add(c.answerItemId());

            rankedByType.computeIfAbsent(c.type(), k -> new ArrayList<>()).add(rankedIds);
            answersByType.computeIfAbsent(c.type(), k -> new ArrayList<>()).add(c.answerItemId());
        }

        printMetrics("전체(ALL)", allRanked, allAnswers);
        for (String type : rankedByType.keySet()) {
            printMetrics("유형(" + type + ")", rankedByType.get(type), answersByType.get(type));
        }
    }

    public static List<String> runRuleOnly(LostFoundService service, String rawQuery) {
        SearchQuery q = service.parseRuleOnly(rawQuery);
        return service.searchRuleOnly(q).stream()
                .map(MatchResult::item)
                .map(Item::getId)
                .toList();
    }

    public static List<String> runHybrid(LostFoundService service, String rawQuery) {
        LostFoundService.ParseOutcome outcome = service.parseNaturalQuery(rawQuery);
        return service.searchMatches(outcome.query()).stream()
                .map(MatchResult::item)
                .map(Item::getId)
                .toList();
    }

    private static void printMetrics(String label, List<List<String>> ranked, List<String> answers) {
        double top1 = Metrics.topKHitRate(ranked, answers, 1) * 100.0;
        double top3 = Metrics.topKHitRate(ranked, answers, 3) * 100.0;
        double top5 = Metrics.topKHitRate(ranked, answers, 5) * 100.0;
        double mrr = Metrics.meanReciprocalRank(ranked, answers);
        System.out.printf("  %-16s | Top-1: %5.1f%% | Top-3: %5.1f%% | Top-5: %5.1f%% | MRR: %.3f%n",
                label, top1, top3, top5, mrr);
    }

    /** 형식: id|type|query|answerItemId, '#'으로 시작하는 줄은 주석. */
    static List<EvalCase> load(Path path) throws IOException {
        List<EvalCase> list = new ArrayList<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] f = line.split("\\|", -1);
            if (f.length != 4) {
                System.err.println("[평가] 형식 오류로 건너뜀: " + line);
                continue;
            }
            list.add(new EvalCase(f[0].trim(), f[1].trim(), f[2].trim(), f[3].trim()));
        }
        return list;
    }
}
