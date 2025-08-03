# データクリーンアップバッチ 完全手順書

## 概要
本書は、管理者がデータクリーンアップバッチ実行ボタンを押してから実行されるデータクリーンアップバッチの完全な処理フローを、関与する全てのクラスとメソッドと共に詳細に記載したものです。

## 1. エントリーポイント
### 1.1 管理者アクション
- 管理者がフロントエンドで「データクリーンアップ」ボタンをクリック
- フロントエンドから `POST /api/batch/cleanup-data` エンドポイントが呼び出される

### 1.2 BatchController.java 
**場所**: `src/main/java/com/example/companybackend/controller/BatchController.java`

**エンドポイント**: `executeDataCleanupBatch()`
**行**: 158-189

```java
@PostMapping("/cleanup-data")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<BatchResponseDto.DataCleanupBatchResponse> executeDataCleanupBatch(
        @RequestBody(required = false) Map<String, Object> parameters)
```

**処理内容**:
1. **管理者権限チェック** (`@PreAuthorize("hasRole('ADMIN')")`)
2. **パラメータ受け取り** (行161)
3. **BatchJobService の呼び出し** (行164)
   - `batchJobService.runJob(dataCleanupJob, parameters)`
4. **レスポンス生成** (行166-182)
   - 成功時のレスポンスデータ設定
   - 削除統計情報の設定
   - 保持期間・カットオフ日付の設定
5. **エラーハンドリング** (行184-189)
   - 例外発生時の適切なエラーレスポンス

## 2. バッチジョブサービス層
### 2.1 BatchJobService.java
**場所**: `src/main/java/com/example/companybackend/batch/service/BatchJobService.java`

#### メソッド: `runJob(Job job, Map<String, Object> parameters)`
**行**: 23-46

**処理フロー**:
1. **JobParametersBuilder生成** (行24)
2. **パラメータ変換処理** (行27-39)
   - String, Long, Double型の判定と変換
   - その他の型は toString() で文字列化
3. **一意性タイムスタンプ追加** (行42)
4. **JobLauncher実行** (行45)
   - `jobLauncher.run(job, jobParameters)`

## 3. バッチ設定とジョブ定義
### 3.1 DataCleanupBatchConfig.java
**場所**: `src/main/java/com/example/companybackend/batch/config/DataCleanupBatchConfig.java`

#### ジョブ定義: `dataCleanupJob()`
**行**: 42-52

**ジョブフロー**:
```
dataCleanupJob
├── preValidationStep()                    (事前検証)
├── recoveryCheckStep()                    (復旧チェック)
├── dataCleanupInitializationStep()       (データクリーンアップ初期化)
├── dataCleanupProcessingStep()           (データクリーンアップ処理)
├── postValidationStep()                  (事後検証)
└── cleanupStatisticsStep()               (クリーンアップ統計)
```

### 3.2 各ステップの詳細処理

#### 3.2.1 事前検証ステップ (preValidationStep)
**行**: 64-70, 130-168

**Tasklet**: `preValidationTasklet()`
**処理内容**:
1. **バッチ設定検証** (行136-142)
   - `validationService.validateBatchConfiguration()`
   - 設定エラーがある場合は例外スロー
2. **データベース接続性検証** (行145-149)
   - `validationService.validateDatabaseConnectivity()`
   - 接続できない場合は例外スロー
3. **削除対象データ数確認** (行152-158)
   - `systemLogRepository.countByCreatedAtBefore(cutoffDate)`
   - 12ヶ月前のカットオフ日時を設定
   - 大量データ警告（10万件超過時）
4. **エラーハンドリング** (行162-167)
   - `diagnosticLogger.logError()` でエラーログ出力

#### 3.2.2 復旧チェックステップ (recoveryCheckStep)
**行**: 72-78, 172-199

**Tasklet**: `recoveryCheckTasklet()`
**処理内容**:
1. **失敗ジョブクリーンアップ** (行178)
   - `recoveryService.cleanupFailedJobs()`
2. **再開安全性チェック** (行181-185)
   - `recoveryService.isRestartSafe()`
3. **ロックファイルクリーンアップ** (行188)
   - `recoveryService.cleanupLockFiles()`

#### 3.2.3 データクリーンアップ初期化ステップ (dataCleanupInitializationStep)
**行**: 80-86, 203-244

**Tasklet**: `dataCleanupInitializationTasklet()`
**処理内容**:
1. **削除対象データ統計取得** (行209-215)
   - 総レコード数: `systemLogRepository.count()`
   - 削除対象数: `systemLogRepository.countByCreatedAtBefore(cutoffDate)`
   - 保持対象数: 総数 - 削除対象数
2. **統計情報ログ出力** (行217-222)
   - 総レコード数、削除対象数、保持対象数、カットオフ日時
3. **バックアップテーブル作成** (行225-232)
   ```sql
   CREATE TABLE IF NOT EXISTS system_logs_backup AS 
   SELECT * FROM system_logs WHERE 1=0
   ```
4. **データアクセス例外ハンドリング** (行235-243)

#### 3.2.4 データクリーンアップ処理ステップ (dataCleanupProcessingStep) - メイン処理
**行**: 88-96

**チャンク処理設定**:
- **チャンクサイズ**: 100件
- **Reader**: `dataCleanupItemReader()`
- **Processor**: `dataCleanupProcessor()`
- **Writer**: `dataCleanupWriter()`

## 4. データ読み取り層 (Reader)
### 4.1 DataCleanupReader.java
**場所**: `src/main/java/com/example/companybackend/batch/reader/DataCleanupReader.java`

#### メソッド: `reader()`
**行**: 28-65

**処理内容**:
1. **JdbcPagingItemReader設定** (行33-36)
   - データソース設定
   - ページサイズ: 100件
   - RowMapper: `BeanPropertyRowMapper<>(SystemLog.class)`
2. **PostgreSQL用クエリプロバイダー設定** (行39-44)
   ```sql
   SELECT id, user_id, action, status, ip_address, user_agent, details, created_at
   FROM system_logs
   WHERE created_at < :cutoffDate
   ```
3. **ソート条件設定** (行47-49)
   - ID昇順でソート
4. **パラメータ設定** (行54-56)
   - カットオフ日時: 現在時刻 - 12ヶ月
5. **リーダー初期化** (行58-61)
   - リーダー名: `dataCleanupReader`
   - `afterPropertiesSet()` で初期化完了

#### メソッド: `getDeleteTargetCount()`
**行**: 70-77

**処理内容**:
- 削除対象データ数を事前に取得
- `systemLogRepository.countByCreatedAtBefore(cutoffDate)`

## 5. データ処理層 (Processor)
### 5.1 DataCleanupProcessor.java
**場所**: `src/main/java/com/example/companybackend/batch/processor/DataCleanupProcessor.java`

#### メソッド: `process(SystemLog systemLog)`
**行**: 25-50

**処理フロー**:
1. **保持期間チェック** (行29-30)
   - カットオフ日時: 現在時刻 - 12ヶ月
   - `systemLog.getCreatedAt().isBefore(cutoffDate)`
2. **削除対象判定** (行32-36)
   - 保持期間を超えた場合: SystemLogオブジェクトを返す（削除対象）
   - 保持期間内の場合: nullを返す（削除対象外）
3. **ログ出力** (行33-34, 37-38)
   - 削除対象データと保持対象データの詳細ログ
4. **エラーハンドリング** (行41-46)
   - 処理中の例外を適切にログ出力してスロー

#### メソッド: `getDeleteTargetCount()`
**行**: 55-63

**処理内容**:
- 削除対象データ数の統計取得
- エラー時は0を返却

#### メソッド: `getRetentionMonths()`, `getCutoffDate()`
**行**: 68-77

**処理内容**:
- 保持期間（12ヶ月）の取得
- カットオフ日付の取得

## 6. データ書き込み層 (Writer)
### 6.1 DataCleanupWriter.java
**場所**: `src/main/java/com/example/companybackend/batch/writer/DataCleanupWriter.java`

#### メソッド: `write(Chunk<? extends SystemLog> chunk)`
**行**: 24-58

**処理内容**:
1. **削除対象チェック** (行27-31)
   - 削除対象データがない場合はスキップ
2. **IDリスト作成** (行36-39)
   - 削除対象のSystemLogからIDリストを抽出
3. **バッチ削除実行** (行42-43)
   - `systemLogRepository.deleteByIdIn(idsToDelete)`
   - 削除件数を累計に追加
4. **削除統計更新** (行44-45)
   - 今回削除件数と累計削除件数の記録
5. **詳細ログ出力** (行48-52)
   - デバッグレベルで削除されたデータの詳細を出力
6. **エラーハンドリング** (行54-58)
   - 削除失敗時の詳細ログとランタイム例外スロー

#### メソッド: `getDeletedCount()`, `resetDeletedCount()`, `logStatistics()`
**行**: 63-82

**処理内容**:
- 削除件数の取得・リセット
- 削除処理の統計情報出力

## 7. 事後処理ステップ
### 7.1 事後検証ステップ (postValidationStep)
**行**: 98-104, 248-289

**処理内容**:
1. **データ整合性チェック** (行254-260)
   - `validationService.validateDataIntegrity()`
2. **クリーンアップ後統計確認** (行263-270)
   - 残存レコード数: `systemLogRepository.count()`
   - 古いデータ残存数: `systemLogRepository.countByCreatedAtBefore(cutoffDate)`
3. **残存データ警告** (行272-274)
   - 古いデータが残存している場合の警告ログ
4. **エラーハンドリング** (行280-288)
   - `diagnosticLogger.logError()` でエラーログ出力

### 7.2 クリーンアップ統計ステップ (cleanupStatisticsStep)
**行**: 106-112, 293-320

**処理内容**:
1. **最終統計出力** (行299-305)
   - 処理完了時刻、最終レコード数、保持期間
2. **実行コンテキスト保存** (行308-312)
   - `finalRecordCount`: 最終レコード数
   - `processingCompletedAt`: 処理完了時刻
3. **統計情報の永続化** (行308-312)
   - ステップ実行コンテキストに統計情報を保存

## 8. データベース操作層
### 8.1 SystemLogRepository.java
**場所**: `src/main/java/com/example/companybackend/repository/SystemLogRepository.java`

#### 使用メソッド:
1. **`countByCreatedAtBefore(OffsetDateTime cutoffDate)`**
   - 指定日時より前のログ数を取得
   - 削除対象データ数の事前確認に使用

2. **`deleteByIdIn(List<Long> ids)`**
   - IDリストによる一括削除
   - バッチ削除処理で使用

3. **`count()`**
   - 総レコード数の取得
   - 統計情報出力に使用

## 9. エラーハンドリングとログ
### 9.1 エラー処理パターン
1. **設定エラー**: バッチ実行停止
2. **データベースエラー**: 再試行可能例外として処理
3. **削除処理エラー**: 詳細ログ出力後にランタイム例外
4. **大量データ警告**: 警告ログ出力（処理継続）

### 9.2 ログ出力レベル
- **INFO**: 処理開始・完了・統計情報
- **DEBUG**: 個別レコード処理詳細
- **WARN**: 大量データ警告・再開安全性警告・残存データ警告
- **ERROR**: エラー詳細・失敗データ情報

### 9.3 使用ログ出力クラス
- **EnhancedJobExecutionListener**: ジョブレベルのログ
- **EnhancedStepExecutionListener**: ステップレベルのログ
- **BatchDiagnosticLogger**: 診断ログ

## 10. 処理フロー全体図
```
[管理者] → [フロントエンド] → [BatchController]
    ↓
[BatchJobService] → [JobLauncher] → [dataCleanupJob]
    ↓
[preValidationStep] → [recoveryCheckStep] → [dataCleanupInitializationStep]
    ↓
[dataCleanupProcessingStep]: DataCleanupReader → DataCleanupProcessor → DataCleanupWriter
    ↓
[postValidationStep] → [cleanupStatisticsStep] → [完了]
```

## 11. データクリーンアップ処理の特徴
### 11.1 他のバッチ処理との違い
| 項目 | 日次処理 | 月次処理 | データクリーンアップ処理 |
|------|----------|----------|------------------------|
| **入力データ** | 出勤記録 (attendance_records) | 日次集計データ (attendance_summaries) | システムログ (system_logs) |
| **処理対象** | 当日の打刻データ | 当月の日次集計データ | 12ヶ月以前の古いログデータ |
| **処理方法** | 打刻ペアから労働時間計算 | 日次データの合計 | 古いデータの削除 |
| **出力** | 日次サマリー作成 | 月次サマリー作成 | データ削除（統計情報出力） |
| **保持期間** | - | - | 12ヶ月 |

### 11.2 データフロー
```
system_logs (全システムログ)
    ↓ (保持期間判定)
古いデータ (12ヶ月以前) → [削除対象]
新しいデータ (12ヶ月以内) → [保持対象]
    ↓ (削除処理)
system_logs (クリーンアップ後)
```

## 12. 重要な設定値
- **保持期間**: 12ヶ月
- **チャンクサイズ**: 100件
- **ページサイズ**: 100件
- **処理対象**: created_at < (現在時刻 - 12ヶ月)
- **大量データ警告閾値**: 100,000件
- **バックアップテーブル**: system_logs_backup

## 13. データベース設計対応
### 13.1 system_logs テーブル
- **created_at**: TIMESTAMP WITH TIME ZONE - 削除判定の基準
- **id**: BIGINT PRIMARY KEY - 削除処理のキー
- **インデックス**: created_at にインデックス推奨

### 13.2 処理対象期間
- **カットオフ日時**: `OffsetDateTime.now().minusMonths(12)`
- **削除条件**: `created_at < cutoffDate`
- **保持条件**: `created_at >= cutoffDate`

## 14. セキュリティ・安全性考慮事項
### 14.1 データ保護
1. **バックアップテーブル作成**: 削除前のデータ構造保持
2. **統計情報記録**: 削除前後の詳細な記録
3. **段階的検証**: 事前・事後の多重チェック

### 14.2 アクセス制御
- **管理者権限必須**: `@PreAuthorize("hasRole('ADMIN')")`
- **バッチ実行権限**: 限定されたユーザーのみ実行可能

### 14.3 復旧機能
- **失敗ジョブクリーンアップ**: 前回失敗時の適切な復旧
- **再開安全性チェック**: 安全な再実行の確認
- **ロックファイル管理**: 重複実行の防止

## 15. パフォーマンス最適化
### 15.1 処理効率
- **ページング処理**: メモリ効率的な大量データ処理
- **一括削除**: `deleteByIdIn()` による効率的な削除
- **インデックス活用**: created_at インデックスによる高速検索

### 15.2 リソース管理
- **チャンク処理**: 100件ずつの分割処理でメモリ使用量制御
- **トランザクション分割**: 長時間ロックの回避
- **統計情報キャッシュ**: 削除件数の効率的な管理

## 16. 監視・運用
### 16.1 監視ポイント
1. **削除対象データ数**: 事前確認での大量データ検出
2. **処理時間**: 長時間実行の監視
3. **エラー発生率**: 削除処理の成功率
4. **残存古データ**: 削除漏れの検出

### 16.2 運用推奨事項
- **実行タイミング**: 深夜・休日の低負荷時間帯
- **実行頻度**: 月次実行を推奨
- **事前確認**: 削除対象データ数の事前チェック
- **バックアップ**: 重要データの事前バックアップ

## 17. トラブルシューティング
### 17.1 一般的な問題
1. **権限エラー**: 管理者権限の確認
2. **データベース接続エラー**: 接続設定の確認
3. **大量データ警告**: 削除対象データ数の確認
4. **削除処理失敗**: ディスク容量・ロック状況の確認

### 17.2 ログ確認箇所
- `DataCleanupBatchConfig` のステップログ
- `DataCleanupProcessor` の処理ログ
- `DataCleanupReader` の読み取りログ
- `DataCleanupWriter` の削除ログ
- `BatchDiagnosticLogger` の診断ログ

### 17.3 データ確認SQL
```sql
-- 削除対象データ数確認
SELECT COUNT(*) FROM system_logs 
WHERE created_at < (CURRENT_TIMESTAMP - INTERVAL '12 months');

-- 削除後データ確認
SELECT COUNT(*) FROM system_logs;

-- 古いデータ残存確認
SELECT COUNT(*) FROM system_logs 
WHERE created_at < (CURRENT_TIMESTAMP - INTERVAL '12 months');

-- バックアップテーブル確認
SELECT COUNT(*) FROM system_logs_backup;
```

## 18. 実行結果の確認
### 18.1 成功時のレスポンス
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

### 18.2 統計情報の確認
- **処理完了時刻**: 実行コンテキストに保存
- **最終レコード数**: クリーンアップ後の総数
- **削除件数**: 実際に削除されたレコード数
- **保持期間**: 12ヶ月の設定値

## 19. 拡張性・将来対応
### 19.1 対象テーブル拡張
- 他のログテーブルへの対応
- テーブル別保持期間設定
- 削除ポリシーの柔軟化

### 19.2 機能拡張
- アーカイブ機能の追加
- 削除予定データのプレビュー
- 削除実行の承認フロー
- 自動実行スケジュール機能

この手順書は、データクリーンアップバッチの完全な実行フローと、関与する全ての主要クラス・メソッドを網羅しています。システムの長期運用におけるデータ肥大化問題の解決と、パフォーマンス維持を実現する重要な機能です。