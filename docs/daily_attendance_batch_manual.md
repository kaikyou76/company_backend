# 日次打刻集計バッチ 完全手順書

## 概要
本書は、管理者が日次打刻集計ボタンを押してから実行される日次打刻集計バッチの完全な処理フローを、関与する全てのクラスとメソッドと共に詳細に記載したものです。

## 1. エントリーポイント
### 1.1 管理者アクション
- 管理者がフロントエンドで「日次打刻集計」ボタンをクリック
- フロントエンドから `POST /api/batch/daily-summary` エンドポイントが呼び出される

### 1.2 BatchController.java 
**場所**: `src/main/java/com/example/companybackend/controller/BatchController.java`

**注意**: 実際のコードでは `/daily-summary` エンドポイントが見当たりませんが、月次集計パターンに基づく想定フロー

```java
// 想定される日次集計エンドポイント（月次集計パターンに基づく）
@PostMapping("/daily-summary")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<BatchResponseDto.DailySummaryBatchResponse> executeDailySummaryBatch(
        @RequestBody(required = false) Map<String, Object> parameters)
```

**処理内容**:
1. 管理者権限チェック (`@PreAuthorize("hasRole('ADMIN')")`)
2. パラメータ受け取り
3. BatchJobService の呼び出し
4. レスポンス生成とエラーハンドリング

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

#### ジョブ定義: `dailyAttendanceSummaryJob()`
**行**: 73-84

**ジョブフロー**:
```
dailyAttendanceSummaryJob
├── preValidationStep()         (事前検証)
├── recoveryCheckStep()         (復旧チェック)
├── dataInitializationStep()    (データ初期化)
├── attendanceProcessingStep()  (メイン処理)
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
**行**: 169-174, 303-321

**Tasklet**: `dataInitializationTasklet()`
**処理内容**:
1. **既存集計データ削除** (行309)
   ```sql
   DELETE FROM attendance_summaries WHERE date = CURRENT_DATE
   ```
2. **データアクセス例外ハンドリング** (行313-315)

#### 3.2.4 出席処理ステップ (attendanceProcessingStep) - メイン処理
**行**: 177-185

**チャンク処理設定**:
- **チャンクサイズ**: 10件
- **Reader**: `attendanceRecordItemReader()`
- **Processor**: `dailyWorkTimeProcessor()`
- **Writer**: `attendanceSummaryWriter()`

## 4. データ読み取り層 (Reader)
### 4.1 AttendanceRecordReader.java
**場所**: `src/main/java/com/example/companybackend/batch/reader/AttendanceRecordReader.java`

#### メソッド: `reader()`
**行**: 24-37

**処理内容**:
1. **ソート設定** (行26-27)
   - IDの昇順でソート
2. **RepositoryItemReader構築** (行30-36)
   - リポジトリ: `attendanceRecordRepository`
   - メソッド名: `findAll`
   - ページサイズ: 100件
   - リーダー名: `attendanceRecordReader`

## 5. データ処理層 (Processor)
### 5.1 DailyWorkTimeProcessor.java
**場所**: `src/main/java/com/example/companybackend/batch/processor/DailyWorkTimeProcessor.java`

#### メソッド: `process(AttendanceRecord attendanceRecord)`
**行**: 43-78

**処理フロー**:
1. **入力レコード検証** (行45-47)
   - 'in'タイプのレコードのみ処理
   - 'out'レコードは null を返して処理スキップ

2. **ユーザー情報取得** (行49-50)
   - ユーザーID と対象日付の抽出

3. **日次レコード取得** (行53)
   - `attendanceRecordRepository.findByUserIdAndDate(userId, targetDate)`

4. **レコードソート** (行56)
   - タイムスタンプ順でソート

5. **ペアレコード検証** (行59-61)
   - 2件未満の場合は null を返してスキップ

6. **工作時間計算** (行64)
   - `calculateWorkTime(dailyRecords)` メソッド呼び出し

7. **AttendanceSummary作成** (行67-76)
   - 各種時間データの設定
   - サマリータイプ: "daily"
   - 作成日時: 現在時刻

#### メソッド: `calculateWorkTime(List<AttendanceRecord> records)`
**行**: 85-137

**処理内容**:
1. **ペアレコード処理** (行92-116)
   - 'in'と'out'レコードのペアリング
   - 工作分钟数計算 (行99-102)
   - 深夜工作時間計算 (行108-111)

2. **時間変換** (行119)
   - 分钟 → 時間 (小数点以下2位)

3. **残業時間計算** (行122-125)
   - 標準8時間を超える部分

4. **深夜工作時間変換** (行128)
   - 分钟 → 時間

5. **休日工作時間計算** (行131-134)
   - `calculateHolidayHours()` メソッド呼び出し

#### メソッド: `calculateLateNightMinutes(LocalDateTime inTime, LocalDateTime outTime)`
**行**: 145-187

**処理ロジック**:
1. **日付範囲処理** (行148-152)
   - 開始日から終了日まで日別に処理

2. **各日の深夜時間計算**:
   - **深夜時間帯1: 22:00-23:59** (行157-169)
   - **深夜時間帯2: 00:00-05:00** (行171-183)

3. **重複時間計算** (行162-168, 176-182)
   - 実際の工作時間と深夜時間帯の重複部分を計算

#### メソッド: `calculateHolidayHours(LocalDate workDate, BigDecimal totalHours)`
**行**: 195-210

**処理内容**:
1. **週末チェック** (行197-199)
   - 土曜日(6)、日曜日(7)の判定
2. **法定休日チェック** (行202-207)
   - `holidayRepository.findAll()` で休日リスト取得
   - 日付マッチング判定

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
**行**: 199-204, 325-366

**処理内容**:
1. **データ整合性チェック** (行331-337)
   - `validationService.validateDataIntegrity()`
2. **ビジネスルール検証** (行340-346)
   - `validationService.validateBusinessRules()`
3. **警告処理** (行349-355)
   - 警告がある場合のログ出力

### 7.2 閾値チェックステップ (thresholdCheckStep)
**行**: 207-212, 370-405

**処理内容**:
1. **閾値チェック実行** (行376)
   - `checkThresholds()` メソッド呼び出し
2. **レコード数確認** (行396-397)
   ```sql
   SELECT COUNT(*) FROM attendance_summaries WHERE date = CURRENT_DATE
   ```
3. **閾値判定** (行400)
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
- **AttendanceSummaryRepository**: 集計データ操作
- **HolidayRepository**: 休日データ取得

### 9.2 使用サービス
- **BatchValidationService**: バッチ検証処理
- **BatchRecoveryService**: 復旧処理
- **BatchMonitoringService**: 監視処理

## 10. 処理フロー全体図
```
[管理者] → [フロントエンド] → [BatchController]
    ↓
[BatchJobService] → [JobLauncher] → [dailyAttendanceSummaryJob]
    ↓
[preValidationStep] → [recoveryCheckStep] → [dataInitializationStep]
    ↓
[attendanceProcessingStep]: Reader → Processor → Writer (チャンク処理)
    ↓
[postValidationStep] → [thresholdCheckStep] → [完了]
```

## 11. 重要な設定値
- **チャンクサイズ**: 10件
- **ページサイズ**: 100件
- **標準工作時間**: 8時間
- **深夜時間帯**: 22:00-05:00
- **閾値**: 10,000件
- **集計データ削除範囲**: 当日分のみ

## 12. トラブルシューティング
### 12.1 一般的な問題
1. **権限エラー**: 管理者権限の確認
2. **データベース接続エラー**: 接続設定の確認
3. **メモリ不足**: チャンクサイズの調整
4. **処理タイムアウト**: 深夜時間計算の無限ループチェック

### 12.2 ログ確認箇所
- `DailyAttendanceBatchConfig` のステップログ
- `DailyWorkTimeProcessor` の処理ログ
- `BatchDiagnosticLogger` の診断ログ

この手順書は、日次打刻集計バッチの完全な実行フローと、関与する全ての主要クラス・メソッドを網羅しています。