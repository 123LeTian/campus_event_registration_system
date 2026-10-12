INSERT INTO `user` (`username`, `password`, `name`, `role`, `enabled`) VALUES
    ('admin', '$2a$10$wFClXBvLJYEMUGeTqHnGgu4nM.cHxDTdSSPpXzsfhJ7w45aYcMEoW', '系统管理员', 'ADMIN', 1),
    ('organizer1', '$2a$10$QM5uqnvJ54.eh6egvsRwXOioY//Lv70Iet13fYqmNL9.VufAy8bYO', '活动组织者', 'ORGANIZER', 1),
    ('studentA', '$2a$10$.S4yRoXHOHdy2M..vNSdbea55HQCYaJQkz2UsUPAIJkolnHbzRTO6', '学生A', 'STUDENT', 1),
    ('disabledUser', '$2a$10$.S4yRoXHOHdy2M..vNSdbea55HQCYaJQkz2UsUPAIJkolnHbzRTO6', '禁用用户', 'STUDENT', 0);
