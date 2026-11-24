package lostfound.matching;

import java.util.List;

/** 규칙 점수 상위 K개만 LLM으로 한 번에 재평가. 실패 시 입력 목록을 그대로 반환. */
public interface SemanticReRanker {
    List<MatchResult> rerank(String maskedRawInput, List<MatchResult> topK);
}

