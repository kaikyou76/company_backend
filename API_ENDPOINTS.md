# API 接続文档

## 認証方式 {#authentication}

所有 API 均使用 **Bearer Token 認證**。

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

## 通用響應格式 {#response-format}

```json
// 成功響應
{
  "success": true,
  "message": "處理已完成",
  "data": { /* 響應數據 */ }
}

// 錯誤響應
{
  "success": false,
  "message": "錯誤信息",
  "errors": { /* 驗證錯誤詳情 */ }
}
```

## HTTP 狀態碼 {#http-status-codes}

- **200 OK**: 成功
- **201 Created**: 資源創建成功
- **400 Bad Request**: 請求錯誤
- **401 Unauthorized**: 認證錯誤
- **403 Forbidden**: 權限錯誤
- **404 Not Found**: 資源未找到
- **500 Internal Server Error**: 服務器錯誤

---

## 認證相關接口 {#auth-endpoints}

### POST /api/auth/login {#auth-login}

用戶登錄接口

```json
// 請求示例
{
  "employeeCode": "user@example.com",
  "password": "password123"
}

// 成功響應
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400,
  "user": {
    "id": 1,
    "name": "user@example.com",
    "departmentId": 1,
    "departmentName": "開發部",
    "positionId": 1,
    "positionName": "工程師",
    "role": "EMPLOYEE",
    "locationType": "office"
  }
}

// 錯誤響應
{
  "success": false,
  "message": "認證信息不正確"
}
```

### POST /api/auth/refresh {#auth-refresh}

刷新令牌接口

```json
// 請求示例
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}

// 成功響應
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400
}

// 錯誤響應
{
  "success": false,
  "message": "刷新令牌無效"
}
```

### POST /api/auth/logout {#auth-logout}

用戶登出接口

```json
// 請求頭
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...

// 請求體 (空)
{}

// 成功響應
{
  "success": true,
  "message": "已登出"
}

// 錯誤響應
{
  "success": false,
  "message": "無效令牌"
}
```

### POST /api/auth/register {#auth-register}

用戶註冊接口

```json
// 請求示例
{
  "username": "user@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "fullName": "山田太郎",
  "locationType": "office",
  "clientLatitude": "",
  "clientLongitude": "",
  "departmentId": 1,
  "positionId": 3,
  "managerId": 1
}

// 成功響應
{
  "success": true,
  "message": "用戶註冊完成",
  "data": {
    "id": 1,
    "username": "user@example.com",
    "fullName": "山田太郎",
    "locationType": "office",
    "clientLatitude": null,
    "clientLongitude": null,
    "departmentId": 1,
    "positionId": 3,
    "managerId": 1,
    "createdAt": "2025-01-18T09:00:00+09:00"
  }
}

// 錯誤響應
{
  "success": false,
  "message": "用戶註冊過程中發生錯誤"
}
```

### POST /api/auth/admin-register {#auth-admin-register}

管理員註冊接口

```json
// 請求頭
X-Admin-Username: admin@example.com

// 請求示例
{
  "username": "user@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "fullName": "山田太郎",
  "locationType": "office",
  "clientLatitude": "",
  "clientLongitude": "",
  "departmentId": 1,
  "positionId": 3,
  "managerId": 1
}

// 成功響應
{
  "success": true,
  "message": "用戶註冊完成",
  "data": {
    "id": 1,
    "username": "user@example.com",
    "fullName": "山田太郎",
    "locationType": "office",
    "clientLatitude": null,
    "clientLongitude": null,
    "departmentId": 1,
    "positionId": 3,
    "managerId": 1,
    "positionName": "工程師",
    "departmentName": "開發部",
    "createdBy": "admin@example.com",
    "createdAt": "2025-01-18T09:00:00+09:00"
  }
}

// 錯誤響應
{
  "success": false,
  "message": "用戶註冊過程中發生錯誤"
}
```

### GET /api/auth/check-username {#auth-check-username}

檢查用戶名是否可用

```json
// 請求參數
// ?username=user@example.com

// 成功響應
{
  "success": true,
  "available": true,
  "message": "用戶名可用"
}

// 錯誤響應
{
  "success": false,
  "available": false,
  "message": "用戶名已被使用"
}
```

### GET /api/auth/admin-positions {#auth-admin-positions}

獲取管理員職位列表

```json
// 成功響應
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "管理員",
      "level": 10
    },
    {
      "id": 2,
      "name": "經理",
      "level": 7
    }
  ]
}

// 錯誤響應
{
  "success": false,
  "message": "獲取管理員職位列表時發生錯誤"
}
```

### POST /api/auth/csvregister {#auth-csv-register}

通過 CSV 文件批量註冊用戶

```json
// 請求 (multipart/form-data)
// file: users.csv

// 成功響應
{
  "success": true,
  "message": "CSV批量註冊完成",
  "data": {
    "successCount": 10,
    "errorCount": 2
  }
}

// 錯誤響應
{
  "success": false,
  "message": "CSV批量註冊處理時發生錯誤"
}
```

### GET /api/auth/csv-template {#auth-csv-template}

獲取 CSV 模板

```json
// 成功響應
{
  "success": true,
  "data": {
    "headers": [
      "username",
      "password",
      "fullname",
      "location_type",
      "client_latitude",
      "client_longitude",
      "department_id",
      "position_id",
      "manager_id"
    ],
    "sampleData": [
      "user@example.com",
      "password123",
      "山田太郎",
      "office",
      "",
      "",
      "1",
      "3",
      "1"
    ]
  }
}
```

## 考勤相關接口 {#attendance-endpoints}

### POST /api/attendance/clock-in {#attendance-clock-in}

上班打卡接口

```json
// 請求示例
{
  "latitude": 35.6895,
  "longitude": 139.6917
}

// 成功響應
{
  "success": true,
  "message": "上班打卡完成",
  "data": {
    "recordId": 12345,
    "timestamp": "2025-01-18T09:00:00+09:00",
    "locationVerified": true
  }
}
```

### POST /api/attendance/clock-out {#attendance-clock-out}

下班打卡接口

```json
// 請求示例
{
  "latitude": 35.6895,
  "longitude": 139.6917
}

// 成功響應
{
  "success": true,
  "message": "下班打卡完成",
  "data": {
    "recordId": 12345,
    "timestamp": "2025-01-18T18:00:00+09:00",
    "workingHours": 8.5,
    "overtimeHours": 0.5
  }
}

// 錯誤響應
{
  "success": false,
  "message": "尚未上班打卡"
}
```

### GET /api/attendance/records {#attendance-records}

獲取考勤記錄

```json
// 請求參數
// ?startDate=2025-01-01&endDate=2025-01-31&page=0&size=10

// 成功響應
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

### GET /api/attendance/daily-summary {#attendance-daily-summary}

獲取每日考勤彙總

```json
// 請求參數
// ?date=2025-01-18

// 成功響應
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

## 休假相關接口 {#leave-endpoints}

### POST /api/leave/request {#leave-request}

提交休假申請

```json
// 請求示例
{
  "leaveType": "PAID_LEAVE",
  "startDate": "2025-02-01",
  "endDate": "2025-02-03",
  "reason": "家庭旅行",
  "substituteUserId": 2,
  "emergencyContact": "090-1234-5678"
}

// 成功響應
{
  "success": true,
  "message": "休假申請已創建",
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

### GET /api/leave/my-requests {#leave-my-requests}

獲取我的休假申請列表

```json
// 請求參數
// ?startDate=2025-01-01&endDate=2025-12-31&leaveType=PAID_LEAVE

// 成功響應
{
  "success": true,
  "data": {
    "requests": [
      {
        "id": 1001,
        "leaveType": "PAID_LEAVE",
        "leaveTypeDisplayName": "年假",
        "startDate": "2025-02-01",
        "endDate": "2025-02-03",
        "days": 3,
        "approvalStatus": "PENDING",
        "approvalStatusDisplayName": "待審批"
      }
    ],
    "totalCount": 5,
    "remainingPaidLeaveDays": 17
  }
}
```

### POST /api/leave/{id}/approve {#leave-approve}

審批休假申請

```json
// 請求示例
{
  "comment": "批准。祝您假期愉快。"
}

// 成功響應
{
  "success": true,
  "message": "休假申請已批准",
  "data": {
    "id": 1001,
    "approvalStatus": "APPROVED",
    "approvedBy": 1,
    "approvedAt": "2025-01-18T10:30:00+09:00",
    "approvalComment": "批准。祝您假期愉快。"
  }
}
```

### GET /api/leave/balance {#leave-balance}

獲取剩餘休假天數

```json
// 成功響應
{
  "success": true,
  "data": {
    "remainingPaidLeaveDays": 15
  }
}

// 錯誤響應
{
  "success": false,
  "message": "發生系統錯誤"
}
```

### GET /api/leave/calendar {#leave-calendar}

獲取休假日曆

```json
// 請求參數
// ?startDate=2025-01-01&endDate=2025-12-31

// 成功響應
{
  "success": true,
  "data": {
    "events": [
      {
        "id": 1001,
        "title": "年假",
        "startDate": "2025-02-01",
        "endDate": "2025-02-03",
        "type": "paid",
        "status": "APPROVED"
      }
    ],
    "startDate": "2025-01-01",
    "endDate": "2025-12-31"
  }
}

// 錯誤響應
{
  "success": false,
  "message": "發生系統錯誤"
}
```

## 報告相關接口 {#reports-endpoints}

### GET /api/reports/attendance/daily {#reports-attendance-daily}

獲取每日考勤報告

```json
// 請求參數
// ?startDate=2025-01-01&endDate=2025-01-31&page=0&size=20

// 成功響應
{
  "success": true,
  "data": {
    "summaries": [
      {
        "id": 1,
        "date": "2025-01-18",
        "userId": 1,
        "userName": "田中太郎",
        "clockInTime": "09:00:00",
        "clockOutTime": "18:00:00",
        "workingHours": 8.0,
        "overtimeHours": 1.0,
        "breakTime": 1.0,
        "status": "completed"
      }
    ],
    "totalCount": 20,
    "currentPage": 0,
    "totalPages": 2
  }
}
```

### GET /api/reports/attendance/monthly {#reports-attendance-monthly}

獲取月度考勤報告

```json
// 請求參數
// ?yearMonth=2025-01&page=0&size=20

// 成功響應
{
  "success": true,
  "data": {
    "summaries": [
      {
        "id": 1,
        "date": "2025-01-18",
        "userId": 1,
        "userName": "田中太郎",
        "clockInTime": "09:00:00",
        "clockOutTime": "18:00:00",
        "workingHours": 8.0,
        "overtimeHours": 1.0,
        "breakTime": 1.0,
        "status": "completed"
      }
    ],
    "totalCount": 20,
    "currentPage": 0,
    "totalPages": 2,
    "targetMonth": "2025-01"
  }
}
```

### GET /api/reports/attendance/overtime {#reports-attendance-overtime}

獲取加班時間統計

```json
// 請求參數
// ?startDate=2025-01-01&endDate=2025-01-31

// 成功響應
{
  "success": true,
  "message": "加班時間統計獲取完成",
  "data": {
    "statistics": {
      "totalEmployees": 50,
      "totalOvertimeHours": 120.5,
      "averageOvertimeHours": 2.41,
      "maxOvertimeHours": 20.0,
      "overtimeEmployeesCount": 35
    },
    "startDate": "2025-01-01",
    "endDate": "2025-01-31"
  }
}
```

### GET /api/reports/attendance/statistics {#reports-attendance-statistics}

獲取考勤統計報告

```json
// 請求參數
// ?startDate=2025-01-01&endDate=2025-01-31

// 成功響應
{
  "success": true,
  "message": "考勤統計報告獲取完成",
  "data": {
    "statistics": {
      "totalEmployees": 50,
      "totalWorkingDays": 1000,
      "totalWorkingHours": 8000.0,
      "totalOvertimeHours": 120.5,
      "averageWorkingHours": 8.0,
      "attendanceRate": 98.5
    },
    "startDate": "2025-01-01",
    "endDate": "2025-01-31"
  }
}
```

### GET /api/reports/attendance/export {#reports-attendance-export}

導出考勤摘要

```json
// 請求參數
// ?startDate=2025-01-01&endDate=2025-01-31&format=csv

// 響應 (CSV 文件下載)
Content-Type: text/csv; charset=UTF-8
Content-Disposition: attachment; filename="attendance_summaries.csv"

// CSV 內容:
"日期","員工ID","員工姓名","上班時間","下班時間","實際工作時間","加班時間","休息時間","狀態"
"2025-01-18",1,"田中太郎","09:00:00","18:00:00",8.0,1.0,1.0,"completed"
```

### GET /api/reports/attendance/personal {#reports-attendance-personal}

獲取個人考勤統計

```json
// 請求參數
// ?userId=1&startDate=2025-01-01&endDate=2025-01-31

// 成功響應
{
  "success": true,
  "message": "個人考勤統計獲取完成",
  "data": {
    "statistics": {
      "userId": 1,
      "userName": "田中太郎",
      "totalWorkingDays": 20,
      "totalWorkingHours": 160.0,
      "totalOvertimeHours": 8.5,
      "averageWorkingHours": 8.0,
      "lateArrivals": 0,
      "earlyLeaves": 0
    }
  }
}
```

### GET /api/reports/attendance/department {#reports-attendance-department}

獲取部門考勤統計

```json
// 請求參數
// ?departmentId=1&startDate=2025-01-01&endDate=2025-01-31

// 成功響應
{
  "success": true,
  "message": "部門考勤統計獲取完成",
  "data": {
    "statistics": {
      "departmentId": 1,
      "departmentName": "開發部",
      "totalEmployees": 10,
      "totalWorkingDays": 200,
      "totalWorkingHours": 1600.0,
      "totalOvertimeHours": 85.0,
      "averageWorkingHours": 8.0,
      "attendanceRate": 99.0
    }
  }
}
```

### POST /api/reports/attendance/daily/calculate {#reports-attendance-daily-calculate}

計算每日摘要

```json
// 請求參數
// ?targetDate=2025-01-18

// 成功響應
{
  "success": true,
  "message": "每日摘要計算完成",
  "data": {
    "summary": {
      "id": 1,
      "date": "2025-01-18",
      "userId": 1,
      "userName": "田中太郎",
      "clockInTime": "09:00:00",
      "clockOutTime": "18:00:00",
      "workingHours": 8.0,
      "overtimeHours": 1.0,
      "breakTime": 1.0,
      "status": "completed"
    },
    "targetDate": "2025-01-18"
  }
}
```

### POST /api/reports/attendance/monthly/calculate {#reports-attendance-monthly-calculate}

計算月度摘要

```json
// 請求參數
// ?yearMonth=2025-01

// 成功響應
{
  "success": true,
  "message": "月度摘要計算完成",
  "data": {
    "statistics": {
      "totalWorkingDays": 20,
      "totalWorkingHours": 160.0,
      "totalOvertimeHours": 8.5,
      "averageWorkingHours": 8.0
    },
    "targetMonth": "2025-01"
  }
}
```

## 用戶相關接口 {#users-endpoints}

### GET /api/users/profile {#users-profile}

獲取用戶個人信息

```json
// 成功響應
{
  "success": true,
  "data": {
    "id": 1,
    "employeeCode": "E12345",
    "name": "田中太郎",
    "email": "tanaka@company.com",
    "role": "EMPLOYEE",
    "department": "開發部",
    "position": "工程師",
    "hireDate": "2023-04-01",
    "phoneNumber": "090-1234-5678",
    "remainingPaidLeave": 20
  }
}
```

### PUT /api/users/profile {#users-profile-update}

更新用戶個人信息

```json
// 請求示例
{
  "name": "田中太郎",
  "email": "tanaka@company.com",
  "phoneNumber": "090-1234-5678",
  "emergencyContact": "090-9876-5432"
}

// 成功響應
{
  "success": true,
  "message": "個人信息已更新",
  "data": {
    "id": 1,
    "name": "田中太郎",
    "email": "tanaka@company.com",
    "phoneNumber": "090-1234-5678",
    "updatedAt": "2025-01-18T10:30:00+09:00"
  }
}
```

### GET /api/users/list {#users-list}

獲取用戶列表（僅管理員）

```json
// 請求參數
// ?page=0&size=10&department=開發部&active=true

// 成功響應
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
        "department": "開發部",
        "position": "工程師",
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

## 部門管理接口 {#departments-endpoints}

### GET /api/departments {#departments-list}

獲取所有部門列表

```json
// 成功響應
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "開發部",
      "code": "DEV001",
      "managerId": 101,
      "createdAt": "2023-01-01T00:00:00+09:00",
      "updatedAt": "2023-01-01T00:00:00+09:00"
    },
    {
      "id": 2,
      "name": "人事部",
      "code": "HR001",
      "managerId": 102,
      "createdAt": "2023-01-01T00:00:00+09:00",
      "updatedAt": "2023-01-01T00:00:00+09:00"
    }
  ]
}

// 錯誤響應
{
  "success": false,
  "message": "獲取部門列表失敗"
}
```

### GET /api/departments/{id} {#departments-get}

根據 ID 獲取特定部門

```json
// 成功響應
{
  "success": true,
  "data": {
    "id": 1,
    "name": "開發部",
    "code": "DEV001",
    "managerId": 101,
    "createdAt": "2023-01-01T00:00:00+09:00",
    "updatedAt": "2023-01-01T00:00:00+09:00"
  }
}

// 錯誤響應
{
  "success": false,
  "message": "獲取部門信息失敗"
}
```

### POST /api/departments {#departments-create}

創建新部門

```json
// 請求示例
{
  "name": "財務部",
  "code": "FIN001",
  "managerId": 103
}

// 成功響應
{
  "success": true,
  "message": "部門創建成功",
  "data": {
    "id": 3,
    "name": "財務部",
    "code": "FIN001",
    "managerId": 103,
    "createdAt": "2025-01-01T00:00:00+09:00",
    "updatedAt": "2025-01-01T00:00:00+09:00"
  }
}

// 錯誤響應 (字段驗證失敗)
{
  "success": false,
  "message": "部門名稱不能為空"
}

// 錯誤響應 (名稱重複)
{
  "success": false,
  "message": "部門名稱已存在"
}

// 錯誤響應 (代碼重複)
{
  "success": false,
  "message": "部門代碼已存在"
}

// 錯誤響應 (系統錯誤)
{
  "success": false,
  "message": "創建部門失敗"
}
```

### PUT /api/departments/{id} {#departments-update}

更新部門信息

```json
// 請求示例
{
  "name": "財務部",
  "code": "FIN001",
  "managerId": 104
}

// 成功響應
{
  "success": true,
  "message": "部門信息更新成功",
  "data": {
    "id": 3,
    "name": "財務部",
    "code": "FIN001",
    "managerId": 104,
    "createdAt": "2025-01-01T00:00:00+09:00",
    "updatedAt": "2025-02-01T00:00:00+09:00"
  }
}

// 錯誤響應 (部門不存在)
{
  "success": false,
  "message": "部門不存在"
}

// 錯誤響應 (名稱重複)
{
  "success": false,
  "message": "部門名稱已存在"
}

// 錯誤響應 (代碼重複)
{
  "success": false,
  "message": "部門代碼已存在"
}

// 錯誤響應 (系統錯誤)
{
  "success": false,
  "message": "更新部門信息失敗"
}
```

### DELETE /api/departments/{id} {#departments-delete}

刪除部門

```json
// 成功響應
{
  "success": true,
  "message": "部門刪除成功"
}

// 錯誤響應 (部門不存在)
{
  "success": false,
  "message": "部門不存在"
}

// 錯誤響應 (系統錯誤)
{
  "success": false,
  "message": "刪除部門失敗"
}
```

## 職位管理接口 {#positions-endpoints}

### GET /api/positions {#positions-list}

獲取所有職位列表

```json
// 成功響應
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "軟體工程師",
      "level": 1,
      "createdAt": "2023-01-01T00:00:00+09:00",
      "updatedAt": "2023-01-01T00:00:00+09:00"
    },
    {
      "id": 2,
      "name": "項目經理",
      "level": 2,
      "createdAt": "2023-01-01T00:00:00+09:00",
      "updatedAt": "2023-01-01T00:00:00+09:00"
    }
  ]
}

// 錯誤響應
{
  "success": false,
  "message": "獲取職位列表失敗"
}
```

### GET /api/positions/{id} {#positions-get}

根據 ID 獲取特定職位

```json
// 成功響應
{
  "success": true,
  "data": {
    "id": 1,
    "name": "軟體工程師",
    "level": 1,
    "createdAt": "2023-01-01T00:00:00+09:00",
    "updatedAt": "2023-01-01T00:00:00+09:00"
  }
}

// 錯誤響應
{
  "success": false,
  "message": "獲取職位信息失敗"
}
```

### POST /api/positions {#positions-create}

創建新職位

```json
// 請求示例
{
  "name": "高級軟體工程師",
  "level": 3
}

// 成功響應
{
  "success": true,
  "message": "職位創建成功",
  "data": {
    "id": 3,
    "name": "高級軟體工程師",
    "level": 3,
    "createdAt": "2025-01-01T00:00:00+09:00",
    "updatedAt": "2025-01-01T00:00:00+09:00"
  }
}

// 錯誤響應 (字段驗證失敗)
{
  "success": false,
  "message": "職位名稱不能為空"
}

// 錯誤響應 (名稱重複)
{
  "success": false,
  "message": "職位名稱已存在"
}

// 錯誤響應 (系統錯誤)
{
  "success": false,
  "message": "創建職位失敗"
}
```

### PUT /api/positions/{id} {#positions-update}

更新職位信息

```json
// 請求示例
{
  "name": "高級軟體工程師",
  "level": 4
}

// 成功響應
{
  "success": true,
  "message": "職位信息更新成功",
  "data": {
    "id": 3,
    "name": "高級軟體工程師",
    "level": 4,
    "createdAt": "2025-01-01T00:00:00+09:00",
    "updatedAt": "2025-02-01T00:00:00+09:00"
  }
}

// 錯誤響應 (職位不存在)
{
  "success": false,
  "message": "職位不存在"
}

// 錯誤響應 (名稱重複)
{
  "success": false,
  "message": "職位名稱已存在"
}

// 錯誤響應 (系統錯誤)
{
  "success": false,
  "message": "更新職位信息失敗"
}
```

### DELETE /api/positions/{id} {#positions-delete}

刪除職位

```json
// 成功響應
{
  "success": true,
  "message": "職位刪除成功"
}

// 錯誤響應 (職位不存在)
{
  "success": false,
  "message": "職位不存在"
}

// 錯誤響應 (系統錯誤)
{
  "success": false,
  "message": "刪除職位失敗"
}
```

## 批處理相關接口 {#batch-endpoints}

### POST /api/batch/daily-summary {#batch-daily-summary}

執行每日考勤彙總批處理

```json
// 請求示例
{
  "targetDate": "2025-02-08"  // 可選（默認為當天）
}

// 成功響應
{
  "success": true,
  "message": "每日考勤彙總批處理已執行",
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

// 錯誤響應
{
  "success": false,
  "message": "每日考勤彙總批處理執行失敗: 參數不正確",
  "executedAt": "2025-02-08T10:30:00+09:00"
}
```

### POST /api/batch/monthly-summary {#batch-monthly-summary}

執行月度考勤彙總批處理

```json
// 請求示例
{
  "targetMonth": "2025-01"  // 可選（默認為上個月）
}

// 成功響應
{
  "success": true,
  "message": "月度考勤彙總批處理已執行",
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

// 錯誤響應
{
  "success": false,
  "message": "月度考勤彙總批處理執行失敗: 參數不正確",
  "executedAt": "2025-02-08T10:30:00+09:00"
}
```

### POST /api/batch/update-paid-leave {#batch-update-paid-leave}

執行年假天數更新批處理

```json
// 請求示例
{
  "fiscalYear": 2025  // 可選（默認為當前財年）
}

// 成功響應
{
  "success": true,
  "message": "年假天數更新批處理已執行",
  "data": {
    "fiscalYear": 2025,
    "updatedCount": 48,
    "totalUserCount": 50,
    "successRate": 96.0,
    "errorCount": 2,
    "errorMessages": [
      "用戶ID: 15, 錯誤: 入職日期未設置"
    ]
  },
  "executedAt": "2025-02-08T10:30:00+09:00"
}

// 錯誤響應
{
  "success": false,
  "message": "年假天數更新批處理執行失敗: 數據庫連接錯誤",
  "executedAt": "2025-02-08T10:30:00+09:00"
}
```

### POST /api/batch/cleanup-data {#batch-cleanup-data}

執行數據清理批處理

```json
// 請求示例
{
  "retentionMonths": 12  // 可選（默認: 12個月）
}

// 成功響應
{
  "success": true,
  "message": "數據清理批處理已執行",
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

// 錯誤響應
{
  "success": false,
  "message": "數據清理批處理執行失敗: 權限不足",
  "executedAt": "2025-02-08T10:30:00+09:00"
}
```

### POST /api/batch/repair-data {#batch-repair-data}

執行數據修復批處理

```json
// 請求示例
{} // 無參數

// 成功響應
{
  "success": true,
  "message": "數據修復批處理已執行",
  "data": {
    "repairedItems": []
  },
  "executedAt": "2025-02-08T10:30:00+09:00"
}

// 錯誤響應
{
  "success": false,
  "message": "數據修復批處理執行失敗: 系統錯誤",
  "executedAt": "2025-02-08T10:30:00+09:00"
}
```

### POST /api/batch/overtime-monitoring {#batch-overtime-monitoring}

執行加班監控批處理

```json
// 請求示例
{
  "targetMonth": "2025-02"  // 可選（默認為當月）
}

// 成功響應
{
  "success": true,
  "message": "加班監控批處理已執行",
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

// 錯誤響應
{
  "success": false,
  "message": "加班監控批處理執行失敗: 系統錯誤",
  "executedAt": "2025-02-08T10:30:00+09:00"
}
```

### GET /api/batch/status {#batch-status}

獲取批處理狀態

```json
// 成功響應
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

// 錯誤響應
{
  "success": false,
  "message": "獲取批處理狀態失敗"
}
```

## 批處理管理接口 {#batch-management-endpoints}

### GET /api/v1/batch/instances {#batch-instances}

獲取所有作業實例

```json
// 成功響應
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

// 錯誤響應
{
  "success": false,
  "message": "獲取作業實例失敗: 數據庫連接錯誤"
}
```

### GET /api/v1/batch/executions/{jobName} {#batch-executions}

獲取作業執行歷史

```json
// 請求參數
// - jobName (路徑參數)
// - page (可選查詢參數, 默認: 0)
// - size (可選查詢參數, 默認: 20)

// 成功響應
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

// 錯誤響應
{
  "success": false,
  "message": "獲取作業執行歷史失敗: 作業未找到"
}
```

### GET /api/v1/batch/steps/{jobExecutionId} {#batch-steps}

獲取步驟執行歷史

```json
// 請求參數
// - jobExecutionId (路徑參數)

// 成功響應
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

// 錯誤響應
{
  "success": false,
  "message": "獲取步驟執行歷史失敗: 執行未找到"
}
```

### GET /api/v1/batch/statistics {#batch-statistics}

獲取批處理執行統計

```json
// 成功響應
{
  "success": true,
  "statistics": {
    "totalJobs": 5,
    "successRate": 100.0,
    "errorRate": 0.0
  }
}

// 錯誤響應
{
  "success": false,
  "message": "獲取批處理統計失敗: 服務不可用"
}
```

### GET /api/v1/batch/running {#batch-running}

獲取正在運行的作業

```json
// 成功響應
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

// 錯誤響應
{
  "success": false,
  "message": "獲取正在運行的作業失敗: 服務錯誤"
}
```

### GET /api/v1/batch/job-names {#batch-job-names}

獲取作業名稱列表

```json
// 成功響應
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

// 錯誤響應
{
  "success": false,
  "message": "獲取作業名稱列表失敗: 數據庫錯誤"
}
```

### GET /api/v1/batch/latest/{jobName} {#batch-latest}

獲取特定作業的最新執行信息

```json
// 請求參數
// - jobName (路徑參數)

// 成功響應
{
  "success": true,
  "jobName": "monthlyAttendanceSummaryJob",
  "latestExecution": {
    "jobExecutionId": 1,
    "jobName": "monthlyAttendanceSummaryJob",
    "status": "COMPLETED"
  }
}

// 錯誤響應
{
  "success": false,
  "message": "獲取最新作業執行信息失敗: 作業未找到"
}
```

## 系統日誌管理接口 {#system-logs-endpoints}

### GET /api/system-logs {#system-logs-list}

獲取系統日誌列表

```json
// 請求參數
// ?page=0&size=20&action=LOGIN&status=success&startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z

// 成功響應
{
  "success": true,
  "message": "系統日誌列表獲取完成",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "action": "LOGIN",
      "status": "success",
      "ipAddress": "192.168.1.1",
      "userAgent": "Mozilla/5.0",
      "details": "Login successful",
      "createdAt": "2025-01-18T10:30:00+09:00"
    }
  ],
  "currentPage": 0,
  "totalItems": 100,
  "totalPages": 5
}

// 錯誤響應
{
  "success": false,
  "message": "獲取系統日誌列表失敗"
}
```

### GET /api/system-logs/{id} {#system-logs-get}

獲取系統日誌詳情

```json
// 成功響應
{
  "success": true,
  "message": "系統日誌詳情獲取完成",
  "data": {
    "id": 1,
    "userId": 1,
    "action": "LOGIN",
    "status": "success",
    "ipAddress": "192.168.1.1",
    "userAgent": "Mozilla/5.0",
    "details": "Login successful",
    "createdAt": "2025-01-18T10:30:00+09:00"
  }
}

// 錯誤響應 (日誌未找到)
{
  "success": false,
  "message": "指定的系統日誌未找到"
}

// 錯誤響應 (系統錯誤)
{
  "success": false,
  "message": "獲取系統日誌詳情失敗"
}
```

### DELETE /api/system-logs/{id} {#system-logs-delete}

刪除系統日誌

```json
// 成功響應
{
  "success": true,
  "message": "系統日誌已刪除"
}

// 錯誤響應 (日誌未找到)
{
  "success": false,
  "message": "指定的系統日誌未找到"
}

// 錯誤響應 (系統錯誤)
{
  "success": false,
  "message": "刪除系統日誌失敗"
}
```

### GET /api/system-logs/export/csv {#system-logs-export-csv}

導出系統日誌為 CSV

```json
// 請求參數
// ?action=LOGIN&status=success&startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z

// 響應 (CSV格式)
Content-Type: text/csv; charset=UTF-8
Content-Disposition: attachment; filename="system_logs.csv"

ID,用戶ID,操作,狀態,IP地址,用戶代理,詳情,創建時間
1,1,LOGIN,success,192.168.1.1,Mozilla/5.0,Login successful,2025-01-18T10:30:00+09:00

// 錯誤響應
系統日誌導出失敗
```

### GET /api/system-logs/export/json {#system-logs-export-json}

導出系統日誌為 JSON

```json
// 請求參數
// ?action=LOGIN&status=success&startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z

// 成功響應
{
  "success": true,
  "message": "系統日誌導出完成",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "action": "LOGIN",
      "status": "success",
      "ipAddress": "192.168.1.1",
      "userAgent": "Mozilla/5.0",
      "details": "Login successful",
      "createdAt": "2025-01-18T10:30:00+09:00"
    }
  ],
  "exportedAt": "2025-01-18T10:30:00+09:00",
  "count": 1
}

// 錯誤響應
{
  "success": false,
  "message": "系統日誌導出失敗"
}
```

### GET /api/system-logs/statistics {#system-logs-statistics}

獲取系統日誌統計信息

```json
// 請求參數
// ?startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z

// 成功響應
{
  "success": true,
  "message": "系統日誌統計信息獲取完成",
  "data": {
    "actionStats": [
      {
        "action": "LOGIN",
        "count": 50
      }
    ],
    "statusStats": [
      {
        "status": "success",
        "count": 45
      }
    ],
    "userStats": [
      {
        "userId": 1,
        "count": 30
      }
    ],
    "dateStats": [
      {
        "date": "2025-01-18",
        "count": 10
      }
    ]
  },
  "period": {
    "startDate": "2024-12-19T10:30:00+09:00",
    "endDate": "2025-01-18T10:30:00+09:00"
  }
}

// 錯誤響應
{
  "success": false,
  "message": "獲取系統日誌統計信息失敗"
}
```

### GET /api/system-logs/search {#system-logs-search}

搜索系統日誌

```json
// 請求參數
// ?keyword=LOGIN&page=0&size=20

// 成功響應
{
  "success": true,
  "message": "系統日誌搜索完成",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "action": "LOGIN",
      "status": "success",
      "ipAddress": "192.168.1.1",
      "userAgent": "Mozilla/5.0",
      "details": "Login successful",
      "createdAt": "2025-01-18T10:30:00+09:00"
    }
  ],
  "currentPage": 0,
  "totalItems": 25,
  "totalPages": 2,
  "keyword": "LOGIN"
}

// 錯誤響應
{
  "success": false,
  "message": "系統日誌搜索失敗"
}
```

## API 權限矩陣 {#permissions-matrix}

| API 類別     | 端點                                    | 管理員 | 經理 | 普通員工 |
| ------------ | --------------------------------------- | ------ | ---- | -------- |
| 認證         | `/api/auth/*`                           | ✅     | ✅   | ✅       |
| 考勤管理     | `/api/attendance/*`                     | ✅     | ✅   | ✅       |
| 休假管理     | `/api/leave/request`                    | ✅     | ✅   | ✅       |
| 休假管理     | `/api/leave/my-requests`                | ✅     | ✅   | ✅       |
| 休假管理     | `/api/leave/balance`                    | ✅     | ✅   | ✅       |
| 休假管理     | `/api/leave/calendar`                   | ✅     | ✅   | ✅       |
| 休假管理     | `/api/leave/*/approve`                  | ✅     | ✅   | ❌       |
| 報告         | `/api/reports/*`                        | ✅     | ✅   | ❌       |
| 用戶管理     | `/api/users/profile`                    | ✅     | ✅   | ✅       |
| 用戶管理     | `/api/users/list`                       | ✅     | ❌   | ❌       |
| 用戶管理     | `/api/users/{id}`                       | ✅     | ❌   | ❌       |
| 用戶管理     | `/api/users`                            | ✅     | ❌   | ❌       |
| 用戶管理     | `/api/users/*/change-password`          | ✅     | ✅   | ✅       |
| 部門管理     | `/api/departments/*`                    | ✅     | ❌   | ❌       |
| 職位管理     | `/api/positions/*`                      | ✅     | ❌   | ❌       |
| 系統日誌管理 | `/api/system-logs/*`                    | ✅     | ❌   | ❌       |
| 打卡修正申請 | `/api/v1/time-corrections/*`            | ✅     | ✅   | ✅       |
| 打卡修正申請 | `/api/v1/time-corrections/pending`      | ✅     | ✅   | ❌       |
| 打卡修正申請 | `/api/v1/time-corrections/{id}/approve` | ✅     | ✅   | ❌       |
| 打卡修正申請 | `/api/v1/time-corrections/{id}/reject`  | ✅     | ✅   | ❌       |
| 批處理       | `/api/batch/status`                     | ✅     | ❌   | ❌       |
| 批處理       | `/api/batch/daily-summary`              | ✅     | ❌   | ❌       |
| 批處理       | `/api/batch/monthly-summary`            | ✅     | ❌   | ❌       |
| 批處理       | `/api/batch/update-paid-leave`          | ✅     | ❌   | ❌       |
| 批處理       | `/api/batch/cleanup-data`               | ✅     | ❌   | ❌       |
| 批處理       | `/api/batch/repair-data`                | ✅     | ❌   | ❌       |
| 批處理       | `/api/batch/overtime-monitoring`        | ✅     | ❌   | ❌       |
| 批處理管理   | `/api/v1/batch/*`                       | ✅     | ✅   | ❌       |

## 錯誤代碼列表 {#error-codes}

| 代碼           | 說明               | HTTP 狀態 |
| -------------- | ------------------ | --------- |
| AUTH_001       | 認證令牌無效       | 401       |
| AUTH_002       | 認證令牌已過期     | 401       |
| AUTH_003       | 權限不足           | 403       |
| VALIDATION_001 | 缺少必需參數       | 400       |
| VALIDATION_002 | 參數格式不正確     | 400       |
| DATA_001       | 未找到指定資源     | 404       |
| DATA_002       | 數據一致性錯誤     | 400       |
| BATCH_001      | 批處理已在運行     | 409       |
| BATCH_002      | 批處理參數不正確   | 400       |
| BATCH_003      | 批處理執行權限不足 | 403       |
| BATCH_004      | 未找到批處理作業   | 404       |
| SYSTEM_001     | 發生系統錯誤       | 500       |
| DATABASE_001   | 數據庫連接錯誤     | 500       |

## 使用示例 {#usage-examples}

### 批處理執行示例（管理員） {#batch-execution-example}

```bash
# 1. 登錄
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"employeeCode":"admin@company.com","password":"admin123"}'

# 2. 檢查批處理狀態
curl -X GET http://localhost:8080/api/batch/status \
  -H "Authorization: Bearer YOUR_TOKEN"

# 3. 執行每日彙總批處理
curl -X POST http://localhost:8080/api/batch/daily-summary \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetDate":"2025-02-08"}'

# 4. 執行月度彙總批處理
curl -X POST http://localhost:8080/api/batch/monthly-summary \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetMonth":"2025-01"}'

# 5. 執行數據清理
curl -X POST http://localhost:8080/api/batch/cleanup-data \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"retentionMonths":12}'

# 6. 執行加班監控批處理
curl -X POST http://localhost:8080/api/batch/overtime-monitoring \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetMonth":"2025-02"}'
```

### 報告導出示例 {#report-export-example}

```bash
# 下載月度考勤報告
curl -X GET "http://localhost:8080/api/reports/attendance/monthly?year=2025&month=1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -O attendance_monthly_202501.csv

# 下載加班時間報告
curl -X GET "http://localhost:8080/api/reports/overtime?year=2025&month=1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -O overtime_report_202501.csv
```

### 用戶操作示例 {#user-operation-example}

#### 員工打卡流程 {#employee-clock-flow}

```bash
# 1. 登錄
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"employeeCode":"employee@company.com","password":"password123"}'

# 2. 上班打卡
curl -X POST http://localhost:8080/api/attendance/clock-in \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"latitude":35.6895,"longitude":139.6917}'

# 3. 下班打卡
curl -X POST http://localhost:8080/api/attendance/clock-out \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"latitude":35.6895,"longitude":139.6917}'

# 4. 查看考勤記錄
curl -X GET "http://localhost:8080/api/attendance/records?startDate=2025-01-01&endDate=2025-01-31" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 休假申請流程 {#leave-request-flow}

```bash
# 1. 查看剩餘年假天數
curl -X GET http://localhost:8080/api/leave/balance \
  -H "Authorization: Bearer YOUR_TOKEN"

# 2. 提交休假申請
curl -X POST http://localhost:8080/api/leave/request \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "leaveType":"PAID_LEAVE",
    "startDate":"2025-02-01",
    "endDate":"2025-02-03",
    "reason":"家庭旅行",
    "substituteUserId":2,
    "emergencyContact":"090-1234-5678"
  }'

# 3. 查看申請狀態
curl -X GET http://localhost:8080/api/leave/my-requests \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 管理員操作示例 {#admin-operation-example}

#### 用戶管理流程 {#user-management-flow}

```bash
# 1. 獲取用戶列表
curl -X GET "http://localhost:8080/api/users/list?page=0&size=10" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 2. 創建新部門
curl -X POST http://localhost:8080/api/departments \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"財務部",
    "code":"FIN001",
    "managerId":103
  }'

# 3. 創建新職位
curl -X POST http://localhost:8080/api/positions \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"高級軟體工程師",
    "level":3
  }'

# 4. 管理員註冊新用戶
curl -X POST http://localhost:8080/api/auth/admin-register \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Admin-Username: admin@company.com" \
  -H "Content-Type: application/json" \
  -d '{
    "username":"newuser@company.com",
    "password":"password123",
    "confirmPassword":"password123",
    "fullName":"新用戶",
    "locationType":"office",
    "departmentId":1,
    "positionId":3,
    "managerId":1
  }'
```

#### 系統監控流程 {#system-monitoring-flow}

```bash
# 1. 查看系統日誌
curl -X GET "http://localhost:8080/api/system-logs?page=0&size=20&action=LOGIN" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 2. 獲取系統日誌統計
curl -X GET "http://localhost:8080/api/system-logs/statistics?startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 3. 導出系統日誌
curl -X GET "http://localhost:8080/api/system-logs/export/csv?startDate=2025-01-01T00:00:00Z&endDate=2025-12-31T23:59:59Z" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -O system_logs.csv

# 4. 獲取批處理執行統計
curl -X GET http://localhost:8080/api/v1/batch/statistics \
  -H "Authorization: Bearer YOUR_TOKEN"

# 5. 查看正在運行的批處理作業
curl -X GET http://localhost:8080/api/v1/batch/running \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 技術注意事項 {#technical-notes}

### 時間格式 {#time-format}

所有 API 中的時間格式均採用 ISO 8601 標準，並包含時區信息：

```
2025-01-18T10:30:00+09:00
```

### 分頁參數 {#pagination-parameters}

支持分頁的 API 使用以下標準參數：

- `page`: 頁碼（從 0 開始）
- `size`: 每頁記錄數（默認：20，最大：100）

### 日期範圍查詢 {#date-range-query}

日期範圍查詢使用以下參數：

- `startDate`: 開始日期（包含）
- `endDate`: 結束日期（包含）

### 文件上傳 {#file-upload}

CSV 批量註冊等文件上傳接口使用 `multipart/form-data` 格式：

```bash
curl -X POST http://localhost:8080/api/auth/csvregister \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@users.csv"
```

### 地理位置參數 {#geolocation-parameters}

打卡相關 API 使用以下地理位置參數：

- `latitude`: 緯度（浮點數）
- `longitude`: 經度（浮點數）

### 批處理作業狀態 {#batch-job-status}

批處理作業具有以下狀態：

- `STARTING`: 啟動中
- `STARTED`: 已啟動
- `STOPPING`: 停止中
- `STOPPED`: 已停止
- `FAILED`: 失敗
- `COMPLETED`: 完成
- `ABANDONED`: 已放棄

### 考勤狀態 {#attendance-status}

考勤記錄具有以下狀態：

- `NORMAL`: 正常
- `LATE`: 遲到
- `EARLY_LEAVE`: 早退
- `ABSENT`: 缺勤
- `HOLIDAY`: 假日

### 休假類型 {#leave-types}

系統支持以下休假類型：

- `PAID_LEAVE`: 年假
- `SICK_LEAVE`: 病假
- `PERSONAL_LEAVE`: 事假
- `MATERNITY_LEAVE`: 產假
- `PATERNITY_LEAVE`: 陪產假
- `COMPENSATORY_LEAVE`: 補休

### 用戶角色 {#user-roles}

系統支持以下用戶角色：

- `ADMIN`: 管理員
- `MANAGER`: 經理
- `EMPLOYEE`: 普通員工

---
