-- 교내 분실물 회수 지원 시스템 MySQL 스키마
-- 프로그램이 처음 연결할 때 테이블을 자동으로 만들므로, 보통은 1~2번만 실행하면 됩니다.

-- 1. 데이터베이스와 전용 계정 만들기 (root 계정으로 한 번 실행)
CREATE DATABASE IF NOT EXISTS lostfound DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'lostfound'@'localhost' IDENTIFIED BY '여기에_비밀번호';
GRANT ALL PRIVILEGES ON lostfound.* TO 'lostfound'@'localhost';
FLUSH PRIVILEGES;

-- 2. (선택) 테이블 직접 만들기 — MySqlItemRepository.CREATE_TABLE과 같은 구조
USE lostfound;
CREATE TABLE IF NOT EXISTS found_items (
    id            VARCHAR(20)  NOT NULL PRIMARY KEY,
    item_type     VARCHAR(20)  NOT NULL,             -- ELECTRONICS / WALLET_CARD / BAG_CLOTHING / GENERAL
    color         VARCHAR(20)  NOT NULL,             -- ItemColor enum 이름
    found_date    DATE         NOT NULL,
    building      VARCHAR(30)  NOT NULL,             -- Building enum 이름
    floor_no      INT          NOT NULL,             -- 지하는 음수
    title         VARCHAR(100) NOT NULL,
    attr1         VARCHAR(100) NOT NULL DEFAULT '',  -- 종류별 고유 속성 1 (브랜드, 지갑 형태 등)
    attr2         VARCHAR(100) NOT NULL DEFAULT '',  -- 종류별 고유 속성 2 (모델명, 신분증 여부 Y/N 등)
    public_detail VARCHAR(500) NOT NULL DEFAULT '',
    secret_detail VARCHAR(500) NOT NULL DEFAULT '',  -- 소유자 확인용 비공개 특징
    photo_path    VARCHAR(300) NOT NULL DEFAULT '',
    status        VARCHAR(20)  NOT NULL,             -- STORED / CHECKING / RETURNED / EXPIRED / DISPOSED
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) DEFAULT CHARSET = utf8mb4;

-- 3. 확인용 쿼리
-- SELECT id, title, status, updated_at FROM found_items ORDER BY updated_at DESC;
-- SELECT status, COUNT(*) FROM found_items GROUP BY status;
