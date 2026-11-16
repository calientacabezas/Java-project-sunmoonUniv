# 기여 규칙

규칙은 충돌과 막판 통합을 줄이기 위한 최소한입니다. 모두 지켜 주세요.

## 1. 매일 작업 흐름

이클립스 화면으로 하는 방법은 [docs/ECLIPSE_SETUP.md](docs/ECLIPSE_SETUP.md) 5장을 보세요. 아래는 명령어 방식입니다.

```bash
git checkout main
git pull origin main                      # 작업 시작 전 항상 최신화
git checkout -b feature/12-llm-client     # 이슈 번호-짧은 설명
# ... 작업, 테스트 ...
./gradlew test       # Windows: gradlew.bat test  (또는 Eclipse에서 JUnit 실행)
git add .
git commit -m "feat: LlmClient 타임아웃 처리 추가 (#12)"
git push -u origin feature/12-llm-client
# GitHub에서 PR 생성 → 리뷰 1명 승인 → Squash and merge → 브랜치 삭제
```

## 2. 브랜치

| 접두어 | 용도 | 예시 |
|---|---|---|
| `feature/` | 새 기능 | `feature/14-scoring-rules` |
| `fix/` | 버그 수정 | `fix/31-empty-result-crash` |
| `docs/` | 문서·발표 자료 | `docs/40-experiment-result` |
| `test/` | 테스트만 추가 | `test/22-masker-cases` |

- `main`에 직접 푸시하지 않습니다. `main`은 항상 실행 가능한 상태여야 합니다.
- 브랜치는 하루~이틀 안에 PR로 끝냅니다. 오래 살아 있는 브랜치가 충돌의 원인입니다.

## 3. 커밋 메시지

`<종류>: <무엇을 했는지> (#이슈번호)`

| 종류 | 의미 |
|---|---|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `test` | 테스트 추가·수정 |
| `refactor` | 동작 변화 없는 구조 개선 |
| `docs` | 문서 |
| `chore` | 빌드·설정 |

예: `feat: 색상 계열 부분 점수 규칙 추가 (#14)`

## 4. 이슈와 PR

- **이슈 1개 = PR 1개.** PR은 300줄 이내로 작게 올립니다.
- PR 본문의 `closes #번호`로 이슈를 연결하면 병합 시 자동으로 닫힙니다.
- 리뷰 짝: 채연 ↔ 경린, 동민 ↔ 도영 (동민 결근 중에는 도영 ↔ 채연). 리뷰는 요청받은 날 안에 합니다.
- 리뷰 기준: 테스트 통과 / 계층 규칙 준수 / 비밀 정보 없음 / 이름이 의미를 설명하는지.
- 병합은 **Squash and merge**를 사용합니다.

## 5. 코드 규칙

- 호출 방향: `ui → service → domain·matching·parsing → repository·llm`. 화면에서 파일이나 LLM을 직접 부르지 않습니다.
- 서비스는 구현체가 아니라 **인터페이스**(`ItemRepository`, `LlmClient`, `QueryParser`)에만 의존합니다.
- LLM으로 보내는 모든 문자열은 `PiiMasker.mask()`를 먼저 거칩니다. `secret`(비공개 특징)은 LLM에 보내지 않습니다.
- LLM 호출은 Swing 이벤트 스레드에서 하지 않고 `SwingWorker`에서 합니다.
- 점수 배점과 가중치는 상수로 분리합니다. 숫자를 코드 중간에 쓰지 않습니다.
- 들여쓰기 공백 4칸, 인코딩 UTF-8. 클래스 `PascalCase`, 메서드·변수 `camelCase`, 상수 `UPPER_SNAKE_CASE`.
- 테스트 메서드는 한국어 이름을 허용합니다: `void 같은_계열_색상이면_10점()`.

## 6. 절대 커밋하지 않는 것

- API 키, 토큰, 비밀번호 (`.env`, `*.key`)
- 실제 사람의 이름·연락처·학번이 들어간 데이터
- `data/`의 실제 운영 데이터 (가상 데이터인 `data/sample`, `data/eval`만 커밋)
- 발표 원본(pptx)·영상(mp4): 팀 드라이브에 보관

실수로 올렸다면 **바로 팀에 알리고 키를 폐기**합니다. 커밋을 지워도 기록에는 남습니다.

## 7. 충돌이 났을 때

```bash
git checkout feature/내-브랜치
git pull origin main          # 충돌 파일 표시됨
# 파일에서 <<<<<<< ======= >>>>>>> 구간을 정리
./gradlew test
git add . && git commit -m "chore: main 병합 충돌 해결"
git push
```

혼자 해결하기 어려우면 해당 파일 담당자를 불러 같이 봅니다.

## 8. 진행 공유

- 매일 밤 10분 체크인: 어제 한 일 / 오늘 할 일 / 막힌 것.
- 이슈는 라벨(`Must`, `llm` 등)과 마일스톤(W1~W4)으로 관리합니다.
- 하루 이상 막히면 바로 강채연(진행 총괄)에게 알려 작업을 다시 나눕니다.
