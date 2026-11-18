package lostfound.matching;

/** 규칙 하나의 결과. comparable=false면 "모름"이라 분모에서 제외. */
public record ScoreDetail(String ruleName, int earned, int max, boolean comparable,
                          String reason) {}

