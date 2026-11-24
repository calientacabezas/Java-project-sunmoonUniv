package lostfound.matching;

import lostfound.domain.Item;

/** 점수 규칙(전략 패턴). 규칙 추가 시 기존 코드 수정 없이 구현체만 추가. */
public interface ScoringRule {
    ScoreDetail evaluate(Item item, SearchQuery query);
}

