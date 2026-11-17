# 이클립스 개발 환경 설정

이 프로젝트는 **Gradle 프로젝트**입니다. 이클립스에서는 "Existing Gradle Project"로 가져오면
라이브러리(JUnit, Gson)와 Java 21 설정이 자동으로 잡힙니다. jar를 직접 추가하지 마세요.

## 1. 설치 (팀원 전원, 1회)

| 항목 | 버전 | 확인 방법 |
|---|---|---|
| JDK | 21 (Temurin 21 권장, adoptium.net) | 명령 프롬프트에서 `java -version` |
| Eclipse | **Eclipse IDE for Java Developers** 2023-12 이상 (최신 권장) | Help → About Eclipse |
| Git | Git for Windows (git-scm.com) 또는 GitHub Desktop | `git --version` |

"for Java Developers" 패키지에는 Gradle(Buildship)과 Git(EGit) 플러그인이 기본으로 들어 있습니다.

## 2. 이클립스 기본 설정 (팀원 전원, 1회)

1. **인코딩 UTF-8:** Window → Preferences → General → Workspace
   - Text file encoding: **Other → UTF-8**
   - New text file line delimiter: **Other → Unix**
   - 이걸 안 하면 Windows에서 한글 주석과 테스트 이름이 깨집니다.
2. **JDK 21 등록:** Window → Preferences → Java → Installed JREs → Add → Standard VM
   → JDK 21 설치 폴더(예: `C:\Program Files\Eclipse Adoptium\jdk-21...`) 선택 → 체크해서 기본값으로.
3. **컴파일러 수준:** Java → Compiler → Compiler compliance level **21**.

## 3. 프로젝트 가져오기 (저장소에 뼈대가 올라간 뒤, 팀원 전원)

1. 저장소 복제: 명령 프롬프트(또는 Git Bash)에서
   ```bash
   cd C:\dev                     # 원하는 작업 폴더 (한글·공백 없는 경로 권장)
   git clone https://github.com/calientacabezas/Java-project-sunmoonUniv.git
   ```
2. Eclipse: **File → Import → Gradle → Existing Gradle Project → Next**
3. Project root directory에 복제한 폴더 선택 → **Finish**
   - 처음에는 Gradle과 라이브러리를 내려받느라 1~3분 걸립니다(오른쪽 아래 진행 표시).
4. 확인
   - `src/main/java/lostfound/app/Main.java` 우클릭 → Run As → **Java Application** → 빈 창이 뜨면 성공
   - `src/test/java` 우클릭 → Run As → **JUnit Test** → 모두 초록색이면 성공

## 4. 자주 쓰는 작업

| 하고 싶은 것 | 방법 |
|---|---|
| 프로그램 실행 | `Main.java` 우클릭 → Run As → Java Application |
| 테스트 실행 | 테스트 파일·폴더 우클릭 → Run As → JUnit Test |
| 정확도 실험 | Window → Show View → Other → Gradle → **Gradle Tasks** → `verification` → `evaluate` 더블클릭 |
| build.gradle 수정 후 반영 | 프로젝트 우클릭 → **Gradle → Refresh Gradle Project** |
| 빨간 줄이 이상하게 남을 때 | Project → Clean, 그다음 Refresh Gradle Project |

## 5. Git 작업 (이클립스 안에서)

명령어가 익숙하면 `CONTRIBUTING.md`의 명령어 방식이 가장 확실합니다. 이클립스로 하려면:

1. Window → Show View → Other → Git → **Git Staging**, **Git Repositories** 두 개를 엽니다.
2. **브랜치 만들기:** 프로젝트 우클릭 → Team → Switch To → New Branch → `feature/12-설명` → Finish
3. **커밋:** Git Staging에서 변경 파일을 Unstaged → Staged로 끌어 옮김 → 메시지 `feat: ... (#12)` → **Commit and Push**
4. **PR:** 푸시 후 GitHub 웹에서 "Compare & pull request" 버튼 → 템플릿 채우기 → 리뷰어 지정
5. **최신화:** PR이 병합되면 Team → Switch To → main → 프로젝트 우클릭 → Team → **Pull**

**로그인:** GitHub는 비밀번호 푸시를 받지 않습니다. 처음 푸시할 때 묻는 비밀번호 칸에는
GitHub → Settings → Developer settings → Personal access tokens에서 만든 **토큰**을 넣습니다
(권한: `repo`, CI 파일을 수정한다면 `workflow`도). 토큰은 어디에도 적어 두지 마세요.

## 6. LLM API 키 (LLM 담당자만)

- 방법 A: `setx LLM_API_KEY "키"` 실행 후 **Eclipse를 완전히 종료했다가 다시 실행**
- 방법 B: Run → Run Configurations → Main → **Environment** 탭 → Add → `LLM_API_KEY`
  - 이때 Common 탭의 **Shared file은 선택하지 마세요.** 선택하면 키가 담긴 `.launch` 파일이 커밋될 수 있습니다.

## 7. 문제 해결

| 증상 | 해결 |
|---|---|
| 한글이 `???`나 깨진 글자로 보임 | 2-1 인코딩 설정 후 프로젝트 우클릭 → Properties → Resource → UTF-8 확인 |
| `Unsupported class file major version 65` / Java 21 문법 오류 | 2-2, 2-3에서 JDK 21 등록·선택 |
| `import org.junit` 빨간 줄 | jar를 직접 넣지 말고 Refresh Gradle Project |
| Gradle 가져오기가 멈춤 | 학교 와이파이 차단 가능성 → 휴대폰 핫스팟으로 재시도 |
| `.classpath`, `.project`가 변경 파일로 뜸 | 정상적으로 `.gitignore`에 있음. 이미 커밋했다면 팀에 알리기 |
