package lostfound.parsing;

import lostfound.domain.Building;
import lostfound.domain.Category;
import lostfound.domain.ItemColor;
import lostfound.matching.SearchQuery;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 정확도 실험의 "규칙만" 기준선 파서.
 *
 * <p>enum에 정의된 <b>표준 이름</b>(Category·ItemColor·Building의 label)만 인식한다.
 * "공대", "하얀", "버즈", 영어 표현 같은 동의어·외국어는 일부러 인식하지 않는다.
 * 그래야 LLM 추출을 붙였을 때 늘어난 정확도를 공정하게 잴 수 있다.
 * 앱의 오프라인 대체 파서(LostFoundService 기본 파서)와는 목적이 다르므로 섞어 쓰지 않는다.</p>
 */
public class KeywordQueryParser implements QueryParser {

    private static final Pattern BASEMENT = Pattern.compile("지하\\s*(\\d+)\\s*층");
    private static final Pattern FLOOR = Pattern.compile("(?<![0-9])(\\d{1,2})\\s*층");

    @Override
    public SearchQuery parse(String maskedInput) {
        String text = maskedInput == null ? "" : maskedInput;
        return new SearchQuery(
                findCategory(text),
                findColor(text),
                null,                 // 기준선은 날짜를 추출하지 않음 (실험 재현성, AccuracyEvaluator 참고)
                findBuilding(text),
                findFloor(text),
                text,                 // 세부 특징 비교에는 원문 전체를 그대로 사용
                text);
    }

    /** Category label("지갑·카드" 등)을 '·'로 나눈 표준 단어 중 하나가 들어 있으면 그 종류로 본다. */
    static Category findCategory(String text) {
        for (Category c : Category.values()) {
            if (c == Category.UNKNOWN || c == Category.GENERAL) continue; // "기타"는 문장에서 의미가 모호해 제외
            for (String word : c.label().split("·")) {
                if (!word.isBlank() && text.contains(word.trim())) return c;
            }
        }
        return Category.UNKNOWN;
    }

    static ItemColor findColor(String text) {
        for (ItemColor color : ItemColor.values()) {
            if (color == ItemColor.UNKNOWN) continue;
            if (text.contains(color.label())) return color;
        }
        return ItemColor.UNKNOWN;
    }

    /** 긴 이름부터 비교해 "스포츠과학관"이 다른 이름의 일부로 잘못 잡히지 않게 한다. */
    static Building findBuilding(String text) {
        Building best = Building.UNKNOWN;
        int bestLen = 0;
        for (Building b : Building.values()) {
            if (b == Building.UNKNOWN) continue;
            String label = b.label();
            int paren = label.indexOf('(');                    // "보건의료관(평생교육원)" → 두 이름 모두 표준어로 인정
            String[] names = paren < 0
                    ? new String[]{label}
                    : new String[]{label.substring(0, paren), label.substring(paren + 1, label.length() - 1)};
            for (String name : names) {
                if (name.length() > bestLen && text.contains(name)) {
                    best = b;
                    bestLen = name.length();
                }
            }
        }
        return best;
    }

    static Integer findFloor(String text) {
        Matcher basement = BASEMENT.matcher(text);
        if (basement.find()) return -Integer.parseInt(basement.group(1));
        Matcher floor = FLOOR.matcher(text);
        if (floor.find()) {
            int f = Integer.parseInt(floor.group(1));
            return (f >= 1 && f <= 15) ? f : null;
        }
        return null;
    }
}
