package lostfound.matching;

import lostfound.domain.Building;
import lostfound.domain.Category;
import lostfound.domain.ItemColor;
import java.time.LocalDate;

/** 정형화된 검색 조건. null 또는 UNKNOWN = "모름". rawText는 마스킹된 원문. */
public record SearchQuery(Category category, ItemColor color, LocalDate lostDate,
                          Building building, Integer floor, String detailText,
                          String rawText) {}

