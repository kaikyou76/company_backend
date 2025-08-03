# 残業監視バッチ 完全手順書

## 概要
本書は、管理者が残業監視バッチボタンを押してから実行される残業監視バッチの完全な処理フローを、関与する全てのクラスとメソッドと共に詳細に記載したものです。

## 1. エントリーポイント
### 1.1 管理者アクション
- 管理者がフロントエンドで「残業監視バッチ」ボタンをクリック
- フロントエンドから `POST /api/batch/overtime-monitoring` エンドポイントが呼び出される

### 1.2 BatchController.java 
**場所**: `src/main/java/com/example/companybackend/controller/BatchController.java`

**エンドポイント**: `executeOvertimeMonitoringBatch()`
**行**: 244-280

```java
@PostMapping("/overtime-monitoring")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<BatchResponseDto.OvertimeMonitoringBatchResponse> executeOvertimeMonitoringBatch(
        @RequestBody(required = false) Map<String, Object> parameters)
```

**処理内容**:
1. 管理者権限チェック (`@PreAuthorize("hasRole('ADMIN')")`)
2. パラメータ受け取り (行248)
3. BatchJobService の呼び出し (行250)
   - `batchJobService.runJob(overtimeMonitoringBatchJob, parameters)`
4. レスポンス生成とエラーハンドリング (行252-280)

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

## 3. バッチ設定とジョブ定義
### 3.1 DailyAttendanceBatchConfig.java
**場所**: `src/main/java/com/example/companybackend/batch/config/DailyAttendanceBatchConfig.java`

#### ジョブ定義: `overtimeMonitoringBatchJob()`
**行**: 135-146

**ジョブフロー**:
```
overtimeMonitoringBatchJob
├── preValidationStep()              (事前検証)
├── recoveryCheckStep()              (復旧チェック)
├── overtimeDataInitializationStep() (残業データ初期化)
├── overtimeMonitoringProcessingStep() (残業監視処理)
├── postValidationStep()             (事後検証)
└── thresholdCheckStep()             (閾値チェック)
```

### 3.2 各ステップの詳細処理

#### 3.2.1 事前検証ステップ (preValidationStep)
**行**: 155-160, 270-300

**Tasklet**: `preValidationTasklet()`
**処理内容**:
1. **バッチ設定検証** (行276-282)
   - `validationService.validateBatchConfiguration()`
   - 設定エラーがある場合は例外スロー
2. **データベース接続性検証** (行285-289)
   - `validationService.validateDatabaseConnectivity()`
   - 接続できない場合は例外スロー
3. **エラーハンドリング** (行294-299)
   - `diagnosticLogger.logError()` でエラーログ出力

#### 3.2.2 復旧チェックステップ (recoveryCheckStep)
**行**: 163-168, 304-331

**Tasklet**: `recoveryCheckTasklet()`
**処理内容**:
1. **失敗ジョブクリーンアップ** (行310)
   - `recoveryService.cleanupFailedJobs()`
2. **再開安全性チェック** (行313-317)
   - `recoveryService.isRestartSafe()`
3. **ロックファイルクリーンアップ** (行320)
   - `recoveryService.cleanupLockFiles()`

#### 3.2.3 残業データ初期化ステップ (overtimeDataInitializationStep)
**行**: 197-202, 450-470

**Tasklet**: `overtimeDataInitializationTasklet()`
**処理内容**:
1. **既存残業レポートデータ削除** (行456-458)
   ```sql
   DELETE FROM overtime_reports 
   WHERE DATE_TRUNC('month', target_month) = DATE_TRUNC('month', CURRENT_DATE)
   ```
2. **データアクセス例外ハンドリング** (行462-468)

#### 3.2.4 残業監視処理ステップ (overtimeMonitoringProcessingStep) - メイン処理
**行**: 189-197

**チャンク処理設定**:
- **チャンクサイズ**: 10件
- **Reader**: `overtimeMonitoringItemReader()`
- **Processor**: `overtimeMonitoringProcessor()`
- **Writer**: `overtimeReportWriter()`

## 4. データ読み取り層 (Reader)
### 4.1 OvertimeMonitoringReader.java
**場所**: `src/main/java/com/example/companybackend/batch/reader/OvertimeMonitoringReader.java`

#### メソッド: `reader()`
**行**: 23-48

**処理内容**:
1. **ソート設定** (行27-29)
   - ユーザーID昇順、対象日昇順でソート
2. **RepositoryItemReader構築** (行31-36)
   - リポジトリ: `attendanceSummaryRepository`
   - メソッド名: `findBySummaryTypeAndTargetDateBetween`
   - ページサイズ: 100件
   - リーダー名: `overtimeMonitoringReader`
3. **残業監視処理範囲設定** (行39-42)
   - 現在月の開始日から終了日まで
   - `YearMonth.now()` で当月を取得
4. **パラメータ設定** (行44-48)
   - 'monthly'タイプのレコードのみ対象
   - 当月の月次集計データ

## 5. データ処理層 (Processor)
### 5.1 OvertimeMonitoringProcessor.java
**場所**: `src/main/java/com/example/companybackend/batch/processor/OvertimeMonitoringProcessor.java`

#### メソッド: `process(AttendanceSummary attendanceSummary)`
**行**: 30-72

**処理フロー**:
1. **入力データ検証** (行32-35)
   - 'monthly'タイプの集計データのみ処理
   - 'daily'データは null を返して処理スキップ

2. **ユーザー・月情報取得** (行37-41)
   - ユーザーID と対象月の抽出
   - `YearMonth.from(targetDate)` で月情報取得

3. **既存残業レポートチェック** (行43-46)
   - `overtimeReportRepository.findByUserIdAndTargetMonth(userId, monthStart)`
   - 既存レポートの存在確認

4. **残業レポート作成/更新** (行48-58)
   - 既存レポートがある場合は更新 (行49-51)
   - ない場合は新規作成 (行52-58)
   - 作成日時・更新日時の設定

5. **残業データ設定** (行60-68)
   - 総残業時間、深夜労働時間、休日労働時間の設定
   - null値の適切な処理（null の場合は 0 として扱う）

6. **ステータス判定** (行70-72)
   - `determineOvertimeStatus(totalOvertime, totalLateNight, totalHoliday)` メソッド呼び出し
   - 閾値に基づく自動ステータス設定

#### メソッド: `determineOvertimeStatus(BigDecimal totalOvertime, BigDecimal totalLateNight, BigDecimal totalHoliday)`
**行**: 81-100

**処理内容**:
1. **閾値超過チェック** (行84-88)
   - 残業時間: 45時間/月 (`OVERTIME_THRESHOLD`)
   - 深夜労働時間: 20時間/月 (`LATE_NIGHT_THRESHOLD`)
   - 休日労働時間: 15時間/月 (`HOLIDAY_THRESHOLD`)
   - 閾値超過時は "confirmed" ステータス

2. **残業時間存在チェック** (行91-95)
   - いずれかの残業時間が存在する場合は "draft" ステータス

3. **残業時間なし** (行98)
   - 全ての残業時間が0の場合は "approved" ステータス

#### 内部クラス: `OvertimeThresholds`
**行**: 107-111

**定数定義**:
- `OVERTIME_THRESHOLD`: 45.00時間（月次残業時間閾値）
- `LATE_NIGHT_THRESHOLD`: 20.00時間（月次深夜労働時間閾値）
- `HOLIDAY_THRESHOLD`: 15.00時間（月次休日労働時間閾値）

## 6. データ書き込み層 (Writer)
### 6.1 OvertimeReportWriter.java
**場所**: `src/main/java/com/example/companybackend/batch/writer/OvertimeReportWriter.java`

#### メソッド: `write(Chunk<? extends OvertimeReport> chunk)`
**行**: 16-18

**処理内容**:
1. **バッチ保存** (行17)
   - `overtimeReportRepository.saveAll(chunk.getItems())`
   - チャンクサイズ分の OvertimeReport を一括保存

## 7. 事後処理ステップ
### 7.1 事後検証ステップ (postValidationStep)
**行**: 205-210, 370-411

**処理内容**:
1. **データ整合性チェック** (行376-382)
   - `validationService.validateDataIntegrity()`
2. **ビジネスルール検証** (行385-391)
   - `validationService.validateBusinessRules()`
3. **警告処理** (行394-400)
   - 警告がある場合のログ出力

### 7.2 閾値チェックステップ (thresholdCheckStep)
**行**: 213-218, 415-435

**処理内容**:
1. **閾値チェック実行** (行421)
   - `checkThresholds()` メソッド呼び出し
2. **レコード数確認** (行473-475)
   ```sql
   SELECT COUNT(*) FROM attendance_summaries WHERE date = CURRENT_DATE
   ```
3. **閾値判定** (行477)
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
- **AttendanceSummaryRepository**: 月次集計データ操作
  - `findBySummaryTypeAndTargetDateBetween()`: 残業監視用データ取得
- **OvertimeReportRepository**: 残業レポート操作
  - `findByUserIdAndTargetMonth()`: 既存レポート取得
  - `saveAll()`: 残業レポートデータ保存

### 9.2 使用サービス
- **BatchValidationService**: バッチ検証処理
- **BatchRecoveryService**: 復旧処理
- **BatchMonitoringService**: 監視処理

## 10. 処理フロー全体図
```
[管理者] → [フロントエンド] → [BatchController]
    ↓
[BatchJobService] → [JobLauncher] → [overtimeMonitoringBatchJob]
    ↓
[preValidationStep] → [recoveryCheckStep] → [overtimeDataInitializationStep]
    ↓
[overtimeMonitoringProcessingStep]: Reader → Processor → Writer (チャンク処理)
    ↓
[postValidationStep] → [thresholdCheckStep] → [完了]
```

## 11. 重要な設定値
- **チャンクサイズ**: 10件
- **ページサイズ**: 100件
- **残業時間閾値**: 45時間/月
- **深夜労働時間閾値**: 20時間/月
- **休日労働時間閾値**: 15時間/月
- **閾値**: 10,000件
- **残業データ削除範囲**: 当月分のみ

## 12. 残業監視ロジック詳細
### 12.1 監視閾値の定義
**場所**: `OvertimeMonitoringProcessor.java` 行22-24

```java
private static final BigDecimal OVERTIME_THRESHOLD = new BigDecimal("45.00"); // 月45時間
private static final BigDecimal LATE_NIGHT_THRESHOLD = new BigDecimal("20.00"); // 月20時間
private static final BigDecimal HOLIDAY_THRESHOLD = new BigDecimal("15.00");   // 月15時間
```

### 12.2 ステータス判定ロジック
**場所**: `OvertimeMonitoringProcessor.java` 行81-100

**判定フロー**:
1. **閾値超過チェック** (行84-88)
   - いずれかの閾値を超過した場合 → "confirmed"
2. **残業時間存在チェック** (行91-95)
   - 残業時間が存在するが閾値以下 → "draft"
3. **残業時間なし** (行98)
   - 全ての残業時間が0 → "approved"

### 12.3 レポート更新ロジック
**場所**: `OvertimeMonitoringProcessor.java` 行48-58

**更新フロー**:
- **既存レポートあり**: 更新処理（updated_at を現在時刻に設定）
- **既存レポートなし**: 新規作成（created_at, updated_at を現在時刻に設定）

## 13. データベース設計対応
### 13.1 overtime_reports テーブル構造
- **id**: 主キー（BIGINT）
- **user_id**: ユーザーID（INTEGER）
- **target_month**: 対象月（DATE）
- **total_overtime**: 総残業時間（NUMERIC(38,2)）
- **total_late_night**: 総深夜労働時間（NUMERIC(38,2)）
- **total_holiday**: 総休日労働時間（NUMERIC(38,2)）
- **status**: ステータス（VARCHAR(255)）
- **created_at**: 作成日時（TIMESTAMP WITH TIME ZONE）
- **updated_at**: 更新日時（TIMESTAMP WITH TIME ZONE）

### 13.2 ステータス値の意味
- **draft**: 通常の残業（閾値以下）
- **confirmed**: 要確認（閾値超過）
- **approved**: 承認済み（残業時間なし）

## 14. トラブルシューティング
### 14.1 一般的な問題
1. **権限エラー**: 管理者権限の確認
2. **データベース接続エラー**: 接続設定の確認
3. **月次データ不足**: 月次バッチの実行状況確認
4. **重複処理**: 既存残業レポートの確認

### 14.2 ログ確認箇所
- `DailyAttendanceBatchConfig` のステップログ
- `OvertimeMonitoringProcessor` の処理ログ
- `OvertimeMonitoringReader` の読み取りログ
- `BatchDiagnosticLogger` の診断ログ

### 14.3 データ確認SQL
```sql
-- 残業レポート確認
SELECT * FROM overtime_reports 
WHERE DATE_TRUNC('month', target_month) = DATE_TRUNC('month', CURRENT_DATE);

-- 月次集計データ確認
SELECT COUNT(*) FROM attendance_summaries 
WHERE summary_type = 'monthly' 
AND DATE_TRUNC('month', target_date) = DATE_TRUNC('month', CURRENT_DATE);

-- 閾値超過レポート確認
SELECT * FROM overtime_reports 
WHERE status = 'confirmed' 
AND DATE_TRUNC('month', target_month) = DATE_TRUNC('month', CURRENT_DATE);
```

## 15. 法令遵守と健康管理
### 15.1 労働基準法対応
- **月45時間制限**: 時間外労働の上限監視
- **深夜労働管理**: 22:00-05:00の労働時間監視
- **休日労働制限**: 過度な休日出勤の防止

### 15.2 健康管理機能
- **自動アラート**: 閾値超過時の自動検出
- **ステータス管理**: 確認が必要な残業の可視化
- **継続監視**: 月次での定期的な残業状況把握

この手順書は、残業監視バッチの完全な実行フローと、関与する全ての主要クラス・メソッドを網羅しています。