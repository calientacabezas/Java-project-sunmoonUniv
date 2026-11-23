package lostfound.eval;

/**
 * 정확도 실험 케이스 1건.
 * type: NORMAL(표준 표현) / SYNONYM(동의어·유사 색상) / TYPO(오타) / FOREIGN(영어 등 외국어)
 * answerItemId: 이 검색어의 주인 물건 ID (data/sample의 습득물 ID)
 */
public record EvalCase(String id, String type, String query, String answerItemId) {}

