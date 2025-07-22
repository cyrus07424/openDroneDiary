-- v001_add_timestamp_columns.sql
-- すべてのテーブルに created_at と updated_at カラムを追加

-- Users table
ALTER TABLE Users ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE Users ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- FlightLogs table  
ALTER TABLE FlightLogs ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE FlightLogs ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- DailyInspectionRecords table
ALTER TABLE DailyInspectionRecords ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE DailyInspectionRecords ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- MaintenanceInspectionRecords table
ALTER TABLE MaintenanceInspectionRecords ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE MaintenanceInspectionRecords ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- PasswordResetTokens table
ALTER TABLE PasswordResetTokens ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE PasswordResetTokens ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 既存レコードの updated_at を created_at と同じ値に設定
UPDATE Users SET updated_at = created_at WHERE updated_at = created_at;
UPDATE FlightLogs SET updated_at = created_at WHERE updated_at = created_at;
UPDATE DailyInspectionRecords SET updated_at = created_at WHERE updated_at = created_at;
UPDATE MaintenanceInspectionRecords SET updated_at = created_at WHERE updated_at = created_at;
UPDATE PasswordResetTokens SET updated_at = created_at WHERE updated_at = created_at;