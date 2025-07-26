# Database Migrations

このディレクトリはデータベースの変更を管理するためのSQLファイルを格納します。

## Structure

- `migrations/` - データベーススキーマ変更のためのSQLファイル
- ファイル命名規則: `v{version}_{description}.sql`

## Migration Files

### v001_add_timestamp_columns.sql
- すべてのテーブルに `created_at` および `updated_at` カラムを追加

## Usage

これらのSQLファイルは手動でデータベースに適用する必要があります。
将来的にはマイグレーションツールの統合を検討する予定です。