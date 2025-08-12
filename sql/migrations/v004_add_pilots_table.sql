-- v004_add_pilots_table.sql
-- パイロット管理機能のためのPilotsテーブルとFlightLogsテーブルの拡張

-- Pilotsテーブルの作成
CREATE TABLE Pilots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(100) NOT NULL,
    user_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(id)
);

-- FlightLogsテーブルにpilot_idカラムを追加（既存データとの互換性のためnullable）
ALTER TABLE FlightLogs ADD COLUMN pilot_id INTEGER NULL;

-- FlightLogsテーブルのpilot_idにPilotsテーブルへの外部キー制約を追加
-- SQLiteでは後から外部キー制約を追加することができないため、
-- Exposedフレームワークがアプリケーションレベルで制約を管理します

-- Pilotsテーブルのuser_idにインデックスを作成（検索性能向上のため）
CREATE INDEX idx_pilots_user_id ON Pilots(user_id);

-- FlightLogsテーブルのpilot_idにインデックスを作成（検索性能向上のため）
CREATE INDEX idx_flightlogs_pilot_id ON FlightLogs(pilot_id);