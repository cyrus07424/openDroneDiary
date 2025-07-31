-- v002_make_email_unique_and_required.sql
-- メールアドレスをユニークにし、必須項目とする

-- まず、NULL値のメールアドレスを持つユーザーがいる場合、一時的な値を設定
-- （実際の運用では、このようなユーザーは手動で処理が必要）
UPDATE Users SET email = CONCAT('placeholder_', id, '@example.com') WHERE email IS NULL OR email = '';

-- メールアドレス列にユニーク制約を追加
-- PostgreSQL/MySQL syntax:
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON Users(email);

-- SQLite syntax (alternative):
-- CREATE UNIQUE INDEX idx_users_email ON Users(email);

-- メールアドレス列をNOT NULLに変更
-- PostgreSQL syntax:
-- ALTER TABLE Users ALTER COLUMN email SET NOT NULL;

-- MySQL syntax:
-- ALTER TABLE Users MODIFY COLUMN email VARCHAR(255) NOT NULL;

-- SQLite doesn't support ALTER COLUMN NOT NULL directly, 
-- but the Exposed framework will handle this constraint at the application level