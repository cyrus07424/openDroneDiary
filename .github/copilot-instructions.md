# Ktor リポジトリ開発ガイド

このリポジトリは Ktor を用いたアプリケーションです。開発・コントリビュートの際は、以下のガイドラインに従ってください。

## コード標準

### コミット前の必須事項
- すべての変更は `./gradlew ktlintFormat` で整形してください
- コードの静的解析には `./gradlew detekt` を推奨します

### 開発フロー
- ビルド: `./gradlew build`
- テスト: `./gradlew test`
- フォーマット: `./gradlew ktlintFormat`
- 静的解析: `./gradlew detekt`
- 実行: `./gradlew run`
- CIチェック: `./gradlew clean test`

## リポジトリ構成
- `src/main/kotlin/`: アプリケーションの主要な Kotlin コード
- `src/main/resources/`: 設定ファイルやテンプレート
- `public/` または `static/`: 静的アセット（CSS, JS, 画像など）
- `src/test/kotlin/`: テストコード
- `build.gradle.kts`, `settings.gradle.kts`: Gradle 設定ファイル
- `logs/`: ログファイル
- `build/`: ビルド成果物

## 主なガイドライン
1. Ktor のベストプラクティスとイディオムに従うこと
2. コードの構造や命名規則を既存に合わせて統一すること
3. 依存性注入（DI）を活用し、テスト容易性・保守性を高めること
4. 新機能追加時は必ずユニットテスト・統合テストを作成すること
5. 複雑なロジックや公開APIには KDoc やコメントで十分な説明を加えること
6. ドキュメントの更新が必要な場合は `docs/` フォルダに反映すること

---

このガイドは必要に応じて随時更新されます。
