package lostfound.eval;

import lostfound.domain.Item;
import lostfound.llm.LlmClient;
import lostfound.llm.LlmClientFactory;
import lostfound.matching.LlmSemanticReRanker;
import lostfound.matching.MatchResult;
import lostfound.matching.SearchQuery;
import lostfound.parsing.KeywordQueryParser;
import lostfound.parsing.LlmQueryParser;
import lostfound.repository.InMemoryItemRepository;
import lostfound.repository.SampleDataLoader;
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
 * 규칙만 vs 하이브리드 비교 실험 실행기. 실행: ./gradlew evaluate (이클립스: Gradle Tasks → verification → evaluate)
 * <ul>
 *   <li>규칙만:    KeywordQueryParser(표준 이름만) → 규칙 5개 점수 → 정렬</li>
 *   <li>하이브리드: PiiMasker → LlmQueryParser → 규칙 5개 점수 → 상위 10개 LlmSemanticReRanker</li>
 * </ul>
 * 두 방식은 같은 습득물(data/sample)과 같은 검색어(data/eval)를 쓴다.
 * 하이브리드에서 LLM이 실패하면 기준선 파서로 대체되므로, 실패가 많으면 두 결과가 같아진다.
 */
public class AccuracyEvaluator {
    static final Path CASES = Path.of("data", "eval", "eval_cases.txt");

    public static void main(String[] args) throws IOException {
        if (!Files.exists(CASES)) {
            System.err.println("[평가] 평가 데이터 파일이 없습니다: " + CASES.toAbsolutePath());
            return;
        }
        List<Item> items = SampleDataLoader.loadOrEmpty(SampleDataLoader.DEFAULT_PATH);
        if (items.isEmpty()) {
            System.err.println("[평가] 습득물 샘플이 없어 실험을 진행할 수 없습니다.");
            return;
        }
        List<EvalCase> cases = load(CASES);
        System.out.printf("습득물 %d건, 평가 케이스 %d건 로드%n", items.size(), cases.size());
        warnUnknownAnswers(cases, items);

        LlmClient llm = LlmClientFactory.create();
        KeywordQueryParser baseline = new KeywordQueryParser();

        // 두 방식에 같은 습득물 목록을 쓰되 서비스는 따로 만든다 (파서·재정렬기 구성만 다름).
        LostFoundService ruleService = new LostFoundService(
                new InMemoryItemRepository(items), llm, null, baseline, null,
                LostFoundService.defaultScoringRules());
        LostFoundService hybridService = new LostFoundService(
                new InMemoryItemRepository(items), llm, new LlmQueryParser(llm), baseline,
                new LlmSemanticReRanker(llm), LostFoundService.defaultScoringRules());

        List<List<String>> ruleRanked = new ArrayList<>();
        for (EvalCase c : cases) ruleRanked.add(runRuleOnly(ruleService, c.query()));

        List<List<String>> hybridRanked = null;
        if (hybridService.isOnlineMode()) {
            hybridRanked = new ArrayList<>();
            for (EvalCase c : cases) hybridRanked.add(runHybrid(hybridService, c.query()));
        } else {
            System.out.println("\n[안내] LLM_API_KEY가 없어 하이브리드 평가는 건너뜁니다. (규칙만 결과만 출력)");
        }

        printCaseTable(cases, ruleRanked, hybridRanked);
        printSummary(cases, ruleRanked, hybridRanked);
    }

    public static List<String> runRuleOnly(LostFoundService service, String rawQuery) {
        SearchQuery q = service.parseRuleOnly(rawQuery);
        return ids(service.searchRuleOnly(q));
    }

    public static List<String> runHybrid(LostFoundService service, String rawQuery) {
        LostFoundService.ParseOutcome outcome = service.parseNaturalQuery(rawQuery);
        return ids(service.searchMatches(outcome.query()));
    }

    private static List<String> ids(List<MatchResult> results) {
        return results.stream().map(MatchResult::item).map(Item::getId).toList();
    }

    /** 케이스별 정답 순위 (분석·발표용 사례 찾기). 10위 밖이면 '-'. */
    private static void printCaseTable(List<EvalCase> cases, List<List<String>> rule, List<List<String>> hybrid) {
        System.out.println("\n=== 케이스별 정답 순위 ===");
        System.out.println("ID   | 유형     | 규칙만 | 하이브리드 | 검색어");
        for (int i = 0; i < cases.size(); i++) {
            EvalCase c = cases.get(i);
            System.out.printf("%-4s | %-8s | %6s | %10s | %s%n",
                    c.id(), c.type(),
                    rank(rule.get(i), c.answerItemId()),
                    hybrid == null ? "건너뜀" : rank(hybrid.get(i), c.answerItemId()),
                    c.query());
        }
    }

    private static String rank(List<String> ranked, String answer) {
        int idx = ranked.indexOf(answer);
        return idx < 0 ? "-" : (idx + 1) + "위";
    }

    /** docs/EXPERIMENT.md 6장 표에 그대로 붙여 넣을 수 있는 마크다운 표. */
    private static void printSummary(List<EvalCase> cases, List<List<String>> rule, List<List<String>> hybrid) {
        Map<String, List<Integer>> byType = new LinkedHashMap<>();
        for (String t : List.of("NORMAL", "SYNONYM", "TYPO", "FOREIGN")) byType.put(t, new ArrayList<>());
        for (int i = 0; i < cases.size(); i++) {
            byType.computeIfAbsent(cases.get(i).type(), k -> new ArrayList<>()).add(i);
        }
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < cases.size(); i++) all.add(i);

        System.out.println("\n=== 상위 3개 포함률 (Top-3) ===");
        System.out.println("| 유형 | 건수 | 규칙만 Top-3 | 하이브리드 Top-3 | 차이(%p) |");
        System.out.println("|---|---|---|---|---|");
        for (Map.Entry<String, List<Integer>> e : byType.entrySet()) {
            if (!e.getValue().isEmpty()) printRow(e.getKey(), e.getValue(), cases, rule, hybrid);
        }
        printRow("**전체**", all, cases, rule, hybrid);

        System.out.println("\n| 지표 | 규칙만 | 하이브리드 |");
        System.out.println("|---|---|---|");
        System.out.printf("| Top-1 | %s | %s |%n", pct(metric(all, cases, rule, 1)), hybrid == null ? "-" : pct(metric(all, cases, hybrid, 1)));
        System.out.printf("| MRR | %.3f | %s |%n", mrr(all, cases, rule), hybrid == null ? "-" : String.format("%.3f", mrr(all, cases, hybrid)));
    }

    private static void printRow(String label, List<Integer> idx, List<EvalCase> cases,
                                 List<List<String>> rule, List<List<String>> hybrid) {
        double r = metric(idx, cases, rule, 3);
        String h = hybrid == null ? "-" : pct(metric(idx, cases, hybrid, 3));
        String diff = hybrid == null ? "-" : String.format("%+.1f", (metric(idx, cases, hybrid, 3) - r) * 100);
        System.out.printf("| %s | %d | %s | %s | %s |%n", label, idx.size(), pct(r), h, diff);
    }

    private static double metric(List<Integer> idx, List<EvalCase> cases, List<List<String>> ranked, int k) {
        List<List<String>> sub = new ArrayList<>();
        List<String> answers = new ArrayList<>();
        for (int i : idx) { sub.add(ranked.get(i)); answers.add(cases.get(i).answerItemId()); }
        return Metrics.topKHitRate(sub, answers, k);
    }

    private static double mrr(List<Integer> idx, List<EvalCase> cases, List<List<String>> ranked) {
        List<List<String>> sub = new ArrayList<>();
        List<String> answers = new ArrayList<>();
        for (int i : idx) { sub.add(ranked.get(i)); answers.add(cases.get(i).answerItemId()); }
        return Metrics.meanReciprocalRank(sub, answers);
    }

    private static String pct(double v) {
        return String.format("%.1f%%", v * 100);
    }

    private static void warnUnknownAnswers(List<EvalCase> cases, List<Item> items) {
        for (EvalCase c : cases) {
            boolean exists = items.stream().anyMatch(it -> it.getId().equals(c.answerItemId()));
            if (!exists) {
                System.err.println("[평가] 경고: " + c.id() + "의 정답 ID " + c.answerItemId() + "가 습득물 샘플에 없습니다.");
            }
        }
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
