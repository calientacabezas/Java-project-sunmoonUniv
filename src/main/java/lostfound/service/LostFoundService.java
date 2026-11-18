package lostfound.service;

import lostfound.domain.*;
import lostfound.llm.LlmClient;
import lostfound.matching.*;
import lostfound.parsing.QueryParser;
import lostfound.privacy.PiiMasker;
import lostfound.repository.ItemRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UI가 호출하는 업무 흐름 서비스 (등록, 자연어 파싱, 하이브리드 매칭, 회수 승인).
 */
public class LostFoundService {

    public record ParseOutcome(String maskedText, int maskedCount, SearchQuery query,
                               boolean usedLlm, String errorMessage) {}

    private final ItemRepository repository;
    private final LlmClient llmClient;
    private final QueryParser queryParser;
    private final SemanticReRanker semanticReRanker;
    private final AtomicInteger idSequence = new AtomicInteger(100);
    private boolean forceOfflineOverride = false;

    public LostFoundService(ItemRepository repository, LlmClient llmClient) {
        this(repository, llmClient, null, null);
    }

    public LostFoundService(ItemRepository repository, LlmClient llmClient,
                            QueryParser queryParser, SemanticReRanker semanticReRanker) {
        this.repository = repository;
        this.llmClient = llmClient;
        this.queryParser = queryParser;
        this.semanticReRanker = semanticReRanker;
    }

    public boolean isOnlineMode() {
        return !forceOfflineOverride && llmClient != null && llmClient.isOnline();
    }

    public void setForceOfflineOverride(boolean offline) {
        this.forceOfflineOverride = offline;
    }

    public String nextItemId() {
        String candidate;
        do {
            candidate = "ITEM-" + idSequence.incrementAndGet();
        } while (repository.findById(candidate).isPresent());
        return candidate;
    }

    /** 1. 습득물 등록 */
    public void registerItem(Item item) {
        repository.save(item);
    }

    /** 2. 개인정보 마스킹 후 자연어 → SearchQuery 변환 (온라인 시 LLM API 호출, 장애 시 자동 폴백) */
    public ParseOutcome parseNaturalQuery(String rawInput) {
        String masked = PiiMasker.mask(rawInput == null ? "" : rawInput);
        int maskedCount = PiiMasker.countMasked(masked);

        if (isOnlineMode() && queryParser != null) {
            try {
                SearchQuery llmQuery = queryParser.parse(masked);
                return new ParseOutcome(masked, maskedCount, llmQuery, true, "");
            } catch (Exception e) {
                System.err.println("[LLM 경고] API 추출 실패: " + e.getMessage());
                return new ParseOutcome(masked, maskedCount, fallbackKeywordParse(masked), false, e.getMessage());
            }
        }
        return new ParseOutcome(masked, maskedCount, fallbackKeywordParse(masked), false, "오프라인 모드 상태");
    }

    /** 3. 규칙 점수 + 하이브리드 매칭 (반환 완료 물품 자동 제외) */
    public List<MatchResult> searchMatches(SearchQuery query) {
        List<MatchResult> candidates = new ArrayList<>();

        for (Item item : repository.findAll()) {
            if (item.getStatus() == ItemStatus.RETURNED || item.getStatus() == ItemStatus.DISPOSED) {
                continue;
            }

            List<ScoreDetail> details = evaluateRules(item, query);
            int ruleScore = calculateNormalizedRuleScore(details);
            int semanticScore = 0;
            int finalScore = ruleScore;
            String reason = buildReasonSummary(details, false);

            candidates.add(new MatchResult(item, ruleScore, semanticScore, finalScore, details, reason));
        }

        candidates.sort((a, b) -> Integer.compare(b.finalScore(), a.finalScore()));
        List<MatchResult> top10 = candidates.subList(0, Math.min(10, candidates.size()));

        if (isOnlineMode() && semanticReRanker != null && !top10.isEmpty()) {
            return semanticReRanker.rerank(query.rawText(), top10);
        }
        return new ArrayList<>(top10);
    }

    /** 4. 관리자 회수 승인 (상태를 RETURNED로 전환) */
    public void approveRecovery(String itemId) {
        repository.findById(itemId).ifPresent(item -> {
            item.markReturned();
            repository.save(item);
        });
    }

    private List<ScoreDetail> evaluateRules(Item item, SearchQuery query) {
        List<ScoreDetail> list = new ArrayList<>();

        // 1) 종류 (40점)
        if (query.category() == null || query.category() == Category.UNKNOWN) {
            list.add(new ScoreDetail("종류", 0, 40, false, "종류 미지정(분모 제외)"));
        } else if (item.getCategory() == query.category()) {
            list.add(new ScoreDetail("종류", 40, 40, true, "종류 일치(" + item.getCategory().label() + ") +40"));
        } else {
            list.add(new ScoreDetail("종류", 0, 40, true, "종류 불일치 +0"));
        }

        // 2) 색상 (25점 / 같은 계열 10점)
        if (query.color() == null || query.color() == ItemColor.UNKNOWN) {
            list.add(new ScoreDetail("색상", 0, 25, false, "색상 미지정(분모 제외)"));
        } else if (item.getColor() == query.color()) {
            list.add(new ScoreDetail("색상", 25, 25, true, "색상 완전 일치(" + item.getColor().label() + ") +25"));
        } else if (item.getColor().group() != ColorGroup.NONE
                && item.getColor().group() == query.color().group()) {
            list.add(new ScoreDetail("색상", 10, 25, true, "같은 계열 색상(" + item.getColor().label() + ") +10"));
        } else {
            list.add(new ScoreDetail("색상", 0, 25, true, "색상 불일치 +0"));
        }

        // 3) 건물 (20점)
        if (query.building() == null || query.building() == Building.UNKNOWN) {
            list.add(new ScoreDetail("건물", 0, 20, false, "건물 미지정(분모 제외)"));
        } else if (item.getBuilding() == query.building()) {
            list.add(new ScoreDetail("건물", 20, 20, true, "건물 일치(" + item.getBuilding().label() + ") +20"));
        } else {
            list.add(new ScoreDetail("건물", 0, 20, true, "건물 불일치 +0"));
        }

        // 4) 층수 (5점)
        if (query.floor() == null || query.floor() == 0) {
            list.add(new ScoreDetail("층수", 0, 5, false, "층수 미지정(분모 제외)"));
        } else if (item.getFloor() == query.floor()) {
            list.add(new ScoreDetail("층수", 5, 5, true, "층수 일치(" + item.getFloor() + "층) +5"));
        } else {
            list.add(new ScoreDetail("층수", 0, 5, true, "층수 불일치 +0"));
        }

        // 5) 세부 특징 (다형성 detailScore: 0~10점)
        if (query.detailText() == null || query.detailText().isBlank()) {
            list.add(new ScoreDetail("세부특징", 0, 10, false, "세부설명 없음"));
        } else {
            int dScore = item.detailScore(query);
            list.add(new ScoreDetail("세부특징", dScore, 10, true, "세부특징 일치 +" + dScore));
        }

        return list;
    }

    private int calculateNormalizedRuleScore(List<ScoreDetail> details) {
        int earnedSum = 0;
        int maxSum = 0;
        for (ScoreDetail d : details) {
            if (d.comparable()) {
                earnedSum += d.earned();
                maxSum += d.max();
            }
        }
        if (maxSum == 0) return 0;
        return (int) Math.round((earnedSum * 100.0) / maxSum);
    }

    private String buildReasonSummary(List<ScoreDetail> details, boolean hybrid) {
        List<String> positive = new ArrayList<>();
        for (ScoreDetail d : details) {
            if (d.comparable() && d.earned() > 0) {
                positive.add(d.reason());
            }
        }
        if (positive.isEmpty()) {
            return "일치 항목 적음 (기타 조건 검토 필요)";
        }
        String joined = String.join(", ", positive);
        return hybrid ? joined + " [LLM 재평가 반영]" : joined;
    }

    private SearchQuery fallbackKeywordParse(String maskedText) {
        Category cat = Category.UNKNOWN;
        if (maskedText.contains("버즈") || maskedText.contains("에어팟") || maskedText.contains("노트북")
                || maskedText.contains("충전기") || maskedText.contains("폰")) {
            cat = Category.ELECTRONICS;
        } else if (maskedText.contains("지갑") || maskedText.contains("학생증") || maskedText.contains("카드")) {
            cat = Category.WALLET_CARD;
        } else if (maskedText.contains("가방") || maskedText.contains("백팩") || maskedText.contains("옷")
                || maskedText.contains("패딩") || maskedText.contains("후드")) {
            cat = Category.BAG_CLOTHING;
        } else if (maskedText.contains("우산") || maskedText.contains("책") || maskedText.contains("텀블러")) {
            cat = Category.GENERAL;
        }

        ItemColor color = ItemColor.UNKNOWN;
        if (maskedText.contains("흰") || maskedText.contains("하얀") || maskedText.contains("화이트")) color = ItemColor.WHITE;
        else if (maskedText.contains("검정") || maskedText.contains("검은") || maskedText.contains("블랙")) color = ItemColor.BLACK;
        else if (maskedText.contains("회색") || maskedText.contains("그레이")) color = ItemColor.GRAY;
        else if (maskedText.contains("은색") || maskedText.contains("실버")) color = ItemColor.SILVER;
        else if (maskedText.contains("빨강") || maskedText.contains("빨간") || maskedText.contains("레드")) color = ItemColor.RED;
        else if (maskedText.contains("분홍") || maskedText.contains("핑크")) color = ItemColor.PINK;
        else if (maskedText.contains("주황") || maskedText.contains("오렌지")) color = ItemColor.ORANGE;
        else if (maskedText.contains("노랑") || maskedText.contains("노란") || maskedText.contains("옐로우")) color = ItemColor.YELLOW;
        else if (maskedText.contains("파랑") || maskedText.contains("파란") || maskedText.contains("블루")) color = ItemColor.BLUE;
        else if (maskedText.contains("남색") || maskedText.contains("네이비")) color = ItemColor.NAVY;
        else if (maskedText.contains("초록") || maskedText.contains("녹색") || maskedText.contains("그린")) color = ItemColor.GREEN;
        else if (maskedText.contains("보라") || maskedText.contains("퍼플")) color = ItemColor.PURPLE;
        else if (maskedText.contains("갈색") || maskedText.contains("브라운")) color = ItemColor.BROWN;
        else if (maskedText.contains("베이지") || maskedText.contains("아이보리")) color = ItemColor.BEIGE;

        Building building = Building.UNKNOWN;
        if (maskedText.contains("공대") || maskedText.contains("공학관")) building = Building.ENGINEERING;
        else if (maskedText.contains("도서관")) building = Building.LIBRARY;
        else if (maskedText.contains("학생회관") || maskedText.contains("학관")) building = Building.STUDENT_HALL;
        else if (maskedText.contains("본관")) building = Building.MAIN_HALL;
        else if (maskedText.contains("인문관") || maskedText.contains("인문대")) building = Building.HUMANITIES;
        else if (maskedText.contains("자연관") || maskedText.contains("자연대")) building = Building.NATURAL_SCIENCE;
        else if (maskedText.contains("원화관")) building = Building.WONHWA;
        else if (maskedText.contains("스포츠과학관") || maskedText.contains("스과관")) building = Building.SPORTS_SCIENCE;
        else if (maskedText.contains("체육관")) building = Building.GYMNASIUM;
        else if (maskedText.contains("산학협력관") || maskedText.contains("산학관")) building = Building.INDUSTRY_ACADEMY;
        else if (maskedText.contains("오렌지식당") || maskedText.contains("오렌지")) building = Building.ORANGE_CAFETERIA;
        else if (maskedText.contains("기숙사") || maskedText.contains("생활관") || maskedText.contains("성화학숙")) building = Building.DORMITORY;
        else if (maskedText.contains("보건의료관") || maskedText.contains("평생교육원") || maskedText.contains("보건관")) building = Building.HEALTH_MEDICAL;

        Integer floor = null;
        if (maskedText.contains("1층")) floor = 1;
        else if (maskedText.contains("2층")) floor = 2;
        else if (maskedText.contains("3층")) floor = 3;
        else if (maskedText.contains("4층")) floor = 4;
        else if (maskedText.contains("5층")) floor = 5;
        else if (maskedText.contains("6층")) floor = 6;
        else if (maskedText.contains("7층")) floor = 7;

        LocalDate date = maskedText.contains("어제") ? LocalDate.now().minusDays(1) : LocalDate.now();
        return new SearchQuery(cat, color, date, building, floor, maskedText, maskedText);
    }
}
