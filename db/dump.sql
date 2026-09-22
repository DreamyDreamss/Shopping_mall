/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19-11.4.5-MariaDB, for Win64 (AMD64)
--
-- Host: 127.0.0.1    Database: sl_shop
-- ------------------------------------------------------
-- Server version	11.4.5-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Current Database: `sl_shop`
--

/*!40000 DROP DATABASE IF EXISTS `sl_shop`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `sl_shop` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;

USE `sl_shop`;

--
-- Table structure for table `cart_items`
--

DROP TABLE IF EXISTS `cart_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `cart_items` (
  `member_id` varchar(20) NOT NULL COMMENT '회원 ID (FK→MEMBERS)',
  `sku` varchar(20) NOT NULL COMMENT '상품 SKU (FK→PRODUCTS)',
  `qty` int(11) NOT NULL COMMENT '수량(1 이상 — 애플리케이션에서 강제, D4)',
  `added_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT '담은 일시',
  PRIMARY KEY (`member_id`,`sku`),
  KEY `fk_cart_product` (`sku`),
  CONSTRAINT `fk_cart_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`),
  CONSTRAINT `fk_cart_product` FOREIGN KEY (`sku`) REFERENCES `products` (`sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='장바구니 품목(SR-202) — 재담기는 PK로 합산(UPSERT), 재고는 차감하지 않음(보관 전용)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cart_items`
--

LOCK TABLES `cart_items` WRITE;
/*!40000 ALTER TABLE `cart_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `cart_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `id_sequences`
--

DROP TABLE IF EXISTS `id_sequences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `id_sequences` (
  `name` varchar(50) NOT NULL COMMENT '시퀀스 이름(예: MEMBER_ID) — 이 SR은 MEMBER_ID만 사용',
  `next_val` bigint(20) NOT NULL COMMENT '직전에 채번된 값(다음 채번은 INSERT..ON DUPLICATE KEY UPDATE로 이 값+1을 원자적으로 반환)',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='범용 원자 채번 테이블(SR-231 round2, FUNC-member-003) — AUTO_INCREMENT를 쓸 수 없는 접두 포맷 PK의 원자 채번용';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `id_sequences`
--

LOCK TABLES `id_sequences` WRITE;
/*!40000 ALTER TABLE `id_sequences` DISABLE KEYS */;
INSERT INTO `id_sequences` VALUES
('MEMBER_ID',717);
/*!40000 ALTER TABLE `id_sequences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member_addresses`
--

DROP TABLE IF EXISTS `member_addresses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_addresses` (
  `address_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` varchar(20) NOT NULL,
  `recipient` varchar(50) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `phone_norm` varchar(20) NOT NULL,
  `zipcode` varchar(10) NOT NULL,
  `road_address` varchar(200) NOT NULL,
  `detail_address` varchar(200) NOT NULL,
  `entrance_method` varchar(200) DEFAULT NULL,
  `delivery_memo` varchar(200) DEFAULT NULL,
  `is_default` char(1) NOT NULL DEFAULT 'N',
  `last_used_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `del_yn` char(1) NOT NULL DEFAULT 'N',
  PRIMARY KEY (`address_id`),
  KEY `fk_member_addresses_member` (`member_id`),
  CONSTRAINT `fk_member_addresses_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1850 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='회원 배송지(SR-235, FUNC-member-011) — 회원당 최대 10개·기본 1개';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_addresses`
--

LOCK TABLES `member_addresses` WRITE;
/*!40000 ALTER TABLE `member_addresses` DISABLE KEYS */;
/*!40000 ALTER TABLE `member_addresses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member_api_keys`
--

DROP TABLE IF EXISTS `member_api_keys`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_api_keys` (
  `member_id` varchar(20) NOT NULL COMMENT 'MEMBERS.member_id',
  `api_key` varchar(64) NOT NULL COMMENT '발급된 API 키(정적 lab.api-keys 맵과 별개 — DB 폴백 조회 전용)',
  `issued_at` datetime(3) NOT NULL COMMENT '최초 발급 시각',
  `revoked_at` datetime(3) DEFAULT NULL COMMENT '폐기 시각 — round 9(재작업 지시 2) 추가. 로그인(이 FUNC)은 항상 NULL로만 INSERT하고 세팅하지 않는다. 폐기는 FUNC-006(로그아웃) 소관 — 006이 로그아웃 시 이 컬럼을 세팅하는 인터페이스로 못박는다(STORY "006과의 인터페이스 가정" 참고).',
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uq_member_api_keys_api_key` (`api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='회원 API 키(SR-232, FUNC-member-005) — ApiKeyAuthFilter 정적 맵 미스 시 폴백 조회 대상';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_api_keys`
--

LOCK TABLES `member_api_keys` WRITE;
/*!40000 ALTER TABLE `member_api_keys` DISABLE KEYS */;
/*!40000 ALTER TABLE `member_api_keys` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member_login_attempts`
--

DROP TABLE IF EXISTS `member_login_attempts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_login_attempts` (
  `email` varchar(255) NOT NULL COMMENT '로그인 시도 대상 이메일(회원 미존재도 포함) — FUNC-member-005',
  `fail_count` int(11) NOT NULL DEFAULT 0 COMMENT '연속 실패 횟수(잠금 만료 후 재실패 시 1로 리셋 — 무한 누적 방지)',
  `locked_until` datetime(3) DEFAULT NULL COMMENT '이 시각까지 잠김(NULL이면 미잠김) — 5회 도달 시 세팅',
  `last_failed_at` datetime(3) DEFAULT NULL COMMENT '마지막 실패 시각',
  PRIMARY KEY (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='로그인 실패 카운터 + 잠금(SR-232, FUNC-member-005) — 5회 실패 시 10분 잠금';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_login_attempts`
--

LOCK TABLES `member_login_attempts` WRITE;
/*!40000 ALTER TABLE `member_login_attempts` DISABLE KEYS */;
/*!40000 ALTER TABLE `member_login_attempts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member_password_reset_rate_limits`
--

DROP TABLE IF EXISTS `member_password_reset_rate_limits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_password_reset_rate_limits` (
  `target` varchar(100) NOT NULL COMMENT '재설정 코드 요청 대상(이메일 원문) — target 단위로 일일 상한',
  `day_key` date NOT NULL COMMENT '요청 날짜(앱 시계 기준 로컬 날짜) — daily_count의 하루 경계',
  `daily_count` int(11) NOT NULL DEFAULT 1 COMMENT '이 target의 이 날짜에 허용된 요청 횟수(#2가 원자 UPSERT로 갱신 — 이 항목은 쓰지 않음)',
  `last_requested_at` datetime(3) NOT NULL COMMENT '이 target의 이 날짜 마지막으로 허용된 요청 시각(#2의 쿨다운 판정 기준 — 이 항목은 쓰지 않음)',
  `last_token` varchar(36) DEFAULT NULL COMMENT '#2가 조건부로 갱신할 요청 토큰(affected-rows 대신 재조회 판정용) — 이 항목은 쓰지 않음',
  PRIMARY KEY (`target`,`day_key`),
  KEY `idx_mprl_day_key` (`day_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='비밀번호 재설정 코드 요청 일일 상한 전용 카운터(SR-297 #1) — MEMBER_PASSWORD_RESETS(코드 테이블)와 완전히 분리된 락 범위';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_password_reset_rate_limits`
--

LOCK TABLES `member_password_reset_rate_limits` WRITE;
/*!40000 ALTER TABLE `member_password_reset_rate_limits` DISABLE KEYS */;
/*!40000 ALTER TABLE `member_password_reset_rate_limits` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member_password_resets`
--

DROP TABLE IF EXISTS `member_password_resets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_password_resets` (
  `target` varchar(100) NOT NULL COMMENT '재설정 대상(이메일 또는 휴대폰번호, 정규화된 값) — 단일 컬럼(email trim+소문자 또는 phone 숫자만)',
  `code_hash` char(64) NOT NULL COMMENT 'SHA-256 hex(소문자) — 코드 원문은 저장하지 않음',
  `expires_at` datetime(3) NOT NULL COMMENT '코드 만료 시각(발급+10분)',
  `consumed_at` datetime(3) DEFAULT NULL COMMENT '코드 소비(확정 완료) 시각 — 이 FUNC(008)은 항상 NULL로만 쓰고 세팅하지 않음. FUNC-009(확정 API) 소관',
  `attempt_count` int(11) NOT NULL DEFAULT 0 COMMENT '확정 시도 횟수 — 이 FUNC(008)은 항상 0으로만 두고 증가시키지 않음. FUNC-009(확정 API) 소관',
  `created_at` datetime(3) NOT NULL COMMENT '이 코드가 마지막으로 발급된 시각(쿨다운 60초 판정 기준) — ON DUPLICATE KEY UPDATE SET 목록의 맨 뒤에서만 갱신되어 앞선 IF 조건들이 항상 갱신 전 값을 참조한다(MEMBER_SIGNUP_RATE_LIMITS round6과 동일 원리)',
  PRIMARY KEY (`target`),
  KEY `idx_mpr_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='비밀번호 재설정 코드 요청(SR-234, FUNC-member-008) — target 단위 쿨다운(60초)·만료(10분) 관리';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_password_resets`
--

LOCK TABLES `member_password_resets` WRITE;
/*!40000 ALTER TABLE `member_password_resets` DISABLE KEYS */;
/*!40000 ALTER TABLE `member_password_resets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member_refresh_tokens`
--

DROP TABLE IF EXISTS `member_refresh_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_refresh_tokens` (
  `token_hash` varchar(64) NOT NULL COMMENT 'SHA-256 hex(소문자) — 원문 미저장',
  `member_id` varchar(20) NOT NULL COMMENT 'MEMBERS.member_id',
  `issued_at` datetime(3) NOT NULL COMMENT '발급 시각',
  `expires_at` datetime(3) NOT NULL COMMENT '만료 시각(발급+30일)',
  `revoked_at` datetime(3) DEFAULT NULL COMMENT '폐기 시각(로그아웃) — FUNC-006 전용, 이 FUNC(005)은 항상 NULL로만 INSERT',
  PRIMARY KEY (`token_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='리프레시 토큰(SR-232, FUNC-member-005 소유 — FUNC-006이 조회/폐기만 재사용)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_refresh_tokens`
--

LOCK TABLES `member_refresh_tokens` WRITE;
/*!40000 ALTER TABLE `member_refresh_tokens` DISABLE KEYS */;
/*!40000 ALTER TABLE `member_refresh_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member_signup_rate_limits`
--

DROP TABLE IF EXISTS `member_signup_rate_limits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_signup_rate_limits` (
  `target` varchar(100) NOT NULL COMMENT '인증 대상(이메일 또는 휴대폰번호 원문) — 채널 무관, target 단위로 레이트리밋',
  `day_key` date NOT NULL COMMENT '요청 날짜(앱 시계 기준 로컬 날짜) — daily_count의 하루 경계',
  `daily_count` int(11) NOT NULL DEFAULT 1 COMMENT '이 target의 이 날짜에 허용된 요청 횟수(거부된 요청은 세지 않음)',
  `last_requested_at` datetime(3) NOT NULL COMMENT '이 target의 이 날짜 마지막으로 허용된 요청 시각(쿨다운 60초 판정 기준)',
  `last_token` varchar(36) DEFAULT NULL COMMENT 'round5 — 마지막으로 "허용"을 기록한 요청의 UUID 토큰. touchRateLimit이 조건부로 갱신하고, selectRateLimit이 재조회한 값이 호출자 자신의 토큰과 같으면 허용(200)으로 판정',
  PRIMARY KEY (`target`,`day_key`),
  KEY `idx_msrl_day_key` (`day_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='회원가입 인증코드 레이트리밋 전용 카운터(SR-231 round4/round5) — 코드 테이블(MEMBER_SIGNUP_VERIFICATIONS)과 완전히 분리된 락 범위';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_signup_rate_limits`
--

LOCK TABLES `member_signup_rate_limits` WRITE;
/*!40000 ALTER TABLE `member_signup_rate_limits` DISABLE KEYS */;
/*!40000 ALTER TABLE `member_signup_rate_limits` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member_signup_verifications`
--

DROP TABLE IF EXISTS `member_signup_verifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_signup_verifications` (
  `channel` varchar(10) NOT NULL COMMENT '인증 채널: EMAIL/SMS',
  `target` varchar(100) NOT NULL COMMENT '인증 대상(이메일 주소 또는 휴대폰번호 원문)',
  `code` char(6) DEFAULT NULL COMMENT '6자리 인증코드 — 레이트리밋 판정을 통과한 요청에만 채워짐',
  `expires_at` datetime DEFAULT NULL COMMENT '만료 시각(코드 발급 시점 + 5분, 앱 시계 기준)',
  `verified_at` datetime DEFAULT NULL COMMENT '인증 확인 시각(가입완료 단계, FUNC-member-003에서 기록 예정)',
  `requested_at` datetime DEFAULT NULL COMMENT '(deprecated) 과거 버전 컬럼 — 더 이상 앱이 쓰지 않는다',
  `last_requested_at` datetime DEFAULT NULL COMMENT 'round4 — 의미 변경: 이 채널·타깃으로 코드가 마지막으로 실제 발송된 시각(정보용, writeCode가 갱신). round3까지는 레이트리밋 판정 기준이었으나 round4부터 그 역할은 MEMBER_SIGNUP_RATE_LIMITS로 이전',
  `previous_requested_at` datetime DEFAULT NULL COMMENT '(deprecated, round4) round3 레이트리밋 판정용 컬럼 — 카운터가 전용 테이블로 이전되어 더 이상 앱이 쓰지 않는다',
  `daily_count` int(11) NOT NULL DEFAULT 1 COMMENT '(deprecated, round4) round3 레이트리밋 판정용 컬럼 — MEMBER_SIGNUP_RATE_LIMITS.daily_count로 대체',
  `attempt_count` int(11) NOT NULL DEFAULT 0 COMMENT '인증코드 대입 시도 횟수 — 이 FUNC은 컬럼만 소유하고 쓰지 않음, FUNC-member-003(가입완료 API)이 코드 검증 시 사용',
  `consumed_at` datetime DEFAULT NULL COMMENT '코드 소비 시각(가입 성공 트랜잭션 안에서 기록, SR-231 round2, FUNC-member-003 전용) — 같은 코드의 재사용을 막는다',
  PRIMARY KEY (`channel`,`target`),
  KEY `idx_msv_expires_at` (`expires_at`),
  KEY `idx_msv_last_requested_at` (`last_requested_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='회원가입 인증코드(SR-231, FUNC-member-002/INF-MBR-001) — round4부터 레이트리밋 카운터는 별도 테이블(MEMBER_SIGNUP_RATE_LIMITS)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_signup_verifications`
--

LOCK TABLES `member_signup_verifications` WRITE;
/*!40000 ALTER TABLE `member_signup_verifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `member_signup_verifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `members`
--

DROP TABLE IF EXISTS `members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `members` (
  `member_id` varchar(20) NOT NULL COMMENT '회원 ID (M-접두)',
  `member_name` varchar(50) NOT NULL COMMENT '회원명',
  `grade` varchar(10) NOT NULL DEFAULT 'BRONZE' COMMENT '등급: BRONZE/SILVER/GOLD/VIP',
  `phone` varchar(20) DEFAULT NULL COMMENT '휴대폰(암호화 저장 대상 — 랩에서는 평문)',
  `del_yn` char(1) NOT NULL DEFAULT 'N' COMMENT '탈퇴 여부 (soft delete)',
  `created_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT '가입일시',
  `email` varchar(255) DEFAULT NULL COMMENT '이메일(SR-231 가입 채널이 이메일인 경우) — FUNC-member-003',
  `password_hash` varchar(100) DEFAULT NULL COMMENT 'BCrypt 비밀번호 해시(SR-231) — FUNC-member-003',
  `marketing_opt_in` tinyint(1) NOT NULL DEFAULT 0 COMMENT '마케팅 수신 동의(SR-231, 0/1) — FUNC-member-003',
  `phone_norm` varchar(20) DEFAULT NULL COMMENT '휴대폰번호 숫자만 정규화(SR-231 round2) — 하이픈 포함 기존 값과 신규 숫자만 값의 중복 판정을 이 컬럼 하나로 통일',
  `updated_at` datetime(3) DEFAULT NULL COMMENT '마지막 갱신 시각 — 현재는 비밀번호 재설정 확정(FUNC-member-009)만 세팅. 기존 행은 계속 NULL',
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uq_members_email` (`email`),
  UNIQUE KEY `uq_members_phone_norm` (`phone_norm`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='회원 마스터';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `members`
--

LOCK TABLES `members` WRITE;
/*!40000 ALTER TABLE `members` DISABLE KEYS */;
INSERT INTO `members` VALUES
('M-0001','김실증','GOLD','010-1111-2222','N','2026-09-15 06:54:58',NULL,NULL,0,'01011112222',NULL),
('M-0002','이도그','SILVER','010-3333-4444','N','2026-09-15 06:54:58',NULL,NULL,0,'01033334444',NULL),
('M-0003','박푸딩','BRONZE',NULL,'N','2026-09-15 06:54:58',NULL,NULL,0,NULL,NULL),
('M-0004','탈퇴한회원','BRONZE',NULL,'Y','2026-09-15 06:54:58',NULL,NULL,0,NULL,NULL);
/*!40000 ALTER TABLE `members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_delivery`
--

DROP TABLE IF EXISTS `order_delivery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_delivery` (
  `delivery_no` varchar(20) NOT NULL COMMENT '배송번호',
  `order_no` varchar(20) NOT NULL COMMENT '주문번호',
  `delivery_state` varchar(20) NOT NULL DEFAULT 'READY' COMMENT '상태: READY/SHIPPED/DELIVERED/CANCELED',
  `invoice_no` varchar(30) DEFAULT NULL COMMENT '송장번호',
  `shipped_at` datetime DEFAULT NULL COMMENT '출고일시',
  PRIMARY KEY (`delivery_no`),
  KEY `fk_delivery_order` (`order_no`),
  CONSTRAINT `fk_delivery_order` FOREIGN KEY (`order_no`) REFERENCES `orders` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='주문 배송';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_delivery`
--

LOCK TABLES `order_delivery` WRITE;
/*!40000 ALTER TABLE `order_delivery` DISABLE KEYS */;
INSERT INTO `order_delivery` VALUES
('D-0815-1','20260815-0001','DELIVERED','INV-88010','2026-08-15 18:00:00'),
('D-0816-1','20260816-0002','SHIPPED','INV-88031','2026-08-17 08:20:00'),
('D-0816-2','20260816-0002','READY',NULL,NULL);
/*!40000 ALTER TABLE `order_delivery` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
  `order_no` varchar(20) NOT NULL COMMENT '주문번호',
  `line_no` int(11) NOT NULL COMMENT '주문 라인 번호',
  `sku` varchar(20) NOT NULL COMMENT '상품 SKU',
  `qty` int(11) NOT NULL COMMENT '수량',
  `unit_price` bigint(20) NOT NULL COMMENT '주문 시점 단가(원)',
  PRIMARY KEY (`order_no`,`line_no`),
  KEY `fk_items_product` (`sku`),
  CONSTRAINT `fk_items_order` FOREIGN KEY (`order_no`) REFERENCES `orders` (`order_no`),
  CONSTRAINT `fk_items_product` FOREIGN KEY (`sku`) REFERENCES `products` (`sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='주문 상세(라인)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
INSERT INTO `order_items` VALUES
('20260815-0001',1,'SKU-1002',1,129000),
('20260815-0001',2,'SKU-1001',1,390000),
('20260816-0001',1,'SKU-1001',1,390000),
('20260816-0002',1,'SKU-1003',1,450000),
('20260816-0002',2,'SKU-1002',1,129000),
('20260817-0001',1,'SKU-1002',1,129000),
('20260817-0002',1,'SKU-1003',1,450000);
/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `order_no` varchar(20) NOT NULL COMMENT '주문번호 (yyyymmdd+seq)',
  `member_id` varchar(20) NOT NULL COMMENT '주문 회원',
  `order_state` varchar(20) NOT NULL DEFAULT 'PLACED' COMMENT '상태: PLACED/PAID/SHIPPED/PARTIAL_SHIPPED/CANCELED/DONE',
  `total_amount` bigint(20) NOT NULL COMMENT '주문 총액(원)',
  `del_yn` char(1) NOT NULL DEFAULT 'N' COMMENT '논리 삭제 (soft delete — 전 조회 상시필터)',
  `ordered_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT '주문일시',
  PRIMARY KEY (`order_no`),
  KEY `fk_orders_member` (`member_id`),
  CONSTRAINT `fk_orders_member` FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='주문 마스터';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES
('20260815-0001','M-0001','DONE',519000,'N','2026-08-15 10:12:00'),
('20260816-0001','M-0002','PAID',390000,'N','2026-08-16 14:30:00'),
('20260816-0002','M-0001','PARTIAL_SHIPPED',579000,'N','2026-08-16 16:05:00'),
('20260817-0001','M-0003','PLACED',129000,'N','2026-08-17 09:00:00'),
('20260817-0002','M-0002','CANCELED',450000,'N','2026-08-17 09:40:00');
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `sku` varchar(20) NOT NULL COMMENT '상품 SKU',
  `product_name` varchar(100) NOT NULL COMMENT '상품명',
  `price` bigint(20) NOT NULL COMMENT '판매가(원)',
  `stock_qty` int(11) NOT NULL DEFAULT 0 COMMENT '가용 재고',
  `sale_yn` char(1) NOT NULL DEFAULT 'Y' COMMENT '판매 여부',
  `list_price` bigint(20) DEFAULT NULL COMMENT '정가(원, 표시전용) — 판매가(price)와 무관, NULL=정가 없음',
  `image_url` varchar(300) DEFAULT NULL COMMENT '대표 이미지 경로(앱 서빙 정적 경로, 예: /images/products/sku-1001.svg) — 외부 URL 금지',
  PRIMARY KEY (`sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='상품 마스터';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES
('SKU-1001','스탠딩 데스크',390000,12,'Y',450000,'/images/products/sku-1001.svg'),
('SKU-1002','기계식 키보드',129000,40,'Y',NULL,'/images/products/sku-1002.svg'),
('SKU-1003','4K 모니터',450000,7,'Y',450000,'/images/products/sku-1003.svg'),
('SKU-1004','단종 마우스',35000,0,'N',42000,NULL);
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `zipcodes`
--

DROP TABLE IF EXISTS `zipcodes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `zipcodes` (
  `zipcode_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `zipcode` varchar(10) NOT NULL,
  `road_address` varchar(200) NOT NULL,
  `sido` varchar(50) NOT NULL,
  `sigungu` varchar(50) NOT NULL,
  PRIMARY KEY (`zipcode_id`),
  UNIQUE KEY `uq_zipcodes_zipcode` (`zipcode`)
) ENGINE=InnoDB AUTO_INCREMENT=58942 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='우편번호(도로명) 샘플';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `zipcodes`
--

LOCK TABLES `zipcodes` WRITE;
/*!40000 ALTER TABLE `zipcodes` DISABLE KEYS */;
INSERT INTO `zipcodes` VALUES
(1,'10000','서울특별시 강남구 봉은사로 33길 99','서울특별시','강남구'),
(2,'10001','서울특별시 강남구 테헤란로 16길 10','서울특별시','강남구'),
(3,'10002','서울특별시 강남구 테헤란로 2길 33','서울특별시','강남구'),
(4,'10003','서울특별시 강남구 강남대로 4길 16','서울특별시','강남구'),
(5,'10004','서울특별시 강남구 테헤란로 30길 22','서울특별시','강남구'),
(6,'10005','서울특별시 강남구 강남대로 21길 56','서울특별시','강남구'),
(7,'10006','서울특별시 강남구 언주로 29길 94','서울특별시','강남구'),
(8,'10007','서울특별시 강남구 강남대로 36길 11','서울특별시','강남구'),
(9,'10008','서울특별시 서초구 강남대로 31길 66','서울특별시','서초구'),
(10,'10009','서울특별시 서초구 효령로 8길 59','서울특별시','서초구'),
(11,'10010','서울특별시 서초구 강남대로 6길 51','서울특별시','서초구'),
(12,'10011','서울특별시 서초구 서초대로 13길 71','서울특별시','서초구'),
(13,'10012','서울특별시 서초구 효령로 13길 48','서울특별시','서초구'),
(14,'10013','서울특별시 서초구 강남대로 22길 15','서울특별시','서초구'),
(15,'10014','서울특별시 마포구 월드컵로 31길 6','서울특별시','마포구'),
(16,'10015','서울특별시 마포구 와우산로 8길 79','서울특별시','마포구'),
(17,'10016','서울특별시 마포구 와우산로 26길 9','서울특별시','마포구'),
(18,'10017','서울특별시 마포구 동교로 19길 73','서울특별시','마포구'),
(19,'10018','서울특별시 마포구 와우산로 25길 27','서울특별시','마포구'),
(20,'10019','서울특별시 마포구 동교로 20길 45','서울특별시','마포구'),
(21,'10020','서울특별시 마포구 동교로 1길 51','서울특별시','마포구'),
(22,'10021','서울특별시 종로구 종로 4길 72','서울특별시','종로구'),
(23,'10022','서울특별시 종로구 자하문로 18길 71','서울특별시','종로구'),
(24,'10023','서울특별시 종로구 사직로 28길 40','서울특별시','종로구'),
(25,'10024','서울특별시 종로구 율곡로 35길 8','서울특별시','종로구'),
(26,'10025','서울특별시 종로구 사직로 5길 8','서울특별시','종로구'),
(27,'10026','서울특별시 종로구 자하문로 12길 54','서울특별시','종로구'),
(28,'10027','서울특별시 종로구 자하문로 21길 85','서울특별시','종로구'),
(29,'10028','경기도 수원시 권선로 31길 21','경기도','수원시'),
(30,'10029','경기도 수원시 인계로 9길 66','경기도','수원시'),
(31,'10030','경기도 수원시 매탄로 15길 78','경기도','수원시'),
(32,'10031','경기도 수원시 권선로 12길 82','경기도','수원시'),
(33,'10032','경기도 수원시 매탄로 26길 36','경기도','수원시'),
(34,'10033','경기도 수원시 팔달로 27길 31','경기도','수원시'),
(35,'10034','경기도 성남시 판교로 4길 57','경기도','성남시'),
(36,'10035','경기도 성남시 성남대로 11길 42','경기도','성남시'),
(37,'10036','경기도 성남시 분당로 22길 59','경기도','성남시'),
(38,'10037','경기도 성남시 성남대로 36길 33','경기도','성남시'),
(39,'10038','경기도 성남시 성남대로 34길 64','경기도','성남시'),
(40,'10039','경기도 성남시 분당로 18길 73','경기도','성남시'),
(41,'10040','경기도 고양시 중앙로 10길 95','경기도','고양시'),
(42,'10041','경기도 고양시 중앙로 22길 73','경기도','고양시'),
(43,'10042','경기도 고양시 킨텍스로 38길 75','경기도','고양시'),
(44,'10043','경기도 고양시 킨텍스로 29길 25','경기도','고양시'),
(45,'10044','경기도 고양시 호수로 16길 76','경기도','고양시'),
(46,'10045','경기도 고양시 일산로 21길 26','경기도','고양시'),
(47,'10046','경기도 고양시 킨텍스로 20길 40','경기도','고양시'),
(48,'10047','경기도 고양시 일산로 16길 77','경기도','고양시'),
(49,'10048','경기도 고양시 일산로 26길 89','경기도','고양시'),
(50,'10049','부산광역시 해운대구 우동로 21길 47','부산광역시','해운대구'),
(51,'10050','부산광역시 해운대구 송정로 6길 59','부산광역시','해운대구'),
(52,'10051','부산광역시 해운대구 해운대로 29길 50','부산광역시','해운대구'),
(53,'10052','부산광역시 해운대구 해운대로 29길 67','부산광역시','해운대구'),
(54,'10053','부산광역시 해운대구 센텀중앙로 20길 1','부산광역시','해운대구'),
(55,'10054','부산광역시 해운대구 우동로 25길 39','부산광역시','해운대구'),
(56,'10055','부산광역시 해운대구 송정로 31길 6','부산광역시','해운대구'),
(57,'10056','부산광역시 해운대구 우동로 15길 14','부산광역시','해운대구'),
(58,'10057','부산광역시 부산진구 중앙대로 34길 5','부산광역시','부산진구'),
(59,'10058','부산광역시 부산진구 가야대로 22길 58','부산광역시','부산진구'),
(60,'10059','부산광역시 부산진구 동평로 27길 46','부산광역시','부산진구'),
(61,'10060','부산광역시 부산진구 중앙대로 37길 55','부산광역시','부산진구'),
(62,'10061','부산광역시 부산진구 동평로 20길 65','부산광역시','부산진구'),
(63,'10062','부산광역시 부산진구 가야대로 21길 79','부산광역시','부산진구'),
(64,'10063','부산광역시 부산진구 중앙대로 22길 20','부산광역시','부산진구'),
(65,'10064','부산광역시 부산진구 동평로 33길 88','부산광역시','부산진구'),
(66,'10065','대구광역시 수성구 수성로 10길 6','대구광역시','수성구'),
(67,'10066','대구광역시 수성구 달구벌대로 2길 82','대구광역시','수성구'),
(68,'10067','대구광역시 수성구 수성로 20길 38','대구광역시','수성구'),
(69,'10068','대구광역시 수성구 동대구로 2길 15','대구광역시','수성구'),
(70,'10069','대구광역시 수성구 달구벌대로 32길 15','대구광역시','수성구'),
(71,'10070','대구광역시 수성구 수성로 24길 78','대구광역시','수성구'),
(72,'10071','인천광역시 연수구 컨벤시아대로 6길 34','인천광역시','연수구'),
(73,'10072','인천광역시 연수구 컨벤시아대로 30길 37','인천광역시','연수구'),
(74,'10073','인천광역시 연수구 컨벤시아대로 9길 91','인천광역시','연수구'),
(75,'10074','인천광역시 연수구 연수로 10길 4','인천광역시','연수구'),
(76,'10075','인천광역시 연수구 연수로 32길 23','인천광역시','연수구'),
(77,'10076','인천광역시 연수구 컨벤시아대로 11길 49','인천광역시','연수구'),
(78,'10077','인천광역시 연수구 컨벤시아대로 9길 45','인천광역시','연수구'),
(79,'10078','인천광역시 남동구 정각로 29길 33','인천광역시','남동구'),
(80,'10079','인천광역시 남동구 구월로 32길 2','인천광역시','남동구'),
(81,'10080','인천광역시 남동구 남동대로 21길 53','인천광역시','남동구'),
(82,'10081','인천광역시 남동구 구월로 26길 74','인천광역시','남동구'),
(83,'10082','인천광역시 남동구 정각로 1길 52','인천광역시','남동구'),
(84,'10083','인천광역시 남동구 구월로 4길 92','인천광역시','남동구'),
(85,'10084','인천광역시 남동구 정각로 40길 77','인천광역시','남동구'),
(86,'10085','서울특별시 강남구 테헤란로 39길 71','서울특별시','강남구'),
(87,'10086','서울특별시 강남구 테헤란로 38길 68','서울특별시','강남구'),
(88,'10087','서울특별시 강남구 테헤란로 30길 25','서울특별시','강남구'),
(89,'10088','서울특별시 강남구 테헤란로 15길 42','서울특별시','강남구'),
(90,'10089','서울특별시 강남구 테헤란로 25길 36','서울특별시','강남구'),
(91,'10090','서울특별시 강남구 테헤란로 19길 32','서울특별시','강남구'),
(92,'10091','서울특별시 강남구 테헤란로 26길 86','서울특별시','강남구'),
(93,'10092','서울특별시 강남구 테헤란로 23길 72','서울특별시','강남구'),
(94,'10093','서울특별시 강남구 테헤란로 36길 15','서울특별시','강남구'),
(95,'10094','서울특별시 강남구 테헤란로 14길 9','서울특별시','강남구'),
(96,'10095','서울특별시 강남구 테헤란로 40길 23','서울특별시','강남구'),
(97,'10096','서울특별시 강남구 테헤란로 14길 56','서울특별시','강남구'),
(98,'10097','서울특별시 강남구 테헤란로 24길 63','서울특별시','강남구'),
(99,'10098','서울특별시 강남구 테헤란로 22길 84','서울특별시','강남구'),
(100,'10099','서울특별시 강남구 테헤란로 21길 77','서울특별시','강남구');
/*!40000 ALTER TABLE `zipcodes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'sl_shop'
--

--
-- Dumping routines for database 'sl_shop'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- Dump completed on 2026-09-22  6:33:05
