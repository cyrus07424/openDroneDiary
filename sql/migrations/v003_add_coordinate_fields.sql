-- v003_add_coordinate_fields.sql
-- 飛行記録テーブルに座標情報と入力種別フィールドを追加

-- FlightLogs table に座標と入力種別の列を追加
ALTER TABLE FlightLogs ADD COLUMN takeoff_input_type VARCHAR(20) DEFAULT 'text';
ALTER TABLE FlightLogs ADD COLUMN landing_input_type VARCHAR(20) DEFAULT 'text';
ALTER TABLE FlightLogs ADD COLUMN takeoff_latitude DECIMAL(10, 8) NULL;
ALTER TABLE FlightLogs ADD COLUMN takeoff_longitude DECIMAL(11, 8) NULL;
ALTER TABLE FlightLogs ADD COLUMN landing_latitude DECIMAL(10, 8) NULL;
ALTER TABLE FlightLogs ADD COLUMN landing_longitude DECIMAL(11, 8) NULL;

-- 既存レコードの入力種別をテキストに設定
UPDATE FlightLogs SET takeoff_input_type = 'text', landing_input_type = 'text' WHERE takeoff_input_type IS NULL OR landing_input_type IS NULL;