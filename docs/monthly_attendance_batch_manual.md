# 月次打刻集計バッチ 完全手順書

## 概要
本書は、管理者が月次打刻集計ボタンを押してから実行される月次打刻集計バッチの完全な処理フローを、関与する全てのクラスとメソッドと共に詳細に記載したものです。

## 1. エントリーポイント
### 1.1 管理者アクション
- 管理者がフロントエンドで「月次打刻集計」ボタンをクリック
- フロントエンドから `POST /api/batch/monthly-summary` エンドポイントが呼び出される

### 1.2 BatchController.java 
**場所**: `src/main/java/com/example/companybackend/controller/BatchController.java`

**エンドポイント**: `executeMonthlySummaryBatch()`
**行**: 104-135

```java
@PostMapping("/monthly-summary")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<BatchResponseDto.MonthlySummaryBatchResponse> executeMonthlySummaryBatch(
        @RequestBody(required = false) Map<String, Object> parameters)
```

**処理内容**:
1. **管理者権限チェック** (`@PreAuthorize("hasRole('ADMIN')")`)
2. **パラメータ受け取り** (行105)
3. **BatchJobService の呼び出し** (行106)
   - `batchJobService.runJob(monthlyAttendanceSummaryJob, parameters)`
4. **レスポンス生成** (行108-124)
   - 成功時のレスポンスデータ設定
   - 月次集計結果の統計情報設定
5. **エラーハンドリング** (行126-135)
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
### 3.1 DailyAttendanceBatchConfig.java
**場所**: `src/main/java/com/example/companybackend/batch/config/DailyAttendanceBatchConfig.java`

#### ジョブ定義: `monthlyAttendanceSummaryJob()`
**行**: 87-97

**ジョブフロー**:
```
monthlyAttendanceSummaryJob
├── preValidationStep()         (事前検証)
├── recoveryCheckStep()         (復旧チェック)
├── dataInitializationStep()    (データ初期化)
├── monthlyAttendanceProcessingStep()  (月次集計処理)
├── postValidationStep()        (事後検証)
└── thresholdCheckStep()        (閾値チェック)
```

### 3.2 各ステップの詳細処理

#### 3.2.1 事前検証ステップ (preValidationStep)
**行**: 153-158, 238-268

**Tasklet**: `preValidationTasklet()`
**処理内容**:
1. **バッチ設定検証** (行244-250)
   - `validationService.validateBatchConfiguration()`
   - 設定エラーがある場合は例外スロー
2. **データベース接続性検証** (行253-257)
   - `validationService.validateDatabaseConnectivity()`
   - 接続できない場合は例外スロー
3. **エラーハンドリング** (行262-267)
   - `diagnosticLogger.logError()` でエラーログ出力

#### 3.2.2 復旧チェックステップ (recoveryCheckStep)
**行**: 161-166, 272-299

**Tasklet**: `recoveryCheckTasklet()`
**処理内容**:
1. **失敗ジョブクリーンアップ** (行278)
   - `recoveryService.cleanupFailedJobs()`
2. **再開安全性チェック** (行281-285)
   - `recoveryService.isRestartSafe()`
3. **ロックファイルクリーンアップ** (行288)
   - `recoveryService.cleanupLockFiles()`

#### 3.2.3 データ初期化ステップ (dataInitializationStep)
**行**: 169-174, 303-334

**Tasklet**: `dataInitializationTasklet()`
**処理内容**:
1. **ジョブ名判定** (行309)
   - `contribution.getStepExecution().getJobExecution().getJobInstance().getJobName()`
2. **月次集計データ削除** (行316-319)
   ```sql
   DELETE FROM attendance_summaries 
   WHERE DATE_TRUNC('month', target_date) = DATE_TRUNC('month', CURRENT_DATE) 
   AND summary_type = 'monthly'
   ```
3. **データアクセス例外ハンドリング** (行325-331)

#### 3.2.4 月次集計処理ステップ (monthlyAttendanceProcessingStep) - メイン処理
**行**: 187-195

**チャンク処理設定**:
- **チャンクサイズ**: 10件
- **Reader**: `monthlySummaryItemReader()`
- **Processor**: `monthlyWorkTimeProcessor()`
- **Writer**: `attendanceSummaryWriter()`

## 4. データ読み取り層 (Reader)
### 4.1 MonthlySummaryReader.java
**場所**: `src/main/java/com/example/companybackend/batch/reader/MonthlySummaryReader.java`

#### メソッド: `reader()`
**行**: 25-50

**処理内容**:
1. **ソート設定** (行29-31)
   - ユーザーID昇順、タイムスタンプ昇順でソート
2. **RepositoryItemReader構築** (行33-38)
   - リポジトリ: `attendanceRecordRepository`
   - メソッド名: `findByTypeAndTimestampBetween`
   - ページサイズ: 100件
   - リーダー名: `monthlySummaryReader`
3. **月次処理範囲設定** (行41-44)
   - 現在月の開始日から終了日まで
   - `YearMonth.now()` で当月を取得
4. **パラメータ設定** (行46-50)
   - 'in'タイプのレコードのみ対象
   - 当月の開始時刻から終了時刻まで

## 5. データ処理層 (Processor)
### 5.1 MonthlyWorkTimeProcessor.java
**場所**: `src/main/java/com/example/companybackend/batch/processor/MonthlyWorkTimeProcessor.java`

#### メソッド: `process(AttendanceRecord attendanceRecord)`
**行**: 20-65

**処理フロー**:
1. **入力レコード検証** (行22-25)
   - 'in'タイプのレコードのみ処理
   - 'out'レコードは null を返して処理スキップ

2. **ユーザー・月情報取得** (行27-30)
   - ユーザーID と対象月の抽出
   - `YearMonth.from(recordDate)` で月情報取得

3. **既存月次サマリーチェック** (行32-39)
   - `attendanceSummaryRepository.findByUserIdAndSummaryTypeAndTargetDateBetween()`
   - 既存の月次集計がある場合はスキップ

4. **日次サマリーデータ取得** (行41-47)
   - 該当ユーザーの該当月の全日次集計データを取得
   - 日次データがない場合はスキップ

5. **月次集計計算** (行49)
   - `calculateMonthlyWorkTime(dailySummaries)` メソッド呼び出し

6. **AttendanceSummary作成** (行51-63)
   - 各種時間データの設定
   - サマリータイプ: "monthly"
   - 対象日: 月初日（例：2025-02-01）
   - 作成日時: 現在時刻

#### メソッド: `calculateMonthlyWorkTime(List<AttendanceSummary> dailySummaries)`
**行**: 72-95

**処理内容**:
1. **初期化** (行75-78)
   - 各時間項目をBigDecimal.ZEROで初期化

2. **日次データ集計** (行80-87)
   - 総労働時間の合計
   - 残業時間の合計
   - 深夜労働時間の合計
   - 休日労働時間の合計
   - null値の適切な処理（null の場合は 0 として扱う）

3. **精度調整** (行89-92)
   - 全ての時間データを小数点以下2位に設定
   - `setScale(2, RoundingMode.HALF_UP)` で四捨五入

#### 内部クラス: `MonthlyWorkTimeResult`
**行**: 97-103

**フィールド**:
- `totalHours`: 総労働時間
- `overtimeHours`: 残業時間
- `lateNightHours`: 深夜労働時間
- `holidayHours`: 休日労働時間

## 6. データ書き込み層 (Writer)
### 6.1 AttendanceSummaryWriter.java
**場所**: `src/main/java/com/example/companybackend/batch/writer/AttendanceSummaryWriter.java`

#### メソッド: `write(Chunk<? extends AttendanceSummary> chunk)`
**行**: 16-18

**処理内容**:
1. **バッチ保存** (行17)
   - `attendanceSummaryRepository.saveAll(chunk.getItems())`
   - チャンクサイズ分の AttendanceSummary を一括保存

## 7. 事後処理ステップ
### 7.1 事後検証ステップ (postValidationStep)
**行**: 199-204, 338-379

**処理内容**:
1. **データ整合性チェック** (行344-350)
   - `validationService.validateDataIntegrity()`
2. **ビジネスルール検証** (行353-359)
   - `validationService.validateBusinessRules()`
3. **警告処理** (行362-368)
   - 警告がある場合のログ出力

### 7.2 閾値チェックステップ (thresholdCheckStep)
**行**: 207-212, 383-418

**処理内容**:
1. **閾値チェック実行** (行389)
   - `checkThresholds()` メソッド呼び出し
2. **レコード数確認** (行409-410)
   ```sql
   SELECT COUNT(*) FROM attendance_summaries WHERE date = CURRENT_DATE
   ```
3. **閾値判定** (行413)
   - 10,000件を超える場合は例外スロー

## 8. エラーハンドリングとログ
### 8.1 エラー処理パターン
1. **設定エラー**: バッチ実行停止
2. **データベースエラー**: 再試行可能例外として処理
3. **ビジネスルール違反**: 警告ログ出力
4. **閾値超過**: 致命的エラーとして処理

### 8.2 ログ出力
- **EnhancedJobExecutionListener**: ジョブレベルのログ
- **EnhancedStepExecutionListener**: ステップレベルのログ
- **BatchDiagnosticLogger**: 診断ログ

## 9. 依存関係とリポジトリ
### 9.1 使用リポジトリ
- **AttendanceRecordRepository**: 出席レコード操作
  - `findByTypeAndTimestampBetween()`: 月次処理用データ取得
- **AttendanceSummaryRepository**: 集計データ操作
  - `findByUserIdAndSummaryTypeAndTargetDateBetween()`: 既存月次・日次データ取得
  - `saveAll()`: 月次集計データ保存

### 9.2 使用サービス
- **BatchValidationService**: バッチ検証処理
- **BatchRecoveryService**: 復旧処理
- **BatchMonitoringService**: 監視処理

## 10. 処理フロー全体図
```
[管理者] → [フロントエンド] → [BatchController]
    ↓
[BatchJobService] → [JobLauncher] → [monthlyAttendanceSummaryJob]
    ↓
[preValidationStep] → [recoveryCheckStep] → [dataInitializationStep]
    ↓
[monthlyAttendanceProcessingStep]: MonthlySummaryReader → MonthlyWorkTimeProcessor → AttendanceSummaryWriter
    ↓
[postValidationStep] → [thresholdCheckStep] → [完了]
```

## 11. 月次処理の特徴
### 11.1 日次処理との違い
| 項目 | 日次処理 | 月次処理 |
|------|----------|----------|
| **入力データ** | 出勤記録 (attendance_records) | 日次集計データ (attendance_summaries) |
| **処理対象** | 当日の打刻データ | 当月の日次集計データ |
| **集計方法** | 打刻ペアから労働時間計算 | 日次データの合計 |
| **出力** | 日次サマリー (summary_type='daily') | 月次サマリー (summary_type='monthly') |
| **対象日設定** | 実際の勤務日 | 月初日 (例: 2025-02-01) |

### 11.2 データフロー
```
attendance_records (打刻データ)
    ↓ (日次バッチ)
attendance_summaries (summary_type='daily')
    ↓ (月次バッチ)
attendance_summaries (summary_type='monthly')
```

## 12. 重要な設定値
- **チャンクサイズ**: 10件
- **ページサイズ**: 100件
- **処理対象**: 当月の'in'タイプレコード
- **閾値**: 10,000件
- **集計データ削除範囲**: 当月の月次データのみ
- **精度**: 小数点以下2位（四捨五入）

## 13. データベース設計対応
### 13.1 attendance_summaries テーブル
- **summary_type**: 'daily' または 'monthly' で処理タイプを区別
- **target_date**: 月次データは月初日を設定
- **各時間フィールド**: NUMERIC(38,2) で高精度計算

### 13.2 処理対象期間
- **月次処理**: `YearMonth.now()` で当月を特定
- **開始日**: `targetMonth.atDay(1)` (月初)
- **終了日**: `targetMonth.atEndOfMonth()` (月末)

## 14. トラブルシューティング
### 14.1 一般的な問題
1. **権限エラー**: 管理者権限の確認
2. **データベース接続エラー**: 接続設定の確認
3. **日次データ不足**: 日次バッチの実行状況確認
4. **重複処理**: 既存月次データの確認

### 14.2 ログ確認箇所
- `DailyAttendanceBatchConfig` のステップログ
- `MonthlyWorkTimeProcessor` の処理ログ
- `MonthlySummaryReader` の読み取りログ
- `BatchDiagnosticLogger` の診断ログ

### 14.3 データ確認SQL
```sql
-- 月次集計データ確認
SELECT * FROM attendance_summaries 
WHERE summary_type = 'monthly' 
AND DATE_TRUNC('month', target_date) = DATE_TRUNC('month', CURRENT_DATE);

-- 日次集計データ確認
SELECT COUNT(*) FROM attendance_summaries 
WHERE summary_type = 'daily' 
AND DATE_TRUNC('month', target_date) = DATE_TRUNC('month', CURRENT_DATE);
```

## 15. パフォーマンス考慮事項
### 15.1 処理効率
- 日次データを基にした集計により、打刻データの再計算を回避
- 既存月次データのチェックにより、重複処理を防止
- チャンク処理によるメモリ効率の最適化

### 15.2 スケーラビリティ
- ページング処理による大量データ対応
- インデックス活用による高速検索
- トランザクション分割による安定性確保

この手順書は、月次打刻集計バッチの完全な実行フローと、関与する全ての主要クラス・メソッドを網羅しています。