package lostfound.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 선문대학교 교내 건물 코드 및 인접 건물 관계.
 * 건물 점수 규칙에서 같은 건물·인접 건물 판단에 사용한다.
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

    private static final Map<Building, Set<Building>> ADJACENT_MAP = Map.ofEntries(
            Map.entry(ENGINEERING, EnumSet.of(INDUSTRY_ACADEMY, NATURAL_SCIENCE, LIBRARY)),
            Map.entry(INDUSTRY_ACADEMY, EnumSet.of(ENGINEERING, NATURAL_SCIENCE)),
            Map.entry(NATURAL_SCIENCE, EnumSet.of(ENGINEERING, INDUSTRY_ACADEMY, LIBRARY, MAIN_HALL)),
            Map.entry(LIBRARY, EnumSet.of(ENGINEERING, NATURAL_SCIENCE, MAIN_HALL, WONHWA)),
            Map.entry(MAIN_HALL, EnumSet.of(LIBRARY, NATURAL_SCIENCE, WONHWA, HUMANITIES)),
            Map.entry(WONHWA, EnumSet.of(MAIN_HALL, LIBRARY, HUMANITIES, STUDENT_HALL)),
            Map.entry(HUMANITIES, EnumSet.of(MAIN_HALL, WONHWA, HEALTH_MEDICAL)),
            Map.entry(STUDENT_HALL, EnumSet.of(WONHWA, ORANGE_CAFETERIA, GYMNASIUM, SPORTS_SCIENCE)),
            Map.entry(ORANGE_CAFETERIA, EnumSet.of(STUDENT_HALL, DORMITORY, GYMNASIUM)),
            Map.entry(SPORTS_SCIENCE, EnumSet.of(STUDENT_HALL, GYMNASIUM)),
            Map.entry(GYMNASIUM, EnumSet.of(STUDENT_HALL, SPORTS_SCIENCE, ORANGE_CAFETERIA)),
            Map.entry(DORMITORY, EnumSet.of(ORANGE_CAFETERIA, HEALTH_MEDICAL)),
            Map.entry(HEALTH_MEDICAL, EnumSet.of(HUMANITIES, DORMITORY))
    );

    private final String label;

    Building(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** 두 건물이 서로 인접한 건물인지 판별 (동일 건물·UNKNOWN은 false). */
    public boolean isAdjacent(Building other) {
        if (other == null || this == UNKNOWN || other == UNKNOWN || this == other) {
            return false;
        }
        return ADJACENT_MAP.getOrDefault(this, EnumSet.noneOf(Building.class)).contains(other);
    }
}