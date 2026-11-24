package lostfound.matching;

import lostfound.domain.Item;
import java.util.Comparator;
import java.util.List;

/** 검색 결과 1건. finalScore = ruleScore × 0.8 + semanticScore × 0.2 (재정렬 실패 시 ruleScore). */
public record MatchResult(Item item, int ruleScore, int semanticScore, int finalScore,
                          List<ScoreDetail> details, String reason) {

    /** 결과 정렬 기준: 최종 점수 내림차순, 동점이면 최근 습득일 우선, 그래도 같으면 ID 순. */
    public static final Comparator<MatchResult> RANKING =
            Comparator.comparingInt(MatchResult::finalScore).reversed()
                    .thenComparing((MatchResult r) -> r.item().getFoundDate(),
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(r -> r.item().getId());
}
