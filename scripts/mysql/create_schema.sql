CREATE DATABASE IF NOT EXISTS clinic CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clinic;

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at DATETIME
);

CREATE TABLE IF NOT EXISTS patients (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    gender VARCHAR(16),
    birthday DATE,
    phone VARCHAR(64),
    address VARCHAR(255),
    emergency_contact VARCHAR(128),
    notes TEXT
);

CREATE TABLE IF NOT EXISTS doctors (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    department VARCHAR(128),
    phone VARCHAR(64),
    schedule VARCHAR(255),
    rating DECIMAL(3,2),
    title VARCHAR(64),
    level VARCHAR(64),
    specialties TEXT
);

CREATE TABLE IF NOT EXISTS appointments (
    id VARCHAR(64) PRIMARY KEY,
    patient_id VARCHAR(64) NOT NULL,
    doctor_id VARCHAR(64) NOT NULL,
    datetime DATETIME NOT NULL,
    status VARCHAR(32) NOT NULL,
    notes TEXT,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);

CREATE TABLE IF NOT EXISTS consultations (
    id VARCHAR(64) PRIMARY KEY,
    patient_id VARCHAR(64) NOT NULL,
    doctor_id VARCHAR(64) NOT NULL,
    appointment_id VARCHAR(64),
    summary TEXT,
    prescription_id VARCHAR(64),
    created_at DATETIME,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);

CREATE TABLE IF NOT EXISTS medicines (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    specification VARCHAR(128),
    stock INT,
    unit VARCHAR(32),
    expiry_date DATE
);

CREATE TABLE IF NOT EXISTS prescriptions (
    id VARCHAR(64) PRIMARY KEY,
    consultation_id VARCHAR(64) NOT NULL,
    medicine_id VARCHAR(64) NOT NULL,
    quantity INT NOT NULL,
    `usage` VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    FOREIGN KEY (consultation_id) REFERENCES consultations(id),
    FOREIGN KEY (medicine_id) REFERENCES medicines(id)
);

CREATE TABLE IF NOT EXISTS expert_sessions (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    host_doctor_id VARCHAR(64),
    scheduled_at DATETIME,
    status VARCHAR(32),
    meeting_url VARCHAR(255),
    notes TEXT
);

CREATE TABLE IF NOT EXISTS expert_participants (
    session_id VARCHAR(64) NOT NULL,
    doctor_id VARCHAR(64) NOT NULL,
    role VARCHAR(64),
    PRIMARY KEY (session_id, doctor_id)
);

CREATE TABLE IF NOT EXISTS meeting_minutes (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    author_doctor_id VARCHAR(64),
    created_at DATETIME,
    content TEXT,
    action_items TEXT
);

CREATE TABLE IF NOT EXISTS expert_advices (
    id VARCHAR(64) PRIMARY KEY,
    patient_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64),
    summary TEXT,
    recommendations TEXT,
    created_at DATETIME
);

CREATE TABLE IF NOT EXISTS case_library (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64),
    summary TEXT,
    recommendations TEXT,
    last_updated DATETIME
);

CREATE TABLE IF NOT EXISTS work_progress (
    id VARCHAR(64) PRIMARY KEY,
    patient_id VARCHAR(64) NOT NULL,
    description TEXT,
    status VARCHAR(64),
    last_updated DATETIME,
    owner_doctor_id VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS calendar_events (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    start DATETIME NOT NULL,
    end DATETIME,
    related_patient_id VARCHAR(64),
    owner_doctor_id VARCHAR(64),
    location VARCHAR(255),
    notes TEXT
);

CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(64) PRIMARY KEY,
    patient_id VARCHAR(64) NOT NULL,
    related_type VARCHAR(64) NOT NULL,
    related_id VARCHAR(64),
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(16) NOT NULL,
    method VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    insurance_claim_id VARCHAR(64),
    created_at DATETIME,
    paid_at DATETIME
);

CREATE TABLE IF NOT EXISTS insurance_claims (
    id VARCHAR(64) PRIMARY KEY,
    payment_id VARCHAR(64) NOT NULL,
    insurance_type VARCHAR(64),
    coverage_ratio DECIMAL(5,2),
    claimed_amount DECIMAL(12,2),
    approved_amount DECIMAL(12,2),
    status VARCHAR(32),
    created_at DATETIME,
    processed_at DATETIME,
    notes TEXT
);

CREATE TABLE IF NOT EXISTS stock_movements (
    id VARCHAR(64) PRIMARY KEY,
    medicine_id VARCHAR(64) NOT NULL,
    movement_type VARCHAR(32) NOT NULL,
    quantity INT NOT NULL,
    unit_cost DECIMAL(12,2),
    total_cost DECIMAL(12,2),
    occurred_at DATETIME,
    reference_type VARCHAR(64),
    reference_id VARCHAR(64),
    operator_id VARCHAR(64),
    notes TEXT
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    `timestamp` DATETIME NOT NULL,
    user_id VARCHAR(64),
    role VARCHAR(32),
    action VARCHAR(128) NOT NULL,
    entity_type VARCHAR(64),
    entity_id VARCHAR(64),
    detail TEXT,
    result VARCHAR(32),
    ip_address VARCHAR(64)
);
