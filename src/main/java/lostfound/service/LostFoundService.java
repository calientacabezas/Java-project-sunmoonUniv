package lostfound.service;

import lostfound.domain.*;
import lostfound.llm.LlmClient;
import lostfound.matching.*;
import lostfound.parsing.QueryParser;
import lostfound.privacy.PiiMasker;
import lostfound.repository.ItemRepository;
import lostfound.repository.PhotoStorage;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UI 및 평가 실행기가 호출하는 업무 흐름 서비스 (등록, 마스킹·파싱, 규칙 5개+하이브리드 매칭, 회수 승인).
 */
public class LostFoundService {

    public record ParseOutcome(String maskedText, int maskedCount, SearchQuery query,
                               boolean usedLlm, String errorMessage) {}

    private final ItemRepository repository;
    private final LlmClient llmClient;
    private final QueryParser llmQueryParser;
    private final QueryParser fallbackQueryParser;
    private final SemanticReRanker semanticReRanker;
    private final List<ScoringRule> scoringRules;
    private final AtomicInteger idSequence = new AtomicInteger(100);
    private final PhotoStorage photoStorage = new PhotoStorage();
    private boolean forceOfflineOverride = false;

    public LostFoundService(ItemRepository repository, LlmClient llmClient) {
        this(repository, llmClient, null, null, null, defaultScoringRules());
    }

    public LostFoundService(ItemRepository repository, LlmClient llmClient,
                            QueryParser llmQueryParser, SemanticReRanker semanticReRanker) {
        this(repository, llmClient, llmQueryParser, null, semanticReRanker, defaultScoringRules());
    }

    public LostFoundService(ItemRepository repository, LlmClient llmClient,
                            QueryParser llmQueryParser, QueryParser fallbackQueryParser,
                            SemanticReRanker semanticReRanker, List<ScoringRule> scoringRules) {
        this.repository = repository;
        this.llmClient = llmClient;
        this.llmQueryParser = llmQueryParser;
        this.fallbackQueryParser = (fallbackQueryParser != null)
                ? fallbackQueryParser
                : LostFoundService::defaultKeywordParse;
        this.semanticReRanker = semanticReRanker;
        this.scoringRules = (scoringRules != null && !scoringRules.isEmpty())
                ? List.copyOf(scoringRules)
                : defaultScoringRules();
    }

    /** 규칙 5개(종류 40점, 색상 25점, 건물 20점, 층수 5점, 세부특징 10점) 기본 구성. */
    public static List<ScoringRule> defaultScoringRules() {
        return List.of(
                // 1) 종류 규칙 (40점)
                (item, query) -> {
                    if (query.category() == null || query.category() == Category.UNKNOWN) {
                        return new ScoreDetail("종류", 0, 40, false, "종류 미지정(분모 제외)");
                    }
                    if (item.getCategory() == query.category()) {
                        return new ScoreDetail("종류", 40, 40, true, "종류 일치(" + item.getCategory().label() + ") +40");
                    }
                    return new ScoreDetail("종류", 0, 40, true, "종류 불일치 +0");
                },
                // 2) 색상 규칙 (완전 일치 25점, 같은 계열 10점)
                (item, query) -> {
                    if (query.color() == null || query.color() == ItemColor.UNKNOWN) {
                        return new ScoreDetail("색상", 0, 25, false, "색상 미지정(분모 제외)");
                    }
                    if (item.getColor() == query.color()) {
                        return new ScoreDetail("색상", 25, 25, true, "색상 완전 일치(" + item.getColor().label() + ") +25");
                    }
                    if (item.getColor().group() != ColorGroup.NONE
                            && item.getColor().group() == query.color().group()) {
                        return new ScoreDetail("색상", 10, 25, true, "같은 계열 색상(" + item.getColor().label() + ") +10");
                    }
                    return new ScoreDetail("색상", 0, 25, true, "색상 불일치 +0");
                },
                // 3) 건물 규칙 (동일 건물 20점, 인접 건물 10점)
                (item, query) -> {
                    if (query.building() == null || query.building() == Building.UNKNOWN) {
                        return new ScoreDetail("건물", 0, 20, false, "건물 미지정(분모 제외)");
                    }
                    if (item.getBuilding() == query.building()) {
                        return new ScoreDetail("건물", 20, 20, true, "건물 일치(" + item.getBuilding().label() + ") +20");
                    }
                    if (item.getBuilding().isAdjacent(query.building())) {
                        return new ScoreDetail("건물", 10, 20, true, "인접 건물(" + item.getBuilding().label() + ") +10");
                    }
                    return new ScoreDetail("건물", 0, 20, true, "건물 불일치 +0");
                },
                // 4) 층수 규칙 (5점)
                (item, query) -> {
                    if (query.floor() == null || query.floor() == 0) {
                        return new ScoreDetail("층수", 0, 5, false, "층수 미지정(분모 제외)");
                    }
                    if (item.getFloor() == query.floor()) {
                        String floorStr = item.getFloor() == -1 ? "지하 1층" : item.getFloor() + "층";
                        return new ScoreDetail("층수", 5, 5, true, "층수 일치(" + floorStr + ") +5");
                    }
                    return new ScoreDetail("층수", 0, 5, true, "층수 불일치 +0");
                },
                // 5) 세부 특징 규칙 (Item 하위 클래스 다형성 detailScore: 0~10점)
                (item, query) -> {
                    if (query.detailText() == null || query.detailText().isBlank()) {
                        return new ScoreDetail("세부특징", 0, 10, false, "세부설명 없음(분모 제외)");
                    }
                    int dScore = item.detailScore(query);
                    return new ScoreDetail("세부특징", dScore, 10, true, "세부특징 일치 +" + dScore);
                }
        );
    }

    public boolean isOnlineMode() {
        return !forceOfflineOverride && llmClient != null && llmClient.isOnline();
    }

    public void setForceOfflineOverride(boolean offline) {
        this.forceOfflineOverride = offline;
    }

    public String maskText(String rawInput) {
        return PiiMasker.mask(rawInput == null ? "" : rawInput);
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

    /**
     * 1-1. 사진과 함께 습득물 등록. 사진은 먼저 검사·복사하고, 실패하면 등록하지 않는다 (예외 메시지를 화면에 표시).
     * photoSource가 null이면 사진 없이 등록한다.
     */
    public void registerItem(Item item, Path photoSource) {
        if (photoSource != null) {
            item.setPhotoPath(photoStorage.store(item.getId(), photoSource));
        }
        repository.save(item);
    }

    /** 2. 개인정보 마스킹 후 자연어 → SearchQuery 변환 (온라인 시 LLM API 호출, 장애 시 규칙 파서 자동 폴백) */
    public ParseOutcome parseNaturalQuery(String rawInput) {
        String masked = maskText(rawInput);
        int maskedCount = PiiMasker.countMasked(masked);

        if (isOnlineMode() && llmQueryParser != null) {
            try {
                SearchQuery llmQuery = llmQueryParser.parse(masked);
                return new ParseOutcome(masked, maskedCount, llmQuery, true, "");
            } catch (Exception e) {
                System.err.println("[LLM 경고] API 추출 실패: " + e.getMessage());
                return new ParseOutcome(masked, maskedCount, fallbackQueryParser.parse(masked), false, e.getMessage());
            }
        }
        return new ParseOutcome(masked, maskedCount, fallbackQueryParser.parse(masked), false, "오프라인 모드 상태");
    }

    /** 오프라인/규칙 기반 파서 단독 호출 (AccuracyEvaluator 규칙 기준선용). */
    public SearchQuery parseRuleOnly(String rawInput) {
        String masked = maskText(rawInput);
        return fallbackQueryParser.parse(masked);
    }

    /** 3-A. 규칙 점수 전용 매칭 (반환 완료·기한 만료·폐기 물품 제외). */
    public List<MatchResult> searchRuleOnly(SearchQuery query) {
        List<MatchResult> candidates = new ArrayList<>();

        for (Item item : repository.findAll()) {
            if (!item.isMatchable()) {
                continue;
            }

            List<ScoreDetail> details = evaluateRules(item, query);
            int ruleScore = calculateNormalizedRuleScore(details);
            String reason = buildReasonSummary(details);

            candidates.add(new MatchResult(item, ruleScore, 0, ruleScore, details, reason));
        }

        candidates.sort(MatchResult.RANKING);
        return new ArrayList<>(candidates.subList(0, Math.min(TOP_K, candidates.size())));
    }

    /** 의미 재평가에 보낼 규칙 점수 상위 후보 수. */
    public static final int TOP_K = 10;

    /** 전체 미반환 목록 (조건 없이 보관 중·확인 중 물품 전체, 최근 습득일 순). LLM을 호출하지 않는다. */
    public List<MatchResult> listAllMatchable() {
        List<MatchResult> all = new ArrayList<>();
        for (Item item : repository.findAll()) {
            if (item.isMatchable()) {
                all.add(new MatchResult(item, 0, 0, 0, List.of(), "전체 목록 (검색 조건 없음)"));
            }
        }
        all.sort(MatchResult.RANKING);
        return all;
    }

    /** 3-B. 하이브리드 매칭 (규칙 5개 점수 상위 10개 추출 후 온라인 시 SemanticReRanker 재정렬). */
    public List<MatchResult> searchMatches(SearchQuery query) {
        List<MatchResult> top10 = searchRuleOnly(query);

        if (isOnlineMode() && semanticReRanker != null && !top10.isEmpty()) {
            String safeRawText = maskText(query.rawText());
            return semanticReRanker.rerank(safeRawText, top10);
        }
        return top10;
    }

    /** 4. 소유자 확인 중 전환 (ClaimService 연계용) */
    public void markItemChecking(String itemId) {
        repository.findById(itemId).ifPresent(item -> {
            item.markChecking();
            repository.save(item);
        });
    }

    /** 4-1. 소유자 확인 실패·취소 시 다시 보관 중으로 되돌림 (확인 중 상태로 남지 않도록). */
    public void cancelChecking(String itemId) {
        repository.findById(itemId).ifPresent(item -> {
            if (item.getStatus() == ItemStatus.CHECKING) {
                item.setStatus(ItemStatus.STORED);
                repository.save(item);
            }
        });
    }

    /** 5. 관리자 최종 회수 승인 (상태를 RETURNED로 전환) */
    public void approveRecovery(String itemId) {
        repository.findById(itemId).ifPresent(item -> {
            item.markReturned();
            repository.save(item);
        });
    }

    private List<ScoreDetail> evaluateRules(Item item, SearchQuery query) {
        List<ScoreDetail> list = new ArrayList<>(scoringRules.size());
        for (ScoringRule rule : scoringRules) {
            list.add(rule.evaluate(item, query));
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

    private String buildReasonSummary(List<ScoreDetail> details) {
        List<String> positive = new ArrayList<>();
        for (ScoreDetail d : details) {
            if (d.comparable() && d.earned() > 0) {
                positive.add(d.reason());
            }
        }
        if (positive.isEmpty()) {
            return "일치 항목 적음 (기타 조건 검토 필요)";
        }
        return String.join(", ", positive);
    }

    /** 오프라인 및 규칙 기준선용 기본 키워드 파서 (KeywordQueryParser 미주입 시 동작). */
    private static SearchQuery defaultKeywordParse(String maskedText) {
        String text = (maskedText == null) ? "" : maskedText;

        Category cat = Category.UNKNOWN;
        if (text.contains("버즈") || text.contains("에어팟") || text.contains("노트북")
                || text.contains("충전기") || text.contains("폰") || text.contains("아이패드") || text.contains("갤럭시")) {
            cat = Category.ELECTRONICS;
        } else if (text.contains("지갑") || text.contains("학생증") || text.contains("카드") || text.contains("신분증")) {
            cat = Category.WALLET_CARD;
        } else if (text.contains("가방") || text.contains("백팩") || text.contains("옷")
                || text.contains("패딩") || text.contains("후드") || text.contains("자켓") || text.contains("에코백")) {
            cat = Category.BAG_CLOTHING;
        } else if (text.contains("우산") || text.contains("책") || text.contains("텀블러") || text.contains("열쇠")) {
            cat = Category.GENERAL;
        }

        ItemColor color = ItemColor.UNKNOWN;
        if (text.contains("흰") || text.contains("하얀") || text.contains("화이트")) color = ItemColor.WHITE;
        else if (text.contains("검정") || text.contains("검은") || text.contains("블랙")) color = ItemColor.BLACK;
        else if (text.contains("회색") || text.contains("그레이")) color = ItemColor.GRAY;
        else if (text.contains("은색") || text.contains("실버")) color = ItemColor.SILVER;
        else if (text.contains("빨강") || text.contains("빨간") || text.contains("레드")) color = ItemColor.RED;
        else if (text.contains("분홍") || text.contains("핑크")) color = ItemColor.PINK;
        else if (text.contains("주황") || text.contains("오렌지")) color = ItemColor.ORANGE;
        else if (text.contains("노랑") || text.contains("노란") || text.contains("옐로우")) color = ItemColor.YELLOW;
        else if (text.contains("파랑") || text.contains("파란") || text.contains("블루")) color = ItemColor.BLUE;
        else if (text.contains("남색") || text.contains("네이비")) color = ItemColor.NAVY;
        else if (text.contains("초록") || text.contains("녹색") || text.contains("그린")) color = ItemColor.GREEN;
        else if (text.contains("보라") || text.contains("퍼플")) color = ItemColor.PURPLE;
        else if (text.contains("갈색") || text.contains("브라운")) color = ItemColor.BROWN;
        else if (text.contains("베이지") || text.contains("아이보리")) color = ItemColor.BEIGE;

        Building building = Building.UNKNOWN;
        if (text.contains("공대") || text.contains("공학관")) building = Building.ENGINEERING;
        else if (text.contains("도서관")) building = Building.LIBRARY;
        else if (text.contains("학생회관") || text.contains("학관")) building = Building.STUDENT_HALL;
        else if (text.contains("본관")) building = Building.MAIN_HALL;
        else if (text.contains("인문관") || text.contains("인문대")) building = Building.HUMANITIES;
        else if (text.contains("자연관") || text.contains("자연대")) building = Building.NATURAL_SCIENCE;
        else if (text.contains("원화관")) building = Building.WONHWA;
        else if (text.contains("스포츠과학관") || text.contains("스과관")) building = Building.SPORTS_SCIENCE;
        else if (text.contains("체육관")) building = Building.GYMNASIUM;
        else if (text.contains("산학협력관") || text.contains("산학관")) building = Building.INDUSTRY_ACADEMY;
        else if (text.contains("오렌지식당") || text.contains("오렌지")) building = Building.ORANGE_CAFETERIA;
        else if (text.contains("기숙사") || text.contains("생활관") || text.contains("성화학숙")) building = Building.DORMITORY;
        else if (text.contains("보건의료관") || text.contains("평생교육원") || text.contains("보건관")) building = Building.HEALTH_MEDICAL;

        Integer floor = null;
        if (text.contains("지하 1층") || text.contains("지하1층")) floor = -1;
        else if (text.contains("1층")) floor = 1;
        else if (text.contains("2층")) floor = 2;
        else if (text.contains("3층")) floor = 3;
        else if (text.contains("4층")) floor = 4;
        else if (text.contains("5층")) floor = 5;
        else if (text.contains("6층")) floor = 6;
        else if (text.contains("7층")) floor = 7;

        LocalDate date = null;
        if (text.contains("오늘")) date = LocalDate.now();
        else if (text.contains("어제")) date = LocalDate.now().minusDays(1);
        else if (text.contains("그제") || text.contains("그저께")) date = LocalDate.now().minusDays(2);

        return new SearchQuery(cat, color, date, building, floor, text, text);
    }
}