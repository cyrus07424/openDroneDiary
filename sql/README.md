# Database Migrations

このディレクトリはデータベースの変更を管理するためのSQLファイルを格納します。

## Structure

- `migrations/` - データベーススキーマ変更のためのSQLファイル
- ファイル命名規則: `v{version}_{description}.sql`

## Migration Files

### v001_add_timestamp_columns.sql
- すべてのテーブルに `created_at` および `updated_at` カラムを追加

### v002 から v005
- メール一意化、座標、パイロット、登録トークン

### v006_add_aircraft_and_compliance_fields.sql
- 機体台帳 `Aircrafts` と、飛行記録・日常点検・点検整備・パイロットの法令対応列を追加
- アプリ起動時の `SchemaUtils.createMissingTablesAndColumns` でも同じ列が追加される

## Usage

これらのSQLファイルは手動でデータベースに適用する必要があります。
既存のPostgreSQLでは、アプリ起動だけでも不足列は追加されます。制約まで揃える場合は v006 を一度適用してください。
将来的にはマイグレーションツールの統合を検討する予定です。