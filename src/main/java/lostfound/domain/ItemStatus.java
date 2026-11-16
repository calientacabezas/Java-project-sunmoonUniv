package lostfound.domain;

/** 물품 상태. 보관 중 → 확인 중 → 반환 완료 / 보관 중 → 기한 경과 → 이관·폐기. */
public enum ItemStatus { STORED, CHECKING, RETURNED, EXPIRED, DISPOSED }
