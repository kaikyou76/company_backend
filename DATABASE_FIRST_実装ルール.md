# 📊 DATABASE FIRST 実装ルール

## 🎯 基本方針

**comsys_dump.sqlのデータベーススキーマに100%準拠した実装**

すべての実装は`F:\Company_system_project\company_backend\src\main\resources\comsys_dump.sql`に記載されたテーブル構造に厳格に従う。

---

## 📋 データベーススキーマ準拠チェックリスト

### ✅ **確認済み：要件定義書との整合性**

#### **1. 主要テーブル（comsys_dump.sql準拠）**
- ✅ `users` - ユーザー情報（認証・プロフィール）
- ✅ `departments` - 部署マスタ
- ✅ `positions` - 職位マスタ
- ✅ `attendance_records` - 勤怠記録（位置情報含む）
- ✅ `attendance_summaries` - 勤怠集計データ
- ✅ `leave_requests` - 休暇申請
- ✅ `holidays` - 祝日マスタ
- ✅ `system_logs` - システムログテーブル
- ✅ `notifications` - 通知テーブル
- ✅ `time_corrections` - 打刻修正申請
- ✅ `overtime_reports` - 残業レポート
- ✅ `ip_whitelist` - IP許可リスト
- ✅ `work_locations` - 勤務地マスタ

#### **2. Spring Batchテーブル（既存・活用必須）**
- ✅ `batch_job_execution` - ジョブ実行履歴
- ✅ `batch_job_instance` - ジョブインスタンス
- ✅ `batch_job_execution_context` - 実行コンテキスト
- ✅ `batch_job_execution_params` - 実行パラメータ
- ✅ `batch_step_execution` - ステップ実行履歴
- ✅ `batch_step_execution_context` - ステップコンテキスト

#### **3. テストデータ（活用必須）**
- ✅ **24名のテストユーザー** - 全権限レベル
- ✅ **68件の勤怠記録** - リアルなデータ
- ✅ **10件の休暇申請** - 承認フロー検証
- ✅ **13件のシステムログ** - ログ機能検証
- ✅ **複雑な組織階層** - 部署・役職関係

---

## 🏗️ 実装ルール

### **Rule 1: テーブル構造厳守**
```java
// ❌ 間違い：独自カラム追加
@Entity
public class User {
    private String customField; // comsys_dump.sqlにない
}

// ✅ 正解：comsys_dump.sql準拠
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "username", nullable = false, length = 50)
    private String username;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(name = "department_id")
    private Integer departmentId;
    
    @Column(name = "position_id")
    private Integer positionId;
    
    @Column(name = "manager_id")
    private Integer managerId;
    
    @Column(name = "location_type")
    private String locationType;
    
    @Column(name = "client_latitude")
    private Double clientLatitude;
    
    @Column(name = "client_longitude")
    private Double clientLongitude;
    
    // comsys_dump.sqlの全カラムを正確に実装
}
```

### **Rule 2: Spring Batch完全活用**
```java
// ✅ 必須：既存Spring Batchテーブル使用
@Configuration
@EnableBatchProcessing
public class AttendanceBatchConfig {
    
    @Bean
    public Job dailyAttendanceSummaryJob(JobRepository jobRepository) {
        return new JobBuilder("dailyAttendanceSummaryJob", jobRepository)
            .start(attendanceProcessingStep())
            .build();
    }
    
    // jobRepositoryは既存のbatch_job_*テーブルを使用
}
```

### **Rule 3: 既存データ活用**
```java
// ✅ 必須：24名のテストユーザーでテスト
@Test
public void testWithRealData() {
    // comsys_dump.sqlの実データを使用
    List<User> users = userRepository.findAll(); // 24名取得
    assertThat(users).hasSize(24);
    
    // 実際の勤怠データでテスト
    List<AttendanceRecord> records = attendanceRepository.findAll(); // 68件
    assertThat(records).hasSize(68);
}
```

### **Rule 4: カラム名・型完全一致**
```java
// ✅ comsys_dump.sql準拠
@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // bigint
    
    @Column(name = "user_id", nullable = false)
    private Integer userId; // integer
    
    @Column(name = "type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private AttendanceType type; // varchar(10) CHECK (type IN ('in', 'out'))
    
    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp; // timestamp with time zone
    
    @Column(name = "latitude", nullable = false)
    private Double latitude; // double precision
    
    @Column(name = "longitude", nullable = false)
    private Double longitude; // double precision
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt; // timestamp with time zone DEFAULT now()
}
```

### **Rule 5: 制約条件準拠**
```java
// ✅ CHECK制約の実装
public enum AttendanceType {
    @JsonValue("in")
    IN("in"),
    
    @JsonValue("out") 
    OUT("out");
    
    // comsys_dump.sqlのCHECK制約に準拠
    // CHECK (((type)::text = ANY ((ARRAY['in'::character varying, 'out'::character varying])::text[])))
}

public enum SummaryType {
    @JsonValue("daily")
    DAILY("daily"),
    
    @JsonValue("monthly")
    MONTHLY("monthly");
    
    // CHECK (((summary_type)::text = ANY ((ARRAY['daily'::character varying, 'monthly'::character varying])::text[])))
}
```

---

## 🚫 禁止事項

### **❌ 絶対禁止**
1. **テーブル構造の変更** - comsys_dump.sqlと異なる構造
2. **カラム追加・削除** - 既存スキーマの改変
3. **データ型変更** - bigint → Long以外の型使用
4. **制約無視** - CHECK制約、NOT NULL制約の無視
5. **Spring Batch無視** - 既存テーブルを使わない実装
6. **テストデータ無視** - 24名のユーザーを使わないテスト

### **❌ 避けるべき実装**
```java
// ❌ 間違い：独自テーブル作成
@Entity
@Table(name = "custom_batch_jobs") // comsys_dump.sqlにない
public class CustomBatchJob { }

// ❌ 間違い：カラム名変更
@Column(name = "employee_id") // comsys_dump.sqlでは"user_id"
private Integer employeeId;

// ❌ 間違い：型変更
@Column(name = "id")
private String id; // comsys_dump.sqlではbigint

// ❌ 間違い：制約無視
@Column(name = "type")
private String type; // CHECK制約を無視
```

---

## ✅ 実装検証チェックリスト

### **Phase 1: Entity設計検証**
- [ ] 全Entityがcomsys_dump.sqlのテーブルに対応
- [ ] 全カラムが正確に実装されている
- [ ] データ型が完全一致している
- [ ] 制約条件が実装されている
- [ ] 外部キー関係が正しく設定されている

### **Phase 2: Spring Batch検証**
- [ ] 既存batch_job_*テーブルを使用
- [ ] JobRepositoryが正しく設定されている
- [ ] 実際のバッチ実行でテーブルが更新される
- [ ] バッチ履歴が正しく記録される

### **Phase 3: データ検証**
- [ ] 24名のテストユーザーが正しく取得できる
- [ ] 68件の勤怠記録が正しく処理できる
- [ ] 既存データとの整合性が保たれている
- [ ] 実データでの動作確認が完了している

### **Phase 4: API検証**
- [ ] 全APIが既存データで正常動作する
- [ ] 要件定義書のAPIエンドポイントが実装されている
- [ ] 権限マトリクスが正しく実装されている
- [ ] エラーハンドリングが適切に実装されている

---

## 📝 実装優先度

### **最高優先度（即座に実装）**
1. **Entity層** - comsys_dump.sql完全準拠
2. **Repository層** - JPA Repository実装
3. **Spring Batch設定** - 既存テーブル活用
4. **基本API** - 要件定義書準拠

### **高優先度（1週間以内）**
1. **認証・登録機能** - JWT + 既存usersテーブル
2. **勤怠管理機能** - attendance_records/summariesテーブル
3. **休暇管理機能** - leave_requestsテーブル
4. **システムログ機能** - system_logsテーブル

### **中優先度（2週間以内）**
1. **バッチ処理** - Spring Batch完全実装
2. **レポート機能** - 既存データ活用
3. **管理機能** - departments/positionsテーブル

---

## 🎯 成功基準

### **技術基準**
- ✅ comsys_dump.sqlとの100%整合性
- ✅ 既存テストデータでの完全動作
- ✅ Spring Batch既存テーブル完全活用
- ✅ 要件定義書API完全実装

### **品質基準**
- ✅ 実データでの包括的テスト
- ✅ データ整合性の完全保証
- ✅ パフォーマンス要件達成
- ✅ セキュリティ要件達成

**この実装ルールに従うことで、データベーススキーマとの完全な整合性を保ち、既存データを最大限活用した堅牢なシステムを構築します。**
