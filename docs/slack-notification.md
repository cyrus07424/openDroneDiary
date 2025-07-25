# Slack通知機能の設定

OpenDroneDialyでは、ユーザーの行動や各種データの登録・更新時にSlackへ通知を送信する機能を提供しています。

## 環境変数の設定

Slackへの通知を有効にするには、以下の環境変数を設定してください：

```bash
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK"
```

もしくは、アプリケーション実行時に指定：

```bash
SLACK_WEBHOOK_URL="https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK" ./gradlew run
```

## Slack Webhookの設定

1. Slackワークスペースで「Apps」→「Incoming Webhooks」を検索して追加
2. 通知を送信したいチャンネルを選択
3. 生成されたWebhook URLを`SLACK_WEBHOOK_URL`環境変数に設定

## 通知対象

以下の操作時にSlackへ通知が送信されます：

### ユーザー操作
- **新規ユーザー登録**: ユーザー名、メールアドレス、IPアドレス、ユーザーエージェントを含む
- **ユーザーログイン**: ユーザー名、IPアドレス、ユーザーエージェントを含む

### データ操作
- **飛行記録作成**: 飛行日、パイロット名、ユーザー情報を含む
- **飛行記録更新**: 記録ID、飛行日、ユーザー情報を含む
- **日常点検記録作成**: 点検日、点検者、ユーザー情報を含む
- **点検整備記録作成**: 点検日、点検者、ユーザー情報を含む

## 通知形式

通知には以下の情報が含まれます：

```
:information_source: **[操作内容]**
👤 ユーザー: [ユーザー名]
🌐 IPアドレス: [IPアドレス]
🖥️ ユーザーエージェント: [ブラウザ/クライアント情報]
📝 詳細: [操作固有の詳細情報]
```

## トラブルシューティング

### 通知が送信されない場合

1. **環境変数の確認**: `SLACK_WEBHOOK_URL`が正しく設定されているか確認
2. **Webhook URLの確認**: Slackから取得したWebhook URLが正しいか確認
3. **ネットワーク接続**: アプリケーションからSlackへのHTTPS接続が可能か確認
4. **ログの確認**: アプリケーションログで通知送信時のエラーメッセージを確認

### 環境変数が設定されていない場合

環境変数`SLACK_WEBHOOK_URL`が設定されていない場合、通知の送信はスキップされ、エラーは発生しません。これにより、Slack通知を使用しない環境でも正常に動作します。

## セキュリティ考慮事項

- Slack Webhook URLは機密情報です。コードに直接書き込まず、必ず環境変数として管理してください
- 通知に含まれるユーザー情報やIPアドレスは適切に保護されたSlackチャンネルに送信してください
- 必要に応じて、通知内容から機密性の高い情報を除外するように設定を調整してください