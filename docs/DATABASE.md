# MySQL 저장소 설정

프로그램은 **MySQL(JDBC)을 기본 저장소**로 쓰고, 접속 정보가 없거나 연결에 실패하면 **텍스트 파일(`data/items.txt`)로 자동 전환**합니다.
화면 오른쪽 위 배지에서 지금 어느 쪽에 저장 중인지 볼 수 있습니다 ("MySQL 저장" / "파일 저장 (items.txt)").

MySQL이 없는 팀원은 아무 설정 없이 파일 저장으로 개발·테스트하면 됩니다. **시연 PC 한 대만** 아래 설정을 하면 됩니다.

## 1. MySQL 준비 (시연 PC, 1회)

1. MySQL 8.x 설치 (수업 실습 때 설치한 것을 그대로 써도 됩니다).
2. MySQL Workbench 또는 명령줄에서 `docs/db/schema.sql`의 **1번 부분**을 실행합니다. `여기에_비밀번호`는 직접 정한 비밀번호로 바꿉니다.
   테이블은 프로그램이 처음 연결할 때 자동으로 만들고, 비어 있으면 샘플 30건을 넣습니다.

## 2. 접속 정보 등록 (환경변수)

비밀번호를 코드나 저장소에 남기지 않기 위해 환경변수로만 받습니다.

| 환경변수 | 예시 |
|---|---|
| `LF_DB_URL` | `jdbc:mysql://localhost:3306/lostfound?serverTimezone=Asia/Seoul&characterEncoding=UTF-8` |
| `LF_DB_USER` | `lostfound` |
| `LF_DB_PASSWORD` | (1번에서 정한 비밀번호) |

**방법 A: 이클립스 실행 설정 (권장)**
Run → Run Configurations → `Main` → **Environment** 탭 → Add로 세 개 추가 → Apply.
Common 탭의 Shared file은 선택하지 않습니다 (비밀번호가 담긴 파일이 커밋될 수 있음).

**방법 B: Windows 환경변수**
```
setx LF_DB_URL "jdbc:mysql://localhost:3306/lostfound?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
setx LF_DB_USER "lostfound"
setx LF_DB_PASSWORD "비밀번호"
```
실행 후 이클립스를 완전히 껐다 켭니다.

## 3. 확인

1. `Main` 실행 → 오른쪽 위 배지가 **"MySQL 저장"**이면 성공.
2. 물건을 하나 등록하고 MySQL Workbench에서 `SELECT * FROM lostfound.found_items ORDER BY updated_at DESC;`로 보이는지 확인.
3. 연동 테스트: 같은 환경변수를 넣고 `src/test/java/lostfound/repository/MySqlItemRepositoryIT.java`를 JUnit으로 실행 (환경변수가 없으면 자동으로 건너뜀).

배지가 "파일 저장"으로 나오면 콘솔(Console 탭)의 `[저장소]` 줄에 실패 이유가 나옵니다.

| 콘솔 메시지 | 원인과 해결 |
|---|---|
| `No suitable driver` | Gradle 새로고침이 안 됨 → 프로젝트 우클릭 → Gradle → Refresh Gradle Project |
| `Access denied` | 사용자 이름·비밀번호 확인 |
| `Communications link failure` | MySQL 서비스가 꺼져 있음 → Windows 서비스에서 MySQL 시작 |
| `Unknown database 'lostfound'` | 1번의 CREATE DATABASE를 실행하지 않음 |

## 4. 설계

- `ItemRepository` 인터페이스 하나에 구현체 3개: `MySqlItemRepository`(JDBC), `TextFileItemRepository`(파일), `InMemoryItemRepository`(테스트·실험).
- `RepositoryFactory`가 시작할 때 고릅니다: MySQL 연결 성공 → MySQL, 실패 → 파일. 서비스·화면 코드는 어느 저장소인지 모릅니다.
- JDBC는 수업 실습과 같은 `DriverManager` + `PreparedStatement`(모든 값을 `?`로 바인딩, SQL 인젝션 방지) 방식입니다.
- 같은 ID를 다시 저장하면 `INSERT ... ON DUPLICATE KEY UPDATE`로 상태만 갱신됩니다 (등록·확인 중·반환 처리).
- 두 저장소 모두 `ItemCodec`으로 같은 필드 구조를 써서, 파일 ↔ DB 사이에 데이터 의미가 같습니다.
