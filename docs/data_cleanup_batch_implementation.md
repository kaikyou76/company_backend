# DataCleanupBatchJob 完全実装レポート

## 概要
DataCleanupBatchJobの完全実装を行いました。OvertimeMonitoringBatchJobを参考に、古いシステムログデータを自動削除するバッチジョブを実装しました。

## 実装コンポーネント

### 1. DataCleanupProcessor
**ファイル**: `src/main/java/com/example/companybackend/batch/processor/DataCleanupProcessor.java`

**機能**:
- 古いシステムログデータを削除対象として識別
- 保持期間（12ヶ月）を超えたデータを削除対象とする
- 削除対象データの統計情報を提供

**主要メソッド**:
- `process(SystemLog systemLog)`: データの削除対象判定
- `getDeleteTargetCount()`: 削除対象データ数の取得
- `getRetentionMonths()`: 保持期間の取得
- `getCutoffDate()`: カットオフ日付の取得

### 2. DataCleanupReader
**ファイル**: `src/main/java/com/example/companybackend/batch/reader/DataCleanupReader.java`

**機能**:
- 古いシステムログデータをページング方式で読み込み
- PostgreSQL用のクエリプロバイダーを使用
- 保持期間を超えたデータのみを対象とする

**特徴**:
- JdbcPagingItemReaderを使用した効率的なデータ読み込み
- ページサイズ: 100件
- IDによる昇順ソート

### 3. DataCleanupWriter
**ファイル**: `src/main/java/com/example/companybackend/batch/writer/DataCleanupWriter.java`

**機能**:
- 削除対象データの一括削除処理
- 削除統計情報の管理
- エラーハンドリングとログ出力

**特徴**:
- バッチ削除による高効率処理
- 削除件数の追跡
- 詳細なログ出力

### 4. DataCleanupBatchConfig
**ファイル**: `src/main/java/com/example/companybackend/batch/config/DataCleanupBatchConfig.java`

**機能**:
- データクリーンアップバッチジョブの設定
- 複数ステップによる安全な処理フロー
- 事前・事後検証の実装

**処理ステップ**:
1. **preValidationStep**: 事前検証（設定・DB接続・削除対象数確認）
2. **recoveryCheckStep**: 復旧チェック（失敗ジョブクリーンアップ）
3. **dataCleanupInitializationStep**: 初期化（統計情報取得・バックアップ準備）
4. **dataCleanupProcessingStep**: メイン処理（データ削除）
5. **postValidationStep**: 事後検証（整合性チェック・残存確認）
6. **cleanupStatisticsStep**: 統計出力（最終結果レポート）

### 5. DataCleanupProcessorTest
**ファイル**: `src/test/java/com/example/companybackend/batch/processor/DataCleanupProcessorTest.java`

**テストケース**:
- 古いデータの削除対象判定テスト
- 最近のデータの保持判定テスト
- 境界値テスト（12ヶ月ちょうど）
- 現在データの保持テスト
- 統計情報取得テスト
- エラーハンドリングテスト
- 様々なステータスでの処理テスト

## データベース拡張

### SystemLogRepository 追加メソッド
```java
// 指定日時より前のログ数を取得
long countByCreatedAtBefore(OffsetDateTime cutoffDate);

// IDリストによる一括削除
int deleteByIdIn(List<Long> ids);
```

## バッチ処理フロー

### 1. 事前検証
- バッチ設定の妥当性確認
- データベース接続性確認
- 削除対象データ数の確認
- 大量データ警告（10万件超過時）

### 2. 復旧チェック
- 前回失敗ジョブのクリーンアップ
- 再開安全性の確認
- ロックファイルのクリーンアップ

### 3. 初期化処理
- 削除対象データの統計情報取得
- バックアップテーブルの準備
- 処理前状態の記録

### 4. メイン処理
- 古いシステムログの読み込み（100件ずつ）
- 削除対象判定（12ヶ月超過）
- 一括削除実行
- 進捗ログ出力

### 5. 事後検証
- データ整合性チェック
- 削除後統計の確認
- 古いデータ残存チェック

### 6. 統計出力
- 最終レコード数の記録
- 処理完了時刻の記録
- 実行コンテキストへの統計保存

## 設定パラメータ

### 保持期間
- **デフォルト**: 12ヶ月
- **対象**: system_logsテーブル
- **判定基準**: created_at < (現在時刻 - 12ヶ月)

### 処理設定
- **チャンクサイズ**: 100件
- **ページサイズ**: 100件
- **バックアップ**: system_logs_backupテーブル（オプション）

## API エンドポイント

### データクリーンアップバッチ実行
```
POST /api/batch/cleanup-data
```

**レスポンス例**:
```json
{
  "success": true,
  "message": "データクリーンアップバッチを実行しました",
  "data": {
    "retentionMonths": 12,
    "cutoffDate": "2024-02-08",
    "deletedCount": 1250,
    "deletedDetails": {
      "system_logs": 1250
    }
  },
  "executedAt": "2025-02-08T10:30:00"
}
```

## 監視・ログ

### ログレベル
- **INFO**: 処理開始・完了・統計情報
- **DEBUG**: 個別レコード処理詳細
- **WARN**: 大量データ警告・再開安全性警告
- **ERROR**: エラー詳細・失敗データ情報

### 監視ポイント
- 削除対象データ数の監視
- 処理時間の監視
- エラー発生率の監視
- 残存古データの監視

## セキュリティ考慮事項

### アクセス制御
- 管理者権限（ADMIN）必須
- バッチ実行権限の制限

### データ保護
- バックアップテーブルの作成
- 削除前の統計情報記録
- 復旧可能性の確保

## パフォーマンス最適化

### 効率的な処理
- ページング方式による メモリ効率化
- 一括削除によるDB負荷軽減
- インデックス活用（created_at）

### リソース管理
- チャンクサイズによる処理量制御
- トランザクション境界の最適化
- 長時間実行への対応

## 運用考慮事項

### 実行タイミング
- 推奨: 深夜・休日の低負荷時間帯
- 頻度: 月次実行を推奨
- 監視: 実行結果の定期確認

### 障害対応
- 失敗時の自動復旧機能
- 部分実行からの再開機能
- エラー詳細の記録・通知

## 今後の拡張予定

### 対象テーブル拡張
- 他のログテーブルへの対応
- 設定可能な保持期間
- テーブル別削除ポリシー

### 機能拡張
- アーカイブ機能の追加
- 削除予定データのプレビュー
- 削除実行の承認フロー

## まとめ

DataCleanupBatchJobの完全実装により、以下を実現しました：

1. **自動化されたデータクリーンアップ**: 古いシステムログの自動削除
2. **安全な処理フロー**: 多段階検証による安全性確保
3. **高性能処理**: ページング・一括処理による効率化
4. **包括的な監視**: 詳細なログ・統計情報の提供
5. **堅牢なエラーハンドリング**: 障害時の自動復旧機能

この実装により、システムの長期運用におけるデータ肥大化問題を解決し、パフォーマンスの維持とストレージコストの最適化を実現できます。