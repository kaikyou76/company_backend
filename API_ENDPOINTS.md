# API エンドポイント仕様書

## 📌 認証方式

全ての API は**Bearer Token 認証**を使用します。

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

## 📌 共通レスポンス形式

```json
// 成功レスポンス
{
  "success": true,
  "message": "処理が完了しました",
  "data": { /* レスポンスデータ */ }
}

// エラーレポート
{
  "success": false,
  "message": "エラーメッセージ",
  "errors": { /* バリデーションエラー詳細 */ }
}
```

## 📌 HTTP ステータスコード

- **200 OK**: 成功
- **201 Created**: リソース作成成功
- **400 Bad Request**: リクエストエラー
- **401 Unauthorized**: 認証エラー
- **403 Forbidden**: 権限エラー
- **404 Not Found**: リソースが見つからない
- **500 Internal Server Error**: サーバーエラー

---

## 🔐 認証関連 API

### POST /api/auth/login

```json
// Request
{
  "employeeCode": "user@example.com",
  "password": "password123"
}

// Response
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400,
  "user": {
    "id": 1,
    "name": "user@example.com",
    "departmentId": 1,
    "departmentName": "開発部",
    "positionId": 1,
    "positionName": "エンジニア",
    "role": "EMPLOYEE",
    "locationType": "office"
  }
}

// Error Response
{
  "success": false,
  "message": "認証情報が正しくありません"
}
```

### POST /api/auth/refresh

```json
// Request
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}

// Response
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400
}

// Error Response
{
  "success": false,
  "message": "リフレッシュトークンが無効です"
}
```

### POST /api/auth/logout

```json
// Request Headers
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...

// Request Body (empty)
{}

// Response
{
  "success": true,
  "message": "ログアウトしました"
}

// Error Response
{
  "success": false,
  "message": "無効なトークンです"
}
```

## ⏰ 勤怠関連 API

### POST /api/attendance/clock-in

```json
// Request
{
  "latitude": 35.6895,
  "longitude": 139.6917
}

// Response
{
  "success": true,
  "message": "出勤打刻が完了しました",
  "data": {
    "recordId": 12345,
    "timestamp": "2025-01-18T09:00:00+09:00",
    "locationVerified": true
  }
}
```

### POST /api/attendance/clock-out

```json
// Request
{
  "latitude": 35.6895,
  "longitude": 139.6917
}

// Response
{
  "success": true,
  "message": "退勤打刻が完了しました",
  "data": {
    "recordId": 12345,
    "timestamp": "2025-01-18T18:00:00+09:00",
    "workingHours": "8.5",
    "overtimeHours": "0.5"
  }
}

// Error Response
{
  "success": false,
  "message": "出勤打刻がされていません"
}
```

### GET /api/attendance/records

```json
// Request Parameters
// ?startDate=2025-01-01&endDate=2025-01-31&page=0&size=10

// Response
{
  "success": true,
  "data": {
    "records": [
      {
        "id": 12345,
        "date": "2025-01-18",
        "clockInTime": "09:00:00",
        "clockOutTime": "18:00:00",
        "workingHours": 8.5,
        "overtimeHours": 0.5,
        "status": "NORMAL"
      }
    ],
    "totalCount": 20,
    "currentPage": 0,
    "totalPages": 2
  }
}
```

### GET /api/attendance/daily-summary

```json
// Request Parameters
// ?date=2025-01-18

// Response
{
  "success": true,
  "data": {
    "date": "2025-01-18",
    "clockInTime": "09:00:00",
    "clockOutTime": "18:00:00",
    "workingHours": 8.5,
    "overtimeHours": 0.5,
    "breakTime": 1.0,
    "status": "NORMAL",
    "monthlyWorkingHours": 160.5,
    "monthlyOvertimeHours": 8.5
  }
}
```

## 🏖️ 休暇関連 API

### POST /api/leave/request

```json
// Request
{
  "leaveType": "PAID_LEAVE",
  "startDate": "2025-02-01",
  "endDate": "2025-02-03",
  "reason": "家族旅行のため",
  "substituteUserId": 2,
  "emergencyContact": "090-1234-5678"
}

// Response
{
  "success": true,
  "message": "休暇申請を作成しました",
  "data": {
    "id": 1001,
    "leaveType": "PAID_LEAVE",
    "startDate": "2025-02-01",
    "endDate": "2025-02-03",
    "days": 3,
    "approvalStatus": "PENDING",
    "remainingPaidLeaveDays": 17
  }
}
```

### GET /api/leave/my-requests

```json
// Request Parameters
// ?startDate=2025-01-01&endDate=2025-12-31&leaveType=PAID_LEAVE

// Response
{
  "success": true,
  "data": {
    "requests": [
      {
        "id": 1001,
        "leaveType": "PAID_LEAVE",
        "leaveTypeDisplayName": "有給休暇",
        "startDate": "2025-02-01",
        "endDate": "2025-02-03",
        "days": 3,
        "approvalStatus": "PENDING",
        "approvalStatusDisplayName": "承認待ち"
      }
    ],
    "totalCount": 5,
    "remainingPaidLeaveDays": 17
  }
}
```

### GET /api/leave/pending-approval

```json
// Response (管理者限定)
{
  "success": true,
  "data": {
    "requests": [
      {
        "id": 1001,
        "employeeCode": "E12345",
        "employeeName": "田中太郎",
        "leaveType": "PAID_LEAVE",
        "startDate": "2025-02-01",
        "endDate": "2025-02-03",
        "days": 3,
        "reason": "家族旅行のため",
        "substituteUserId": 2,
        "substituteName": "佐藤花子"
      }
    ],
    "totalCount": 3
  }
}
```

### POST /api/leave/{id}/approve

```json
// Request
{
  "comment": "承認します。良い休暇をお過ごしください。"
}

// Response
{
  "success": true,
  "message": "休暇申請を承認しました",
  "data": {
    "id": 1001,
    "approvalStatus": "APPROVED",
    "approvedBy": 1,
    "approvedAt": "2025-01-18T10:30:00+09:00",
    "approvalComment": "承認します。良い休暇をお過ごしください。"
  }
}
```

## 📊 レポート関連 API

### GET /api/reports/attendance/monthly

```json
// Request Parameters
// ?year=2025&month=1&departmentId=1

// Response (CSV File Download)
Content-Type: text/csv; charset=UTF-8
Content-Disposition: attachment; filename="monthly_attendance_202501.csv"

// CSV Content:
"社員コード","氏名","部署","出勤日数","実働時間","残業時間","遅刻回数","早退回数"
"E12345","田中太郎","開発部","20","160.0","8.5","0","0"

// Error Response
{
  "success": false,
  "message": "レポート出力に失敗しました"
}
```

### GET /api/reports/attendance/records

```json
// Request Parameters
// ?startDate=2025-01-01&endDate=2025-01-31&departmentId=1

// Response (CSV File Download)
Content-Type: text/csv; charset=UTF-8
Content-Disposition: attachment; filename="attendance_records_20250101_20250131.csv"
```

### GET /api/reports/overtime

```json
// Request Parameters
// ?year=2025&month=1&departmentId=1

// Response (CSV File Download)
Content-Type: text/csv; charset=UTF-8
Content-Disposition: attachment; filename="overtime_report_202501.csv"
```

### GET /api/reports/paid-leave

```json
// Request Parameters
// ?startDate=2025-01-01&endDate=2025-12-31&departmentId=1

// Response (CSV File Download)
Content-Type: text/csv; charset=UTF-8
Content-Disposition: attachment; filename="paid_leave_report_20250101_20251231.csv"
```

### GET /api/reports/list

```json
// Response
{
  "success": true,
  "data": {
    "reports": {
      "attendance": {
        "monthly": {
          "name": "月次勤怠レポート",
          "endpoint": "/api/reports/attendance/monthly",
          "method": "GET"
        },
        "records": {
          "name": "打刻履歴レポート",
          "endpoint": "/api/reports/attendance/records",
          "method": "GET"
        }
      },
      "overtime": {
        "name": "残業時間レポート",
        "endpoint": "/api/reports/overtime",
        "method": "GET"
      }
    },
    "userRole": "ADMIN",
    "isAdmin": true
  }
}
```

## 👤 ユーザー関連 API

### GET /api/users/profile

```json
// Response
{
  "success": true,
  "data": {
    "id": 1,
    "employeeCode": "E12345",
    "name": "田中太郎",
    "email": "tanaka@company.com",
    "role": "EMPLOYEE",
    "department": "開発部",
    "position": "エンジニア",
    "hireDate": "2023-04-01",
    "phoneNumber": "090-1234-5678",
    "remainingPaidLeave": 20
  }
}
```

### PUT /api/users/profile

```json
// Request
{
  "name": "田中太郎",
  "email": "tanaka@company.com",
  "phoneNumber": "090-1234-5678",
  "emergencyContact": "090-9876-5432"
}

// Response
{
  "success": true,
  "message": "プロフィールを更新しました",
  "data": {
    "id": 1,
    "name": "田中太郎",
    "email": "tanaka@company.com",
    "phoneNumber": "090-1234-5678",
    "updatedAt": "2025-01-18T10:30:00+09:00"
  }
}
```

### GET /api/users/list (ADMIN only)

```json
// Request Parameters
// ?page=0&size=10&department=開発部&active=true

// Response
{
  "success": true,
  "data": {
    "users": [
      {
        "id": 1,
        "employeeCode": "E12345",
        "name": "田中太郎",
        "email": "tanaka@company.com",
        "role": "EMPLOYEE",
        "department": "開発部",
        "position": "エンジニア",
        "isActive": true,
        "hireDate": "2023-04-01"
      }
    ],
    "totalCount": 50,
    "currentPage": 0,
    "totalPages": 5
  }
}
```

---

## 🔄 バッチ処理関連 API

### POST /api/batch/daily-summary

**日次勤怠集計バッチの実行**

```json
// Request
{
  "targetDate": "2025-02-08"  // オプション（省略時は当日）
}

// Response
{
  "success": true,
  "message": "日次勤怠集計バッチを実行しました",
  "data": {
    "targetDate": "2025-02-08",
    "processedCount": 45,
    "userCount": 25,
    "totalWorkTime": 2160,
    "totalOvertimeTime": 320,
    "totalLateNightTime": 180,
    "totalHolidayTime": 0,
    "averageWorkHours": 8.6,
    "processingWarnings": []
  },
  "executedAt": "2025-02-08T10:30:00+09:00"
}

// Error Response
{
  "success": false,
  "message": "日次勤怠集計バッチの実行に失敗しました: パラメータが不正です",
  "executedAt": "2025-02-08T10:30:00+09:00"
}
```

### POST /api/batch/monthly-summary

**月次勤怠集計バッチの実行**

```json
// Request
{
  "targetMonth": "2025-01"  // オプション（省略時は前月）
}

// Response
{
  "success": true,
  "message": "月次勤怠集計バッチを実行しました",
  "data": {
    "targetMonth": "2025-01",
    "processedCount": 156,
    "userCount": 25,
    "totalWorkDays": 520,
    "totalWorkTime": 83200,
    "totalOvertimeTime": 6800
  },
  "executedAt": "2025-02-08T10:30:00+09:00"
}

// Error Response
{
  "success": false,
  "message": "月次勤怠集計バッチの実行に失敗しました: パラメータが不正です",
  "executedAt": "2025-02-08T10:30:00+09:00"
}
```

### POST /api/batch/update-paid-leave

**有給日数更新バッチの実行**

```json
// Request
{
  "fiscalYear": 2025  // オプション（省略時は現在年度）
}

// Response
{
  "success": true,
  "message": "有給日数更新バッチを実行しました",
  "data": {
    "fiscalYear": 2025,
    "updatedCount": 48,
    "totalUserCount": 50,
    "successRate": 96.0,
    "errorCount": 2,
    "errorMessages": [
      "ユーザーID: 15, エラー: 入社日が未設定です"
    ]
  },
  "executedAt": "2025-02-08T10:30:00+09:00"
}

// Error Response
{
  "success": false,
  "message": "有給日数更新バッチの実行に失敗しました: データベース接続エラー",
  "executedAt": "2025-02-08T10:30:00+09:00"
}
```

### POST /api/batch/cleanup-data

**データクリーンアップバッチの実行**

```json
// Request
{
  "retentionMonths": 12  // オプション（デフォルト: 12ヶ月）
}

// Response
{
  "success": true,
  "message": "データクリーンアップバッチを実行しました",
  "data": {
    "retentionMonths": 12,
    "cutoffDate": "2024-02-08",
    "deletedCount": 0,
    "deletedDetails": {
      "system_logs": 0
    }
  },
  "executedAt": "2025-02-08T10:30:00+09:00"
}

// Error Response
{
  "success": false,
  "message": "データクリーンアップバッチの実行に失敗しました: 権限不足",
  "executedAt": "2025-02-08T10:30:00+09:00"
}
```

### POST /api/batch/repair-data

**データ修復バッチの実行**

```json
// Request
{} // パラメータなし

// Response
{
  "success": true,
  "message": "データ修復バッチを実行しました",
  "data": {
    "repairedItems": []
  },
  "executedAt": "2025-02-08T10:30:00+09:00"
}

// Error Response
{
  "success": false,
  "message": "データ修復バッチの実行に失敗しました: システムエラー",
  "executedAt": "2025-02-08T10:30:00+09:00"
}
```

### POST /api/batch/overtime-monitoring

**残業監視バッチの実行**

```json
// Request
{
  "targetMonth": "2025-02"  // オプション（省略時は当月）
}

// Response
{
  "success": true,
  "message": "残業監視バッチを実行しました",
  "data": {
    "targetMonth": "2025-02",
    "processedCount": 25,
    "userCount": 25,
    "overtimeReportsGenerated": 18,
    "highOvertimeAlerts": 3,
    "confirmedReports": 3,
    "draftReports": 15,
    "approvedReports": 7,
    "processingWarnings": []
  },
  "executedAt": "2025-02-08T10:30:00+09:00"
}

// Error Response
{
  "success": false,
  "message": "残業監視バッチの実行に失敗しました: システムエラー",
  "executedAt": "2025-02-08T10:30:00+09:00"
}
```

### GET /api/batch/status

**バッチ処理ステータスの取得**

```json
// Response
{
  "systemStatus": "HEALTHY",
  "lastChecked": "2025-02-08T10:30:00+09:00",
  "uptime": "5 days, 12 hours",
  "databaseStatus": {
    "totalUsers": 50,
    "activeUsers": 48,
    "totalAttendanceRecords": 12450,
    "latestRecordDate": "2025-02-08"
  },
  "dataStatistics": {
    "currentMonthRecords": 520,
    "incompleteRecords": 2
  },
  "recentBatchExecutions": [
    {
      "type": "MONTHLY_SUMMARY",
      "executedAt": "2025-02-01T02:00:00+09:00",
      "status": "SUCCESS",
      "duration": "45 seconds"
    },
    {
      "type": "CLEANUP_DATA",
      "executedAt": "2025-01-31T01:00:00+09:00",
      "status": "SUCCESS",
      "duration": "2 minutes"
    }
  ]
}

// Error Response
{
  "success": false,
  "message": "バッチ処理ステータスの取得に失敗しました"
}
```

---

## 🔄 バッチ管理API

### GET /api/v1/batch/instances

**全ジョブインスタンス取得**

```json
// Response
{
  "success": true,
  "totalCount": 2,
  "instances": [
    {
      "jobInstanceId": 1,
      "jobName": "monthlyAttendanceSummaryJob"
    },
    {
      "jobInstanceId": 2,
      "jobName": "dailyAttendanceSummaryJob"
    }
  ]
}

// Error Response
{
  "success": false,
  "message": "ジョブインスタンス取得に失敗しました: Database connection error"
}
```

### GET /api/v1/batch/executions/{jobName}

**ジョブ実行履歴取得**

```json
// Request Parameters
// - jobName (path parameter)
// - page (optional query parameter, default: 0)
// - size (optional query parameter, default: 20)

// Response
{
  "success": true,
  "jobName": "monthlyAttendanceSummaryJob",
  "page": 0,
  "size": 20,
  "totalCount": 1,
  "executions": [
    {
      "jobExecutionId": 1,
      "jobInstanceId": 1,
      "startTime": "2025-01-01T02:00:00+09:00",
      "endTime": "2025-01-01T02:00:45+09:00",
      "status": "COMPLETED",
      "exitCode": "COMPLETED"
    }
  ]
}

// Error Response
{
  "success": false,
  "message": "ジョブ実行履歴取得に失敗しました: Job not found"
}
```

### GET /api/v1/batch/steps/{jobExecutionId}

**ステップ実行履歴取得**

```json
// Request Parameters
// - jobExecutionId (path parameter)

// Response
{
  "success": true,
  "jobExecutionId": 1,
  "totalCount": 2,
  "steps": [
    {
      "stepExecutionId": 1,
      "stepName": "processUsersStep",
      "startTime": "2025-01-01T02:00:00+09:00",
      "endTime": "2025-01-01T02:00:30+09:00",
      "status": "COMPLETED",
      "commitCount": 5,
      "readCount": 50,
      "writeCount": 50,
      "exitCode": "COMPLETED"
    },
    {
      "stepExecutionId": 2,
      "stepName": "generateReportStep",
      "startTime": "2025-01-01T02:00:30+09:00",
      "endTime": "2025-01-01T02:00:45+09:00",
      "status": "COMPLETED",
      "commitCount": 1,
      "readCount": 1,
      "writeCount": 1,
      "exitCode": "COMPLETED"
    }
  ]
}

// Error Response
{
  "success": false,
  "message": "ステップ実行履歴取得に失敗しました: Execution not found"
}
```

### GET /api/v1/batch/statistics

**バッチ実行統計取得**

```json
// Response
{
  "success": true,
  "statistics": {
    "totalJobs": 5,
    "successRate": 100.0,
    "errorRate": 0.0
  }
}

// Error Response
{
  "success": false,
  "message": "バッチ統計取得に失敗しました: Service unavailable"
}
```

### GET /api/v1/batch/running

**実行中ジョブ取得**

```json
// Response
{
  "success": true,
  "totalCount": 1,
  "runningJobs": [
    {
      "jobExecutionId": 3,
      "jobName": "dataCleanupJob",
      "status": "STARTED"
    }
  ]
}

// Error Response
{
  "success": false,
  "message": "実行中ジョブ取得に失敗しました: Service error"
}
```

### GET /api/v1/batch/job-names

**ジョブ名一覧取得**

```json
// Response
{
  "success": true,
  "totalCount": 5,
  "jobNames": [
    "dailyAttendanceSummaryJob",
    "monthlyAttendanceSummaryJob",
    "paidLeaveUpdateJob",
    "dataCleanupJob",
    "dataRepairJob"
  ]
}

// Error Response
{
  "success": false,
  "message": "ジョブ名一覧取得に失敗しました: Database error"
}
```

### GET /api/v1/batch/latest/{jobName}

**特定ジョブの最新実行情報取得**

```json
// Request Parameters
// - jobName (path parameter)

// Response
{
  "success": true,
  "jobName": "monthlyAttendanceSummaryJob",
  "latestExecution": {
    "jobExecutionId": 1,
    "jobName": "monthlyAttendanceSummaryJob",
    "status": "COMPLETED"
  }
}

// Error Response
{
  "success": false,
  "message": "最新ジョブ実行情報取得に失敗しました: Job not found"
}
```

## 👤 ユーザー関連 API

### GET /api/users/profile

```json
// Response
{
  "success": true,
  "data": {
    "id": 1,
    "employeeCode": "E12345",
    "name": "田中太郎",
    "email": "tanaka@company.com",
    "role": "EMPLOYEE",
    "department": "開発部",
    "position": "エンジニア",
    "hireDate": "2023-04-01",
    "phoneNumber": "090-1234-5678",
    "remainingPaidLeave": 20
  }
}
```

### PUT /api/users/profile

```json
// Request
{
  "name": "田中太郎",
  "email": "tanaka@company.com",
  "phoneNumber": "090-1234-5678",
  "emergencyContact": "090-9876-5432"
}

// Response
{
  "success": true,
  "message": "プロフィールを更新しました",
  "data": {
    "id": 1,
    "name": "田中太郎",
    "email": "tanaka@company.com",
    "phoneNumber": "090-1234-5678",
    "updatedAt": "2025-01-18T10:30:00+09:00"
  }
}
```

### GET /api/users/list (ADMIN only)

```json
// Request Parameters
// ?page=0&size=10&department=開発部&active=true

// Response
{
  "success": true,
  "data": {
    "users": [
      {
        "id": 1,
        "employeeCode": "E12345",
        "name": "田中太郎",
        "email": "tanaka@company.com",
        "role": "EMPLOYEE",
        "department": "開発部",
        "position": "エンジニア",
        "isActive": true,
        "hireDate": "2023-04-01"
      }
    ],
    "totalCount": 50,
    "currentPage": 0,
    "totalPages": 5
  }
}
```

---

## 📊 API 権限マトリクス

| API カテゴリ   | エンドポイント                | ADMIN | MANAGER | EMPLOYEE |
| -------------- | ----------------------------- | ----- | ------- | -------- |
| 認証           | `/api/auth/*`                 | ✅    | ✅      | ✅       |
| 勤怠管理       | `/api/attendance/*`           | ✅    | ✅      | ✅       |
| 休暇管理       | `/api/leave/request`          | ✅    | ✅      | ✅       |
| 休暇管理       | `/api/leave/my-requests`      | ✅    | ✅      | ✅       |
| 休暇管理       | `/api/leave/pending-approval` | ✅    | ✅      | ❌       |
| 休暇管理       | `/api/leave/*/approve`        | ✅    | ✅      | ❌       |
| レポート       | `/api/reports/*`              | ✅    | ✅      | ❌       |
| ユーザー管理   | `/api/users/profile`          | ✅    | ✅      | ✅       |
| ユーザー管理   | `/api/users/list`             | ✅    | ❌      | ❌       |
| **バッチ処理** | `/api/batch/status`           | ✅    | ❌      | ❌       |
| **バッチ処理** | `/api/batch/daily-summary`    | ✅    | ❌      | ❌       |
| **バッチ処理** | `/api/batch/monthly-summary`  | ✅    | ❌      | ❌       |
| **バッチ処理** | `/api/batch/update-paid-leave`| ✅    | ❌      | ❌       |
| **バッチ処理** | `/api/batch/cleanup-data`     | ✅    | ❌      | ❌       |
| **バッチ処理** | `/api/batch/repair-data`      | ✅    | ❌      | ❌       |
| **バッチ処理** | `/api/batch/overtime-monitoring` | ✅    | ❌      | ❌       |
| **バッチ管理** | `/api/v1/batch/*`             | ✅    | ✅      | ❌       |

---

## 🚨 エラーコード一覧

| コード         | 説明                                 | HTTP ステータス |
| -------------- | ------------------------------------ | --------------- |
| AUTH_001       | 認証トークンが無効です               | 401             |
| AUTH_002       | 認証トークンの有効期限が切れています | 401             |
| AUTH_003       | 権限が不足しています                 | 403             |
| VALIDATION_001 | 必須パラメータが不足しています       | 400             |
| VALIDATION_002 | パラメータの形式が不正です           | 400             |
| DATA_001       | 指定されたリソースが見つかりません   | 404             |
| DATA_002       | データの整合性エラーです             | 400             |
| BATCH_001      | バッチ処理が既に実行中です           | 409             |
| BATCH_002      | バッチパラメータが不正です           | 400             |
| BATCH_003      | バッチ実行権限が不足しています       | 403             |
| BATCH_004      | バッチジョブが見つかりません         | 404             |
| SYSTEM_001     | システムエラーが発生しました         | 500             |
| DATABASE_001   | データベース接続エラーです           | 500             |

---

## 📝 使用例

### バッチ処理の実行例（管理者）

```
# 1. ログイン
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"employeeCode":"admin@company.com","password":"admin123"}'

# 2. バッチステータス確認
curl -X GET http://localhost:8080/api/batch/status \
  -H "Authorization: Bearer YOUR_TOKEN"

# 3. 日次集計バッチ実行
curl -X POST http://localhost:8080/api/batch/daily-summary \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetDate":"2025-02-08"}'

# 4. 月次集計バッチ実行
curl -X POST http://localhost:8080/api/batch/monthly-summary \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetMonth":"2025-01"}'

# 5. データクリーンアップ実行
curl -X POST http://localhost:8080/api/batch/cleanup-data \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"retentionMonths":12}'

# 6. 残業監視バッチ実行
curl -X POST http://localhost:8080/api/batch/overtime-monitoring \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetMonth":"2025-02"}'
```

### レポート出力例

```
# 月次勤怠レポートのダウンロード
curl -X GET "http://localhost:8080/api/reports/attendance/monthly?year=2025&month=1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -O attendance_monthly_202501.csv

# 残業時間レポートのダウンロード
curl -X GET "http://localhost:8080/api/reports/overtime?year=2025&month=1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -O overtime_report_202501.csv
```