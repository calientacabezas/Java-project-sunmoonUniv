package lostfound.domain;

/**
 * 교내 건물 코드. TODO(권도영 표 → 강채연 반영): 실제 선문대 건물 목록과 인접 관계로 교체.
 * 아래 값은 임시 예시입니다.
 */
public enum Building {
    ENGINEERING("공학관"),
    LIBRARY("도서관"),
    STUDENT_HALL("학생회관"),
    MAIN_HALL("본관"),
    HUMANITIES("인문관"),
    NATURAL_SCIENCE("자연관"),
    WONHWA("원화관"),
    SPORTS_SCIENCE("스포츠과학관"),
    GYMNASIUM("체육관"),
    INDUSTRY_ACADEMY("산학협력관"),
    ORANGE_CAFETERIA("오렌지식당"),
    DORMITORY("기숙사"),
    HEALTH_MEDICAL("보건의료관(평생교육원)"),
    UNKNOWN("모름");

    private final String label;
    Building(String label) { this.label = label; }
    public String label() { return label; }
}
