package lostfound.privacy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 외부 LLM API로 보내기 전 개인정보를 [MASKED]로 바꾼다.
 * 적용 순서가 중요하다: 긴 숫자(카드) → 전화번호 → 학번 → 이메일 → 이름.
 * 이름은 정규식으로 완벽히 잡을 수 없으므로 "이름은 OOO", "OOO의 학생증" 같은 문맥만 처리한다.
 */
public final class PiiMasker {
    public static final String MASK = "[MASKED]";

    private static final Map<String, Pattern> PATTERNS = new LinkedHashMap<>();
    static {
        // 카드번호: 4자리 3묶음 + 1~4자리 (공백·하이픈 허용)
        PATTERNS.put("CARD", Pattern.compile("(?<!\\d)(?:\\d{4}[- ]?){3}\\d{1,4}(?!\\d)"));
        // 휴대폰: 010-1234-5678, 01012345678, 010 1234 5678
        PATTERNS.put("PHONE", Pattern.compile("(?<!\\d)01[016789][- .]?\\d{3,4}[- .]?\\d{4}(?!\\d)"));
        // 학번: 19xx 또는 20xx로 시작하는 8~10자리 숫자
        PATTERNS.put("STUDENT_ID", Pattern.compile("(?<!\\d)(?:19|20)\\d{6,8}(?!\\d)"));
        // 이메일
        PATTERNS.put("EMAIL", Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+"));
    }

    // "이름은 김철수", "성명: 홍길동" → 키워드는 남기고 이름만 가림.
    // 이름 뒤 조사(이고, 이며 등)를 이름으로 삼키지 않도록 가장 짧게 맞추고 뒤를 확인한다.
    private static final Pattern LABELED_NAME =
            Pattern.compile("(이름|성명)(\\s*(?:은|는|이|:)?\\s*)([가-힣]{2,4}?)"
                    + "(?=이고|이며|이에요|입니다|이|학생|씨|님|[^가-힣]|$)");
    // "김철수의 학생증", "홍길동의 신분증"
    private static final Pattern OWNER_NAME =
            Pattern.compile("(?<![가-힣])([가-힣]{2,4})(의\\s*(?:학생증|신분증|카드))");

    private PiiMasker() {}

    public static String mask(String text) {
        if (text == null || text.isEmpty()) return text;
        String result = text;
        for (Pattern p : PATTERNS.values()) {
            result = p.matcher(result).replaceAll(Matcher.quoteReplacement(MASK));
        }
        result = LABELED_NAME.matcher(result).replaceAll("$1$2" + Matcher.quoteReplacement(MASK));
        result = OWNER_NAME.matcher(result).replaceAll(Matcher.quoteReplacement(MASK) + "$2");
        return result;
    }

    /** 로그용: 마스킹된 항목 수 (원문 값은 남기지 않는다). */
    public static int countMasked(String masked) {
        if (masked == null) return 0;
        int count = 0, idx = 0;
        while ((idx = masked.indexOf(MASK, idx)) >= 0) { count++; idx += MASK.length(); }
        return count;
    }
}

