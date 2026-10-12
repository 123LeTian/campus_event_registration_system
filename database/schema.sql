-- Campus Event Registration System - Phase 2 schema
-- MySQL 8.x / utf8mb4 / InnoDB

CREATE DATABASE IF NOT EXISTS `campus_event`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `campus_event`;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'User ID',
    `username` VARCHAR(50) NOT NULL COMMENT 'Login account',
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt password hash',
    `name` VARCHAR(50) NOT NULL COMMENT 'Display name',
    `role` VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT 'STUDENT, ORGANIZER, or ADMIN',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT 'Whether the account is enabled',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    KEY `idx_user_role` (`role`),
    CONSTRAINT `chk_user_role` CHECK (`role` IN ('STUDENT', 'ORGANIZER', 'ADMIN')),
    CONSTRAINT `chk_user_enabled` CHECK (`enabled` IN (0, 1))
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System users';

CREATE TABLE IF NOT EXISTS `event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Event ID',
    `title` VARCHAR(100) NOT NULL COMMENT 'Event title',
    `copywriting` VARCHAR(300) NOT NULL COMMENT 'Event short copywriting',
    `description` TEXT NOT NULL COMMENT 'Confirmed event introduction',
    `location` VARCHAR(100) NOT NULL COMMENT 'Event location',
    `organizer_id` BIGINT NOT NULL COMMENT 'Creator user ID',
    `start_time` DATETIME NOT NULL COMMENT 'Event start time',
    `end_time` DATETIME NOT NULL COMMENT 'Event end time',
    `registration_deadline` DATETIME NOT NULL COMMENT 'Registration deadline',
    `max_participants` INT NOT NULL COMMENT 'Maximum participant count',
    `current_participants` INT NOT NULL DEFAULT 0 COMMENT 'Current active registration count',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'Phase 2 uses PUBLISHED only',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Soft-delete flag',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_event_title` (`title`),
    KEY `idx_event_location` (`location`),
    KEY `idx_event_organizer` (`organizer_id`),
    KEY `idx_event_status_deleted` (`status`, `deleted`),
    KEY `idx_event_start_time` (`start_time`),
    KEY `idx_event_registration_deadline` (`registration_deadline`),
    KEY `idx_event_end_time` (`end_time`),
    CONSTRAINT `fk_event_organizer` FOREIGN KEY (`organizer_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `chk_event_max_participants` CHECK (`max_participants` > 0),
    CONSTRAINT `chk_event_current_participants` CHECK (`current_participants` >= 0),
    CONSTRAINT `chk_event_participant_capacity` CHECK (`current_participants` <= `max_participants`),
    CONSTRAINT `chk_event_time_range` CHECK (`end_time` > `start_time`),
    CONSTRAINT `chk_event_registration_deadline` CHECK (`registration_deadline` < `start_time`),
    CONSTRAINT `chk_event_status` CHECK (`status` = 'PUBLISHED'),
    CONSTRAINT `chk_event_deleted` CHECK (`deleted` IN (0, 1))
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Campus events';

CREATE TABLE IF NOT EXISTS `registration` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Registration ID',
    `event_id` BIGINT NOT NULL COMMENT 'Event ID',
    `user_id` BIGINT NOT NULL COMMENT 'Student user ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE or CANCELLED',
    `registration_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Most recent successful registration time',
    `cancelled_time` DATETIME NULL COMMENT 'Most recent cancellation time',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_registration_event_user` (`event_id`, `user_id`),
    KEY `idx_registration_event_status` (`event_id`, `status`),
    KEY `idx_registration_user_status` (`user_id`, `status`),
    CONSTRAINT `fk_registration_event` FOREIGN KEY (`event_id`) REFERENCES `event` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `fk_registration_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT `chk_registration_status` CHECK (`status` IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT `chk_registration_cancelled_time` CHECK (
        (`status` = 'ACTIVE' AND `cancelled_time` IS NULL)
        OR (`status` = 'CANCELLED' AND `cancelled_time` IS NOT NULL)
    )
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Event registrations';
