package lostfound.eval;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 규칙만 vs 하이브리드 비교 실험 실행기. 실행: ./gradlew evaluate
 *  - 규칙만:   KeywordQueryParser → 규칙 점수 → 정렬
 *  - 하이브리드: PiiMasker → LlmQueryParser → 규칙 점수 → 상위 10개 SemanticReRanker
 * 두 방식 모두 같은 습득물 데이터와 같은 30개 검색어를 사용한다.
 * TODO(11/25): MatchingService가 완성되면 runRuleOnly / runHybrid를 연결한다.
 */
public class AccuracyEvaluator {
    static final Path CASES = Path.of("data", "eval", "eval_cases.txt");

    public static void main(String[] args) throws IOException {
        List<EvalCase> cases = load(CASES);
        System.out.printf("평가 케이스 %d건 로드%n", cases.size());
        cases.stream()
             .collect(java.util.stream.Collectors.groupingBy(EvalCase::type,
                      java.util.TreeMap::new, java.util.stream.Collectors.counting()))
             .forEach((t, n) -> System.out.printf("  %-8s %d건%n", t, n));
        System.out.println("매칭 엔진 연결 전입니다. (TODO: runRuleOnly / runHybrid)");
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

