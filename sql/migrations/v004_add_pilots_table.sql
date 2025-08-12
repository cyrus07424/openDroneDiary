-- v004_add_pilots_table_postgres.sql
-- パイロット管理機能のためのPilotsテーブルとFlightLogsテーブルの拡張（PostgreSQL用）

-- Pilotsテーブルの作成
CREATE TABLE Pilots (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    user_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pilots_users FOREIGN KEY (user_id) REFERENCES users(id)
);

-- FlightLogsテーブルにpilot_idカラムを追加（既存データとの互換性のためnullable）
ALTER TABLE FlightLogs ADD COLUMN pilot_id INTEGER NULL;

-- FlightLogsテーブルのpilot_idにPilotsテーブルへの外部キー制約を追加
ALTER TABLE FlightLogs
    ADD CONSTRAINT fk_flightlogs_pilot_id FOREIGN KEY (pilot_id) REFERENCES Pilots(id);

-- Pilotsテーブルのuser_idにインデックスを作成（検索性能向上のため）
CREATE INDEX idx_pilots_user_id ON Pilots(user_id);

-- FlightLogsテーブルのpilot_idにインデックスを作成（検索性能向上のため）
CREATE INDEX idx_flightlogs_pilot_id ON FlightLogs(pilot_id);
