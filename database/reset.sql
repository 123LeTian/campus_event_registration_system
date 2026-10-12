-- Campus Event Registration System - development reset script
-- This clears data from the three project tables only; it does not drop the database.
-- Run this script only against the intended campus_event development database.

USE `campus_event`;

DELETE FROM `registration`;
DELETE FROM `event`;
DELETE FROM `user`;

ALTER TABLE `registration` AUTO_INCREMENT = 1;
ALTER TABLE `event` AUTO_INCREMENT = 1;
ALTER TABLE `user` AUTO_INCREMENT = 1;
