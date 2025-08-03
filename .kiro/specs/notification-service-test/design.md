# NotificationService 設計書

## 概要
comsys_test_dump.sqlのデータベース構造に基づいて、NotificationServiceの包括的な通知機能を設計します。既存のnotificationsテーブルを活用し、追加のテーブルで機能を拡張します。

## アーキテクチャ

### 既存テーブル構造のみ使用
```sql
-- 既存のnotificationsテーブル
CREATE TABLE public.notifications (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    title character varying(255) NOT NULL,
    message text NOT NULL,
    type character varying(255) NOT NULL,
    is_read boolean DEFAULT false NOT NULL,
    related_id integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT notifications_type_check CHECK (type IN ('leave', 'correction', 'system'))
);

-- 既存のusersテーブル
CREATE TABLE public.users (
    id integer NOT NULL,
    username character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    -- その他のフィールド
);

-- 既存のleave_requestsテーブル
CREATE TABLE public.leave_requests (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    type character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    reason text,
    approver_id integer,
    approved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);

-- 既存のtime_correctionsテーブル
CREATE TABLE public.time_corrections (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    attendance_id bigint NOT NULL,
    request_type character varying(255) NOT NULL,
    before_time timestamp with time zone NOT NULL,
    current_type character varying(255) NOT NULL,
    requested_time timestamp with time zone,
    requested_type character varying(255),
    reason text NOT NULL,
    status character varying(255) NOT NULL,
    approver_id integer,
    approved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);
```

## コンポーネントと インターフェース

### NotificationService (既存テーブルのみ使用)
```java
@Service
public class NotificationService {
    // 休暇申請通知
    void sendLeaveRequestNotification(LeaveRequest leaveRequest, User user);
    void sendLeaveApprovalNotification(LeaveRequest leaveRequest, User user);
    void sendLeaveRejectionNotification(LeaveRequest leaveRequest, User user);
    
    // 時刻修正申請通知
    void sendTimeCorrectionNotification(TimeCorrection timeCorrection, User user);
    void sendTimeCorrectionApprovalNotification(TimeCorrection timeCorrection, User user);
    void sendTimeCorrectionRejectionNotification(TimeCorrection timeCorrection, User user);
    
    // システム内通知管理（notificationsテーブル使用）
    List<Notification> getUserNotifications(Long userId, boolean unreadOnly);
    void markAsRead(Long notificationId);
    void markAllAsRead(Long userId);
    void deleteOldNotifications(int daysOld);
    
    // 通知統計（notificationsテーブルから集計）
    Map<String, Object> getNotificationStatistics(LocalDate startDate, LocalDate endDate);
    
    // メール通知（シンプルな実装）
    void sendEmailNotification(String toEmail, String subject, String body);
    
    // 通知テンプレート（ハードコード）
    String createNotificationTitle(String type, Map<String, Object> variables);
    String createNotificationMessage(String type, Map<String, Object> variables);
    
    // 重複防止（メモリベース）
    boolean isDuplicateNotification(String content, Long userId, Duration timeWindow);
}
```

## データモデル

### 既存エンティティのみ使用

#### Notification エンティティ (既存)
```java
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "message", nullable = false)
    private String message;
    
    @Column(name = "type", nullable = false)
    private String type; // 'leave', 'correction', 'system'
    
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    @Column(name = "related_id")
    private Integer relatedId;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
```

#### User エンティティ (既存)
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "username", nullable = false)
    private String username;
    
    @Column(name = "email", nullable = false)
    private String email;
    
    // その他の既存フィールド
}
```

#### LeaveRequest エンティティ (既存)
```java
@Entity
@Table(name = "leave_requests")
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "type", nullable = false)
    private String type;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    // その他の既存フィールド
}
```

#### TimeCorrection エンティティ (既存)
```java
@Entity
@Table(name = "time_corrections")
public class TimeCorrection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    // その他の既存フィールド
}
```

## エラーハンドリング

### 通知送信エラー
- システム内通知失敗: ログ記録、履歴に失敗記録
- メール送信失敗: 再送信キューに追加、最大3回まで再試行
- テンプレート不正: デフォルトテンプレート使用
- ユーザー設定取得失敗: デフォルト設定適用

### データベースエラー
- 通知保存失敗: ログ記録、例外スロー
- 通知取得失敗: 空リスト返却、ログ記録
- ユーザー情報取得失敗: デフォルト値使用

### システム負荷制御
- メモリベースの重複チェック（ConcurrentHashMap使用）
- 古い通知の定期削除（30日以上）
- メール送信失敗時のログ記録

## テスト戦略

### 単体テスト
- 各サービスクラスの個別機能テスト
- モックを使用した依存関係の分離
- エラーケースの網羅的テスト

### 統合テスト
- 実データベースとの統合テスト
- comsys_test_dump.sqlの実データ活用
- トランザクション境界の確認

### パフォーマンステスト
- 大量通知送信時の性能確認
- メモリ使用量の監視
- データベース負荷の測定

### エンドツーエンドテスト
- 申請作成から通知送信までの完全フロー
- メール送信を含む実際の通知配信
- 通知設定変更の反映確認