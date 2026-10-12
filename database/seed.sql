-- Campus Event Registration System - Phase 2 demo users
-- Passwords are stored as BCrypt hashes only. This script does not insert events or registrations.

USE `campus_event`;

INSERT INTO `user` (`username`, `password`, `name`, `role`, `enabled`) VALUES
    ('admin', '$2a$10$wFClXBvLJYEMUGeTqHnGgu4nM.cHxDTdSSPpXzsfhJ7w45aYcMEoW', '系统管理员', 'ADMIN', 1),
    ('organizer1', '$2a$10$QM5uqnvJ54.eh6egvsRwXOioY//Lv70Iet13fYqmNL9.VufAy8bYO', '活动组织者', 'ORGANIZER', 1),
    ('studentA', '$2a$10$.S4yRoXHOHdy2M..vNSdbea55HQCYaJQkz2UsUPAIJkolnHbzRTO6', '学生A', 'STUDENT', 1),
    ('studentB', '$2a$10$9ShZII34NjuC6ZRprfh/0ext48qNuPV2hm7/3XUYIepqzy8MNo3Ze', '学生B', 'STUDENT', 1),
    ('studentC', '$2a$10$Ny/0ex3ibw.NFqghCLY5UOCH62LzHdSWm6QyhW2Ps/XcrLyl0AUFe', '学生C', 'STUDENT', 1),
    ('studentD', '$2a$10$pnFOUPOK4P9S5Ru2DUYt3.JJKPTM7nOu42YGvxFkjeIWyaMHq4evm', '学生D', 'STUDENT', 1)
ON DUPLICATE KEY UPDATE
    `password` = VALUES(`password`),
    `name` = VALUES(`name`),
    `role` = VALUES(`role`),
    `enabled` = VALUES(`enabled`),
    `updated_at` = CURRENT_TIMESTAMP;
