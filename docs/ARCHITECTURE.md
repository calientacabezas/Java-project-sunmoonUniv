# 아키텍처

## 1. 계층과 호출 방향

```
ui (Swing)
  │  화면은 서비스만 호출
  ▼
service (ItemService, MatchingService, ClaimService)
  │
  ├─▶ parsing   (QueryParser: Keyword / LLM, ExtractionValidator)
  ├─▶ matching  (ScoringRule ×5, SemanticReRanker, HybridScorer)
  ├─▶ domain    (Item 상속 구조, enum, 상태)
  │
  ├─▶ repository (ItemRepository: InMemory / TextFile) ──▶ data/*.txt
  └─▶ llm        (LlmClient: Fake / 실제 / Caching) ◀── privacy (PiiMasker)
```

- 위에서 아래로만 호출합니다. 저장 방식이나 LLM 제공사를 바꿔도 매칭 코드는 그대로입니다.
- LLM은 규칙 엔진을 **대체하지 않고 앞(추출)과 뒤(재정렬)에 붙는 보조 계층**입니다.

## 2. 검색 파이프라인

```
[자유 입력]
  → ① PiiMasker            개인정보 [MASKED]
  → ② LlmQueryParser       JSON 추출 (실패 → 선택형 폼)
  → ③ ExtractionValidator  enum 밖 값 → UNKNOWN(모름)
  → [확인 화면]            사용자가 추출 결과 수정
  → ④ 하드 필터            종류 불일치·반환 완료·습득일이 분실일보다 2일 이상 앞섬 → 제외
  → ⑤ 규칙 점수            ScoringRule 5개, 100점 만점
  → ⑥ 상위 10개 재정렬     SemanticReRanker, API 1회 (실패 → 규칙 점수만)
  → ⑦ 최종 점수            규칙 × 0.8 + 의미 × 0.2, 내림차순, 동점은 최근 습득일
```

**의미 유사도를 ScoringRule로 만들지 않는 이유:** 규칙은 물건마다 호출되므로 1,000건이면 API 1,000번입니다. 규칙으로 후보를 줄이고 상위 10개만 한 번에 보내 비용과 시간을 줄입니다.

## 3. 점수 규칙 (초기값, 실험으로 조정)

| 항목 | 만점 | 부분 점수 | 제외 조건 |
|---|---|---|---|
| 종류 | 40 | 없음 | 다르면 결과에서 제외 |
| 색상 | 20 | 같은 계열 10 | |
| 날짜 | 15 | 하루 차이 10, 3일 이내 5 (습득일이 늦으면 7일 이내 부분 점수) | 습득일이 분실일보다 2일 이상 앞섬 |
| 위치 | 15 | 같은 건물 다른 층 10, 인접 건물 5 | |
| 세부 특징 | 10 | 종류별 `detailScore()` | |

"모름"인 항목은 분모에서 뺍니다: 최종 규칙 점수 = 획득 점수 ÷ 비교 가능한 항목의 만점 합 × 100. 표시 기준 50점 이상.

## 4. 적용한 설계 원칙

| 설계 | 적용 위치 | 효과 |
|---|---|---|
| 상속·다형성 | `Item` → 하위 클래스 4개의 `detailScore()` | 종류별 세부 비교 |
| 전략 패턴 | `ScoringRule`, `QueryParser`, `LlmClient` 구현체 | 규칙·LLM 교체 시 기존 코드 수정 없음 |
| 저장소 패턴 | `ItemRepository` | 파일 없이 테스트, 나중에 DB 교체 가능 |
| 데코레이터 | `CachingLlmClient` | 같은 입력 재호출 방지, 시연용 오프라인 캐시 |
| 대체 경로 | 키 없음·장애 → Fake·선택형 폼·규칙 점수 | API 장애에도 Must 기능 동작 |

## 5. 비기능 요구사항

- 정확도: 평가 30건 중 하이브리드 상위 3개 포함률 80% 이상 목표
- 성능: 규칙 결과 1초 이내, 의미 보정 결과 5초 이내 (규칙 결과를 먼저 표시 후 갱신)
- 개인정보: 공개 결과와 LLM 요청에 연락처·이름·카드번호·비공개 특징 없음
- 유지보수: 규칙 추가 시 기존 규칙·화면 코드 수정 없음
- 비밀 정보: API 키는 환경변수 `LLM_API_KEY`로만 읽음

## 6. 파일 저장 형식

```
# version=1
# id|type|category|color|date|building|floor|place|storage|status|detail|secret
F0001|ELECTRONICS|EARPHONE|WHITE|2026-11-05|ENGINEERING|3|강의실|학과사무실|STORED|brand=apple|keyring
```

UTF-8, 구분자 `|`는 이스케이프, `.tmp`에 쓴 뒤 `Files.move`로 교체, 손상된 줄은 건너뛰고 로그.
