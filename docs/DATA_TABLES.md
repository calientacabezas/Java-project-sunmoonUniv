# 건물·색상·동의어 표

## 1. 건물 코드와 인접 관계

인접 기준: 도보 약 3분 이내. 같은 건물 다른 층 10점, 인접 건물 5점.

| 코드 | 건물명 | 인접 건물 코드 |
|---|---|---|
| ENGINEERING | 공학관 | (작성) |
| LIBRARY | 도서관 | |
| STUDENT_HALL | 학생회관 | |
| MAIN_HALL | 본관 | |

## 2. 색상 계열

같은 계열이면 10점.

| 계열 | 색상 코드 |
|---|---|
| ACHROMATIC 무채색 | WHITE, BLACK, GRAY, SILVER |
| WARM 따뜻한 색 | RED, PINK, ORANGE, YELLOW |
| COOL 차가운 색 | BLUE, NAVY, GREEN, PURPLE |
| EARTH 갈색 계열 | BROWN, BEIGE |

## 3. 동의어 (11/21 재정렬 대체안에 사용)

실험 기준선(KeywordQueryParser)에는 넣지 않습니다. 기준선은 enum의 표준 이름(흰색, 공학관 등)만 인식해야 LLM 효과를 공정하게 잴 수 있습니다.

| 표준 값 | 동의어·유사어 |
|---|---|
| WHITE | 흰색, 하얀, 화이트, 아이보리 |
| PINK | 분홍, 핑크, 연분홍, 로즈 |
| ELECTRONICS / 무선 이어폰 | 에어팟, 버즈, 이어버드, 블루투스 이어폰 |
| ENGINEERING | 공학관, 공대 |
| (추가) | |
