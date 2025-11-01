CREATE DATABASE IF NOT EXISTS clinic CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clinic;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` VARCHAR(64) PRIMARY KEY,
    `username` VARCHAR(128) NOT NULL UNIQUE,
    `passwordHash` VARCHAR(128) NOT NULL,
    `role` VARCHAR(32) NOT NULL,
    `createdAt` DATETIME
);

DROP TABLE IF EXISTS `patients`;
CREATE TABLE `patients` (
    `id` VARCHAR(64) PRIMARY KEY,
    `name` VARCHAR(128) NOT NULL,
    `gender` VARCHAR(16),
    `birthday` DATE,
    `phone` VARCHAR(64),
    `address` VARCHAR(255),
    `emergencyContact` VARCHAR(128),
    `notes` TEXT
);

DROP TABLE IF EXISTS `doctors`;
CREATE TABLE `doctors` (
    `id` VARCHAR(64) PRIMARY KEY,
    `name` VARCHAR(128) NOT NULL,
    `department` VARCHAR(128),
    `phone` VARCHAR(64),
    `schedule` VARCHAR(255),
    `rating` DECIMAL(3,2),
    `title` VARCHAR(64),
    `level` VARCHAR(64),
    `specialties` TEXT
);

DROP TABLE IF EXISTS `appointments`;
CREATE TABLE `appointments` (
    `id` VARCHAR(64) PRIMARY KEY,
    `patientId` VARCHAR(64) NOT NULL,
    `doctorId` VARCHAR(64) NOT NULL,
    `datetime` DATETIME NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `notes` TEXT
);

DROP TABLE IF EXISTS `consultations`;
CREATE TABLE `consultations` (
    `id` VARCHAR(64) PRIMARY KEY,
    `patientId` VARCHAR(64) NOT NULL,
    `doctorId` VARCHAR(64) NOT NULL,
    `appointmentId` VARCHAR(64),
    `summary` TEXT,
    `prescriptionId` VARCHAR(64),
    `createdAt` DATETIME
);

DROP TABLE IF EXISTS `medicines`;
CREATE TABLE `medicines` (
    `id` VARCHAR(64) PRIMARY KEY,
    `name` VARCHAR(128) NOT NULL,
    `specification` VARCHAR(128),
    `stock` INT,
    `unit` VARCHAR(32),
    `expiryDate` DATE
);

DROP TABLE IF EXISTS `prescriptions`;
CREATE TABLE `prescriptions` (
    `id` VARCHAR(64) PRIMARY KEY,
    `consultationId` VARCHAR(64) NOT NULL,
    `medicineId` VARCHAR(64) NOT NULL,
    `quantity` INT NOT NULL,
    `usage` VARCHAR(255),
    `status` VARCHAR(32) NOT NULL
);

DROP TABLE IF EXISTS `expert_sessions`;
CREATE TABLE `expert_sessions` (
    `id` VARCHAR(64) PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `hostDoctorId` VARCHAR(64),
    `scheduledAt` DATETIME,
    `status` VARCHAR(32),
    `meetingUrl` VARCHAR(255),
    `notes` TEXT
);

DROP TABLE IF EXISTS `expert_participants`;
CREATE TABLE `expert_participants` (
    `sessionId` VARCHAR(64) NOT NULL,
    `participantId` VARCHAR(64) NOT NULL,
    `participantRole` VARCHAR(64),
    PRIMARY KEY (`sessionId`, `participantId`)
);

DROP TABLE IF EXISTS `meeting_minutes`;
CREATE TABLE `meeting_minutes` (
    `id` VARCHAR(64) PRIMARY KEY,
    `sessionId` VARCHAR(64) NOT NULL,
    `recordedAt` DATETIME,
    `authorDoctorId` VARCHAR(64),
    `summary` TEXT,
    `actionItems` TEXT
);

DROP TABLE IF EXISTS `expert_advices`;
CREATE TABLE `expert_advices` (
    `id` VARCHAR(64) PRIMARY KEY,
    `sessionId` VARCHAR(64),
    `patientId` VARCHAR(64) NOT NULL,
    `doctorId` VARCHAR(64),
    `adviceDate` DATETIME,
    `adviceSummary` TEXT,
    `followUpPlan` TEXT
);

DROP TABLE IF EXISTS `case_library`;
CREATE TABLE `case_library` (
    `id` VARCHAR(64) PRIMARY KEY,
    `patientId` VARCHAR(64),
    `title` VARCHAR(255) NOT NULL,
    `summary` TEXT,
    `tags` VARCHAR(255),
    `attachment` VARCHAR(255)
);

DROP TABLE IF EXISTS `work_progress`;
CREATE TABLE `work_progress` (
    `id` VARCHAR(64) PRIMARY KEY,
    `patientId` VARCHAR(64) NOT NULL,
    `description` TEXT,
    `status` VARCHAR(64),
    `lastUpdated` DATETIME,
    `ownerDoctorId` VARCHAR(64)
);

DROP TABLE IF EXISTS `calendar_events`;
CREATE TABLE `calendar_events` (
    `id` VARCHAR(64) PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `start` DATETIME NOT NULL,
    `end` DATETIME,
    `relatedPatientId` VARCHAR(64),
    `ownerDoctorId` VARCHAR(64),
    `location` VARCHAR(255),
    `notes` TEXT
);

DROP TABLE IF EXISTS `payments`;
CREATE TABLE `payments` (
    `id` VARCHAR(64) PRIMARY KEY,
    `patientId` VARCHAR(64) NOT NULL,
    `relatedType` VARCHAR(64) NOT NULL,
    `relatedId` VARCHAR(64),
    `amount` DECIMAL(12,2) NOT NULL,
    `currency` VARCHAR(16) NOT NULL,
    `method` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `insuranceClaimId` VARCHAR(64),
    `createdAt` DATETIME,
    `paidAt` DATETIME
);

DROP TABLE IF EXISTS `insurance_claims`;
CREATE TABLE `insurance_claims` (
    `id` VARCHAR(64) PRIMARY KEY,
    `paymentId` VARCHAR(64) NOT NULL,
    `insuranceType` VARCHAR(64),
    `coverageRatio` DECIMAL(5,2),
    `claimedAmount` DECIMAL(12,2),
    `approvedAmount` DECIMAL(12,2),
    `status` VARCHAR(32),
    `submittedAt` DATETIME,
    `processedAt` DATETIME,
    `notes` TEXT
);

DROP TABLE IF EXISTS `stock_movements`;
CREATE TABLE `stock_movements` (
    `id` VARCHAR(64) PRIMARY KEY,
    `medicineId` VARCHAR(64) NOT NULL,
    `movementType` VARCHAR(32) NOT NULL,
    `quantity` INT NOT NULL,
    `unitCost` DECIMAL(12,2),
    `totalCost` DECIMAL(12,2),
    `occurredAt` DATETIME,
    `referenceType` VARCHAR(64),
    `referenceId` VARCHAR(64),
    `operatorId` VARCHAR(64),
    `notes` TEXT
);

DROP TABLE IF EXISTS `audit_logs`;
CREATE TABLE `audit_logs` (
    `id` VARCHAR(64) PRIMARY KEY,
    `timestamp` DATETIME NOT NULL,
    `userId` VARCHAR(64),
    `role` VARCHAR(32),
    `action` VARCHAR(128) NOT NULL,
    `entityType` VARCHAR(64),
    `entityId` VARCHAR(64),
    `detail` TEXT,
    `result` VARCHAR(32),
    `ipAddress` VARCHAR(64)
);

SET FOREIGN_KEY_CHECKS = 1;
