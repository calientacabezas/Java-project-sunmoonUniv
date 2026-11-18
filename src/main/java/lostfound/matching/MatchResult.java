package lostfound.matching;

import lostfound.domain.Item;
import java.util.List;

/** 검색 결과 1건. finalScore = ruleScore × 0.8 + semanticScore × 0.2 (재정렬 실패 시 ruleScore). */
public record MatchResult(Item item, int ruleScore, int semanticScore, int finalScore,
                          List<ScoreDetail> details, String reason) {}
