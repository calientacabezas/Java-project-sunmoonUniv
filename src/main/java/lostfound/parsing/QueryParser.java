package lostfound.parsing;

import lostfound.matching.SearchQuery;

/**
 * 자연어 → SearchQuery. 구현체:
 *  - KeywordQueryParser: 사전 단어 일치만 사용 (정확도 실험의 "규칙만" 기준선)
 *  - LlmQueryParser: LLM 추출 + ExtractionValidator (담당: 김동민)
 */
public interface QueryParser {
    SearchQuery parse(String rawInput);
}

