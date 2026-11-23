package lostfound.eval;

import java.util.List;

/** 평가 지표 계산. 담당: 강채연(코드), 권도영(실행·기록). */
public final class Metrics {
    private Metrics() {}

    /** 정답이 상위 k개 안에 있는 케이스의 비율 (0.0~1.0). */
    public static double topKHitRate(List<List<String>> rankedIdsPerCase, List<String> answers, int k) {
        if (rankedIdsPerCase.size() != answers.size())
            throw new IllegalArgumentException("결과 수와 정답 수가 다릅니다.");
        if (answers.isEmpty()) return 0.0;
        int hit = 0;
        for (int i = 0; i < answers.size(); i++) {
            List<String> ranked = rankedIdsPerCase.get(i);
            List<String> top = ranked.subList(0, Math.min(k, ranked.size()));
            if (top.contains(answers.get(i))) hit++;
        }
        return (double) hit / answers.size();
    }

    /** 평균 역순위(MRR): 정답이 1위면 1, 2위면 0.5, 없으면 0. 보조 지표. */
    public static double meanReciprocalRank(List<List<String>> rankedIdsPerCase, List<String> answers) {
        if (answers.isEmpty()) return 0.0;
        double sum = 0;
        for (int i = 0; i < answers.size(); i++) {
            int rank = rankedIdsPerCase.get(i).indexOf(answers.get(i));
            if (rank >= 0) sum += 1.0 / (rank + 1);
        }
        return sum / answers.size();
    }
}