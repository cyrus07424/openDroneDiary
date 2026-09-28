-- v006_add_aircraft_and_compliance_fields.sql
-- 機体台帳と、飛行日誌ガイドライン（様式1〜3）向けの列を追加する（PostgreSQL用）
-- アプリ起動時の SchemaUtils.createMissingTablesAndColumns でも同じ列が足される。
-- 既存環境ではどちらか一方で足りる。制約名が衝突する場合は再実行しないこと。

CREATE TABLE IF NOT EXISTS Aircrafts (
    id SERIAL PRIMARY KEY,
    registration_symbol VARCHAR(32) NOT NULL,
    user_id INTEGER NOT NULL,
    manufacturer VARCHAR(100) NULL,
    model_name VARCHAR(100) NULL,
    serial_number VARCHAR(100) NULL,
    category VARCHAR(100) NULL,
    type_certificate_number VARCHAR(100) NULL,
    aircraft_certificate_class VARCHAR(20) NULL,
    aircraft_certificate_number VARCHAR(100) NULL,
    initial_total_minutes INTEGER NOT NULL DEFAULT 0,
    maintenance_interval_hours INTEGER NOT NULL DEFAULT 20,
    notes VARCHAR(1000) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aircrafts_users FOREIGN KEY (user_id) REFERENCES Users(id)
);

CREATE INDEX IF NOT EXISTS idx_aircrafts_user_id ON Aircrafts(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_aircrafts_user_registration ON Aircrafts(user_id, registration_symbol);

ALTER TABLE Pilots ADD COLUMN IF NOT EXISTS skill_certificate_number VARCHAR(50) NULL;

ALTER TABLE FlightLogs ADD COLUMN IF NOT EXISTS aircraft_id INTEGER NULL;
ALTER TABLE FlightLogs ADD COLUMN IF NOT EXISTS flight_purpose VARCHAR(500) NULL;
ALTER TABLE FlightLogs ADD COLUMN IF NOT EXISTS flight_route VARCHAR(1000) NULL;
ALTER TABLE FlightLogs ADD COLUMN IF NOT EXISTS specific_flight VARCHAR(500) NULL;
ALTER TABLE FlightLogs ADD COLUMN IF NOT EXISTS safety_matters VARCHAR(1000) NULL;
ALTER TABLE FlightLogs ADD COLUMN IF NOT EXISTS skill_certificate_number VARCHAR(50) NULL;
ALTER TABLE FlightLogs ADD COLUMN IF NOT EXISTS permission_number VARCHAR(100) NULL;
ALTER TABLE FlightLogs ADD COLUMN IF NOT EXISTS cumulative_flight_minutes INTEGER NULL;

ALTER TABLE FlightLogs DROP CONSTRAINT IF EXISTS fk_flightlogs_aircraft_id;
ALTER TABLE FlightLogs
    ADD CONSTRAINT fk_flightlogs_aircraft_id FOREIGN KEY (aircraft_id) REFERENCES Aircrafts(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_flightlogs_aircraft_id ON FlightLogs(aircraft_id);

ALTER TABLE DailyInspectionRecords ADD COLUMN IF NOT EXISTS aircraft_id INTEGER NULL;
ALTER TABLE DailyInspectionRecords ADD COLUMN IF NOT EXISTS airframe_result VARCHAR(20) NULL;
ALTER TABLE DailyInspectionRecords ADD COLUMN IF NOT EXISTS propeller_result VARCHAR(20) NULL;
ALTER TABLE DailyInspectionRecords ADD COLUMN IF NOT EXISTS frame_result VARCHAR(20) NULL;
ALTER TABLE DailyInspectionRecords ADD COLUMN IF NOT EXISTS communication_result VARCHAR(20) NULL;
ALTER TABLE DailyInspectionRecords ADD COLUMN IF NOT EXISTS propulsion_result VARCHAR(20) NULL;
ALTER TABLE DailyInspectionRecords ADD COLUMN IF NOT EXISTS power_result VARCHAR(20) NULL;
ALTER TABLE DailyInspectionRecords ADD COLUMN IF NOT EXISTS automatic_control_result VARCHAR(20) NULL;
ALTER TABLE DailyInspectionRecords ADD COLUMN IF NOT EXISTS controller_result VARCHAR(20) NULL;
ALTER TABLE DailyInspectionRecords ADD COLUMN IF NOT EXISTS battery_result VARCHAR(20) NULL;

ALTER TABLE DailyInspectionRecords DROP CONSTRAINT IF EXISTS fk_dailyinspections_aircraft_id;
ALTER TABLE DailyInspectionRecords
    ADD CONSTRAINT fk_dailyinspections_aircraft_id FOREIGN KEY (aircraft_id) REFERENCES Aircrafts(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_dailyinspections_aircraft_id ON DailyInspectionRecords(aircraft_id);

ALTER TABLE MaintenanceInspectionRecords ADD COLUMN IF NOT EXISTS aircraft_id INTEGER NULL;
ALTER TABLE MaintenanceInspectionRecords ADD COLUMN IF NOT EXISTS total_flight_time VARCHAR(20) NULL;

ALTER TABLE MaintenanceInspectionRecords DROP CONSTRAINT IF EXISTS fk_maintenance_aircraft_id;
ALTER TABLE MaintenanceInspectionRecords
    ADD CONSTRAINT fk_maintenance_aircraft_id FOREIGN KEY (aircraft_id) REFERENCES Aircrafts(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_maintenance_aircraft_id ON MaintenanceInspectionRecords(aircraft_id);
