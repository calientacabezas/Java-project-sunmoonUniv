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
  ├─▶ repository (ItemRepository: MySQL / TextFile / InMemory) ──▶ MySQL 또는 data/items.txt
  └─▶ llm        (LlmClient: Fake / 실제 / Caching) ◀── privacy (PiiMasker)
```

- 위에서 아래로만 호출합니다. 저장 방식이나 LLM 제공사를 바꿔도 매칭 코드는 그대로입니다.
- LLM은 규칙 엔진을 **대체하지 않고 앞(추출)과 뒤(재정렬)에 붙는 보조 계층**입니다.

## 2. 검색 파이프라인 (11/24 구현 기준)

```
[자유 입력]
  → ① PiiMasker              개인정보 [MASKED]
  → ② LlmQueryParser         JSON 추출, enum 밖 값 → UNKNOWN(모름)
                              (실패·오프라인 → 키워드 대체 파서)
  → [확인 화면]              사용자가 추출 결과 수정 (SwingWorker로 화면 멈춤 없음)
  → ③ 상태 필터              반환 완료·기한 만료·폐기 물품 제외 (Item.isMatchable)
  → ④ 규칙 점수              ScoringRule 5개, 모름 항목은 분모 제외, 100점 환산
  → ⑤ 상위 10개 재정렬       LlmSemanticReRanker, API 1회 (실패 → 규칙 점수만)
  → ⑥ 최종 점수              규칙 × 0.8 + 의미 × 0.2, 내림차순
                              동점은 최근 습득일 → ID 순 (MatchResult.RANKING)
```

**의미 유사도를 ScoringRule로 만들지 않는 이유:** 규칙은 물건마다 호출되므로 1,000건이면 API 1,000번입니다. 규칙으로 후보를 줄이고 상위 10개만 한 번에 보내 비용과 시간을 줄입니다.

**LLM에 보내는 정보:** 마스킹된 검색 문장과 후보의 공개 정보(종류·색상·위치·이름·공개 특징)만 보냅니다. 비공개 특징(secret)은 보내지 않습니다.

## 3. 점수 규칙 (확정, `LostFoundService.defaultScoringRules()`)

| 항목 | 만점 | 부분 점수 | 비고 |
|---|---|---|---|
| 종류 | 40 | 없음 | 다르면 0점 (결과에는 남음) |
| 색상 | 25 | 같은 계열 10 | 무채색·따뜻한 색·차가운 색·갈색 계열 |
| 건물 | 20 | 인접 건물 10 | 13개 건물 인접 관계표 (`Building.isAdjacent`) |
| 층 | 5 | 없음 | 지하는 음수 |
| 세부 특징 | 10 | 키워드 1개당 5 | 종류별 `detailScore()` (다형성) |

- "모름"인 항목은 분모에서 뺍니다: 최종 규칙 점수 = 획득 점수 ÷ 비교 가능한 항목의 만점 합 × 100.
- **날짜 점수는 이번 버전에서 제외**하고 향후 과제로 둡니다. 날짜 입력은 받지만 점수에는 반영하지 않습니다.
- 초기 설계(색상 20 / 날짜 15 / 위치 15)에서 바뀐 이유: 교내 분실물은 대부분 며칠 안에 신고되어 날짜의 변별력이 낮고, 건물·층을 나누어야 점수 근거를 더 구체적으로 보여 줄 수 있기 때문입니다.

## 4. 적용한 설계 원칙

| 설계 | 적용 위치 | 효과 |
|---|---|---|
| 상속·다형성 | `Item` → 하위 클래스 4개의 `detailScore()` | 종류별 세부 비교 |
| 전략 패턴 | `ScoringRule`, `QueryParser`, `LlmClient` 구현체 | 규칙·LLM 교체 시 기존 코드 수정 없음 |
| 저장소 패턴 | `ItemRepository` (MySQL·파일·메모리) | 저장 방식을 바꿔도 서비스·화면 코드는 그대로, DB 장애 시 파일로 자동 전환 |
| 대체 경로 | 키 없음·장애 → Fake·선택형 폼·규칙 점수 | API 장애에도 Must 기능 동작 |

## 5. 비기능 요구사항

- 정확도: 평가 30건 중 하이브리드 상위 3개 포함률 80% 이상 목표
- 성능: 규칙 결과 1초 이내, 의미 보정 포함 결과 5초 이내. LLM 호출 중에도 화면이 멈추지 않음 (SwingWorker, 처리 중 버튼 비활성화)
- 개인정보: 공개 결과와 LLM 요청에 연락처·이름·카드번호·비공개 특징 없음. 비공개 특징은 관리자 승인 확인 창에서만 표시
- 유지보수: 규칙 추가 시 기존 규칙·화면 코드 수정 없음
- 비밀 정보: API 키는 환경변수 `LLM_API_KEY`로만 읽음

## 6. 습득물 사진

- 등록 화면에서 사진 파일을 클릭으로 고르거나 끌어다 놓으면 미리보기가 나오고, 등록 시 `data/photos/물품ID.확장자`로 복사됩니다 (`PhotoStorage`).
- JPG·PNG·GIF·BMP, 10MB 이하만 허용합니다. 아이폰 기본 형식(HEIC)은 Java가 읽지 못하므로 JPG로 보내 달라고 안내합니다.
- 결과 목록에는 축소 사진, 상세 영역에는 큰 사진이 보이고, 사진이 없으면 종류별 색 아이콘으로 대신합니다.
- 사진은 `data/` 아래라 GitHub에 올라가지 않습니다. 사진은 LLM에 보내지 않습니다 (학생증 등 개인정보가 찍힐 수 있음).

## 7. 샘플 데이터 형식 (`data/sample/found_items.txt`, `SampleDataLoader`)

```
# id|type|color|foundDate|building|floor|title|attr1|attr2|publicDetail|secret[|photo]
ITEM-001|ELECTRONICS|WHITE|2026-11-22|ENGINEERING|3|갤럭시 버즈2 프로|삼성|버즈2 프로|투명 하드케이스 장착|케이스 안쪽 뚜껑에 강아지 스티커
```

| type | attr1 | attr2 |
|---|---|---|
| ELECTRONICS | 브랜드 | 모델명 |
| WALLET_CARD | 지갑 형태 | 신분증 포함 여부 (Y/N) |
| BAG_CLOTHING | 세부 종류 | 브랜드·소재 |
| GENERAL | 품목 구분 | (비움) |

마지막 `photo` 열은 선택 항목(사진 파일 경로)입니다. UTF-8, 구분자 `|`. 형식이 틀린 줄은 경고만 남기고 건너뜁니다. 파일이 없으면 기본 샘플 4건으로 실행합니다.

## 8. 저장소 (MySQL + 텍스트 파일)

- 기본은 MySQL(JDBC). 환경변수 `LF_DB_URL`·`LF_DB_USER`·`LF_DB_PASSWORD`가 있고 연결되면 사용합니다.
- 연결할 수 없으면 `data/items.txt` 텍스트 파일 저장소로 자동 전환합니다. 임시 파일에 쓴 뒤 바꿔치기해 저장 중 꺼져도 파일이 깨지지 않습니다.
- 등록·확인 중·반환 처리 때마다 저장되므로 재실행해도 데이터가 유지됩니다.
- 설치·설정 방법: [DATABASE.md](DATABASE.md), 스키마: [db/schema.sql](db/schema.sql)
