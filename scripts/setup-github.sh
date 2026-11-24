#!/usr/bin/env bash
# 라벨·마일스톤·초기 이슈를 한 번에 만든다. 저장소 폴더 안에서 한 명만 1회 실행.
# 준비: GitHub CLI 설치 후 `gh auth login` (저장소 쓰기 권한이 있는 협업자면 누구나 가능)
set -euo pipefail

echo "== 라벨 생성 =="
label() { gh label create "$1" --color "$2" --description "$3" --force >/dev/null && echo "  $1"; }
label "Must"       "d73a4a" "제출 기준선 기능"
label "Should"     "fbca04" "여유가 있을 때만"
label "llm"        "7057ff" "LLM 계층·프롬프트"
label "privacy"    "b60205" "개인정보 마스킹"
label "experiment" "0e8a16" "정확도 비교 실험"
label "domain"     "1d76db" "Item·enum·상태"
label "matching"   "0052cc" "점수 규칙·정렬·재정렬"
label "repository" "5319e7" "저장소·파일"
label "ui"         "c5def5" "Swing 화면"
label "docs"       "0075ca" "문서·발표·영상"
label "bug"        "e11d21" "버그"

echo "== 마일스톤 생성 =="
REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
ms() {
  gh api "repos/$REPO/milestones" -f title="$1" -f due_on="$2T14:59:59Z" -f description="$3" >/dev/null 2>&1 \
    && echo "  $1" || echo "  $1 (이미 있음)"
}
ms "W1 착수"          "2026-11-14" "인터페이스·샘플 데이터·빈 화면"
ms "W2 핵심 구현"     "2026-11-20" "저장·규칙·LLM 추출·마스킹·화면"
ms "W3 통합·동결"     "2026-11-24" "재정렬·회수 흐름, 11/24 코드 동결"
ms "W4 테스트·발표"   "2026-11-29" "실험·버그 수정·영상·PPT·제출"

echo "== 이슈 생성 =="
issue() { gh issue create --title "$1" --label "$2" --milestone "$3" --body "$4" >/dev/null && echo "  $1"; }
issue "[화면] 스타터 묶음 커밋 및 CI 통과 확인" "Must,docs" "W1 착수" "README의 '처음 받았을 때' 절차대로 ./gradlew build 성공 확인"
issue "[화면] Item 추상 클래스와 하위 클래스 4개" "Must,domain" "W1 착수" "ElectronicsItem, WalletCardItem, BagClothingItem, GeneralItem. detailScore() 재정의 + 단위 테스트"
issue "[화면] 빈 메인 창과 화면 3개 와이어프레임" "Must,ui" "W1 착수" "입력 / 확인 / 결과 / 관리자 흐름. 와이어프레임 사진을 이슈에 첨부"
issue "[화면] InMemoryItemRepository" "Must,repository" "W1 착수" "화면 개발·테스트용 메모리 저장소"
issue "[진행·매칭] enum 값 확정 (건물·색상 표 반영)" "Must,domain" "W1 착수" "데이터 담당의 표를 받아 Building, ItemColor 값과 인접·계열 정보 반영"
issue "[진행·매칭] 인터페이스 확정 PR" "Must,matching" "W1 착수" "ScoringRule, SearchQuery, MatchResult, QueryParser, SemanticReRanker, ItemRepository 계약 확정"
issue "[데이터] 건물 코드·인접 건물 표" "Must,docs" "W1 착수" "docs/DATA_TABLES.md에 작성. 인접 기준: 도보 약 3분 이내"
issue "[데이터] 색상 계열·동의어 표" "Must,docs" "W1 착수" "예: 연분홍→PINK, 하얀→WHITE, 버즈→무선 이어폰"
issue "[데이터] 샘플 습득물 30건 + 평가 케이스 30건" "Must,experiment" "W1 착수" "data/sample/found_items.txt, data/eval/eval_cases.txt. NORMAL 10 / SYNONYM 10 / TYPO 5 / FOREIGN 5"
issue "[LLM] LlmClient 실제 구현과 호출 1회 성공" "Must,llm" "W2 핵심 구현" "11/17 마감. 타임아웃 10초, 실패 시 LlmException"
issue "[LLM] PiiMasker 검토 및 LLM 호출 전 적용" "Must,privacy" "W2 핵심 구현" "모든 LlmClient 호출 직전에 mask() 적용, 호출 로그에는 마스킹 개수만 기록"
issue "[LLM] LlmQueryParser + ExtractionValidator" "Must,llm" "W2 핵심 구현" "enum 밖 값은 UNKNOWN, 실패 시 선택형 폼으로 대체"
issue "[LLM] TextFileItemRepository" "Must,repository" "W2 핵심 구현" ".tmp 저장 후 Files.move 교체, 손상된 줄 건너뛰기"
issue "[진행·매칭] 점수 규칙 5개와 정렬" "Must,matching" "W2 핵심 구현" "종류40 색상20 날짜15 위치15 세부10, 모름은 분모 제외, 동점은 최근 습득일 우선"
issue "[진행·매칭] KeywordQueryParser (실험 기준선)" "Must,experiment" "W2 핵심 구현" "사전 단어 일치만으로 SearchQuery 생성"
issue "[화면] 검색·확인·결과 화면 구현" "Must,ui" "W2 핵심 구현" "확인 화면에서 추출 결과 수정 가능, 결과에 점수 근거 표시"
issue "[LLM] SemanticReRanker + HybridScorer 연결" "Must,llm" "W3 통합·동결" "상위 10개를 한 번에 호출, 최종 = 규칙×0.8 + 의미×0.2"
issue "[진행·매칭] ClaimService 회수 요청·승인" "Must,domain" "W3 통합·동결" "보관 중→확인 중→반환 완료, 거절 시 보관 중"
issue "[진행·매칭] AccuracyEvaluator 연결" "Must,experiment" "W3 통합·동결" "runRuleOnly / runHybrid 구현, 결과를 docs/EXPERIMENT.md 표 형식으로 출력"
issue "[데이터] 정확도 실험 실행과 결과 기록" "Must,experiment" "W4 테스트·발표" "11/25 1차, 11/26 최종. 유형별 상위 3개 포함률 표"
issue "[화면] 발표 PPT" "Must,docs" "W4 테스트·발표" "docs/SCHEDULE.md의 슬라이드 구성 참고"
issue "[화면] 시연 영상" "Must,docs" "W4 테스트·발표" "docs/SCHEDULE.md의 촬영 시나리오 참고"
echo "완료"
