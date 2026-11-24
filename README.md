# 교내 분실물 회수 지원 시스템

LLM API와 하이브리드 유사도 알고리즘으로 습득물과 분실자를 자동 매칭하고,
소유자 확인과 관리자 승인을 거쳐 "찾았다"가 아니라 "돌려받았다"에서 끝나는 Java Swing 데스크톱 프로그램입니다.

선문대학교 SW중심대학 기업연계 프로젝트 · JAVA응용 프로젝트 · 담당 교수 이택

## 주요 기능

| 구분 | 기능 |
|---|---|
| 등록 | 종류·색상·건물·층·날짜를 목록에서 선택해 표준화 저장, **사진 첨부**(클릭 또는 끌어다 놓기) |
| 저장 | **MySQL(JDBC)** 저장, 연결할 수 없으면 텍스트 파일로 자동 전환. 재실행해도 데이터 유지 |
| 자연어 검색 | "어제 공대 3층에서 하얀 버즈 잃어버렸어요" → LLM이 검색 조건으로 변환 → 사용자 확인 |
| 하이브리드 매칭 | 규칙 점수(100점) 상위 10개를 LLM이 의미 기준으로 재평가, 최종 = 규칙 × 0.8 + 의미 × 0.2 |
| 점수 근거 | 결과마다 "종류 일치 +40, 같은 계열 색상 +10" 등 추천 이유 표시 |
| 개인정보 보호 | 외부 API 전송 전 전화번호·학번·카드번호·이메일·이름을 `[MASKED]` 처리 |
| 회수 승인 | 비공개 특징은 관리자 확인 창에서만 표시, 승인 시 반환 완료·검색 제외, 불일치 시 보관 중으로 복귀 |
| 화면 | 카드형 디자인, 사진 미리보기, 점수 배지·규칙별 점수 막대 (FlatLaf 테마) |
| 장애 대응 | API 키 없음·네트워크 장애 시 선택형 입력과 규칙 점수만으로 계속 동작 |

## 처음 받았을 때 (이클립스 기준)

자세한 화면별 설명과 문제 해결은 **[docs/ECLIPSE_SETUP.md](docs/ECLIPSE_SETUP.md)**에 있습니다.

1. **JDK 21**과 **Eclipse IDE for Java Developers**(2023-12 이상)를 설치합니다.
2. Eclipse: Window → Preferences → General → Workspace → Text file encoding을 **UTF-8**로 바꿉니다.
3. 저장소를 받습니다.
   ```bash
   git clone https://github.com/calientacabezas/Java-project-sunmoonUniv.git
   ```
4. File → Import → Gradle → **Existing Gradle Project** → 받은 폴더 선택 → Finish.
5. `Main.java` 우클릭 → Run As → Java Application으로 빈 창이 뜨면 완료입니다.

명령줄에서도 같은 빌드를 쓸 수 있습니다(Gradle 설치 불필요).

```bash
gradlew.bat build      # macOS: ./gradlew build
gradlew.bat run
gradlew.bat evaluate   # 정확도 비교 실험
```

## LLM API 키 설정 (LLM 담당자·통합 테스트하는 사람만)

키가 없으면 자동으로 **오프라인 모드**(FakeLlmClient)로 실행되므로 대부분의 개발에는 필요 없습니다.

```bash
# Windows (명령 프롬프트, 1회) → Eclipse와 터미널을 완전히 껐다 켜기
setx LLM_API_KEY "발급받은키"

# macOS (~/.zshrc 맨 아래) → source ~/.zshrc 후 Eclipse 재시작
export LLM_API_KEY="발급받은키"
```

키는 코드·커밋·이슈·단톡방에 절대 남기지 않습니다. 노출되면 즉시 폐기하고 재발급합니다.

## 폴더 구조

```
src/main/java/lostfound/
├─ app/         Main, 객체 조립
├─ ui/          Swing 화면 (화면 → 서비스만 호출)
├─ service/     업무 흐름 (등록, 매칭, 회수 승인)
├─ domain/      Item 상속 구조, enum, 상태
├─ matching/    ScoringRule 규칙, SemanticReRanker, HybridScorer, MatchResult
├─ parsing/     QueryParser (Keyword / LLM), ExtractionValidator
├─ llm/         LlmClient, FakeLlmClient, 실제 클라이언트, 캐시
├─ privacy/     PiiMasker (외부 전송 전 개인정보 마스킹)
├─ repository/  ItemRepository (InMemory / TextFile)
├─ eval/        정확도 비교 실험 (상위 3개 포함률)
└─ exception/   사용자 정의 예외
src/main/resources/prompts/   LLM 프롬프트 (버전별 파일)
data/sample/   가상 샘플 습득물     data/eval/   평가 케이스와 정답
docs/          일정, 설계, 실험, 기여 규칙
```

## 팀

| 이름 | 역할 |
|---|---|
| 강채연 | 진행 총괄, enum·인터페이스, 점수 규칙, 회수 승인, 실험 코드 |
| 김경린 | 빌드·CI, Item 상속 구조, Swing 화면, 발표 자료·시연 영상 |
| 김동민 | 저장소 관리, LLM 계층, 개인정보 마스킹, 파일 저장소, 배포 |
| 권도영 | 건물·색상·동의어 표, 샘플·평가 데이터, 실험 실행, 수동 테스트 |

## 문서

- [이클립스 환경 설정](docs/ECLIPSE_SETUP.md)
- [MySQL 저장소 설정](docs/DATABASE.md)
- [기여 규칙 (브랜치·커밋·PR)](CONTRIBUTING.md)
- [일정 (11/29 제출, PPT·시연 영상 포함)](docs/SCHEDULE.md)
- [아키텍처와 LLM 파이프라인](docs/ARCHITECTURE.md)
- [정확도 비교 실험](docs/EXPERIMENT.md)
- [개인정보 마스킹 규칙](docs/PRIVACY.md)
- [건물·색상·동의어 표](docs/DATA_TABLES.md)
