# 位置検証機能仕様書

## 概要

本システムでは、従業員の勤怠打刻時に位置情報の検証を行うことで、業務の適正な実施を確保します。位置検証機能は以下の4つの主要機能で構成されています。

## 機能一覧

| 機能ID | 機能名 | 概要 | 実装状況 |
|--------|--------|------|----------|
| F-201 | 位置情報検証（オフィス） | 自社出勤社員はオフィス座標との距離検証（100m以内） | ✅ 完了 |
| F-202 | 位置情報検証（顧客先） | 顧客先派遣社員は個別設定された緯度経度と照合 | ✅ 完了 |
| F-203 | 位置チェックスキップ判定 | 顧客先派遣社員で「位置チェック不要」フラグがある場合はスキップ | ✅ 完了 |
| F-204 | 重複打刻チェック | 同日に同種別（出勤/退勤）の打刻がないか確認 | ✅ 完了 |

## 詳細仕様

### F-201 位置情報検証（オフィス）

#### 概要
オフィス勤務の従業員が打刻する際、登録されたオフィスのいずれかの座標から100m以内にいるかを検証します。

#### 処理フロー
1. ユーザーの[location_type](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/entity/User.java#L24-L25)が"office"であることを確認
2. 登録されているすべてのオフィス位置情報を取得
3. ユーザーの現在位置と各オフィス位置の距離を計算
4. いずれかのオフィスから100m以内であれば検証OK、そうでなければエラー

#### 距離計算方法
ハバーサイン公式を使用して正確な距離を計算します。

#### エラーメッセージ
```
オフィスから100m以上離れた場所での打刻はできません
```

### F-202 位置情報検証（顧客先）

#### 概要
顧客先勤務の従業員が打刻する際、登録された顧客先のいずれかの座標から500m以内にいるかを検証します。

#### 処理フロー
1. ユーザーの[location_type](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/entity/User.java#L24-L25)が"client"であることを確認
2. 登録されているすべての顧客先位置情報を取得
3. ユーザーの現在位置と各顧客先位置の距離を計算
4. いずれかの顧客先から500m以内であれば検証OK、そうでなければエラー

#### 距離計算方法
ハバーサイン公式を使用して正確な距離を計算します。

#### エラーメッセージ
```
指定された客先から500m以上離れた場所での打刻はできません
```

### F-203 位置チェックスキップ判定

#### 概要
特定のユーザーに対して位置チェックをスキップできるようにする特例対応機能です。

#### 処理フロー
1. ユーザーの[skipLocationCheck](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/entity/User.java#L63-L64)フラグがtrueであるか確認
2. trueの場合は位置検証をスキップして打刻処理を続行

#### 利用シーン
- 特殊な事情で位置情報が取得できない場合
- 特別認可されたユーザーの場合
- システムテスト時

### F-204 重複打刻チェック

#### 概要
同日に同種別の打刻（出勤または退勤）がすでに行われていないかを確認します。

#### 処理フロー
1. 当日のユーザーの勤怠記録を取得
2. 出勤打刻の場合、すでに"in"タイプの記録が存在するか確認
3. 退勤打刻の場合、すでに"out"タイプの記録が存在するか確認
4. 存在する場合はエラーを返す

#### 追加の重複チェック
5分以内に同種別の打刻がないかも確認し、ある場合は重複と判断します。

#### エラーメッセージ
```
既に出勤打刻済みです
既に退勤打刻済みです
5分以内に重複する出勤打刻があります
5分以内に重複する退勤打刻があります
```

## データベース構造

### usersテーブル

| カラム名 | 型 | 説明 |
|----------|----|------|
| [location_type](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/entity/User.java#L24-L25) | VARCHAR(20) | 勤務場所タイプ（office/client） |
| [client_latitude](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/entity/User.java#L27-L28) | DOUBLE | クライアント勤務地の緯度 |
| [client_longitude](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/entity/User.java#L30-L31) | DOUBLE | クライアント勤務地の経度 |
| [skip_location_check](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/entity/User.java#L63-L64) | BOOLEAN | 位置チェックスキップフラグ |

### work_locationsテーブル

| カラム名 | 型 | 説明 |
|----------|----|------|
| id | BIGINT | 主キー |
| name | TEXT | 勤務地名 |
| type | TEXT | 勤務地タイプ（office/client/other） |
| latitude | DOUBLE | 緯度 |
| longitude | DOUBLE | 経度 |
| radius | INTEGER | 許容半径（メートル） |
| is_active | BOOLEAN | 有効フラグ |

## API仕様

### 出勤打刻API
```
POST /api/attendance/clock-in
```

#### リクエストボディ
```json
{
  "latitude": 35.6812,
  "longitude": 139.7671
}
```

#### レスポンス
```json
{
  "success": true,
  "message": "出勤打刻が完了しました",
  "data": {
    "recordId": 1,
    "timestamp": "2023-01-01T09:00:00+09:00"
  }
}
```

### 退勤打刻API
```
POST /api/attendance/clock-out
```

#### リクエストボディ
```json
{
  "latitude": 35.6812,
  "longitude": 139.7671
}
```

#### レスポンス
```json
{
  "success": true,
  "message": "退勤打刻が完了しました",
  "data": {
    "recordId": 2,
    "timestamp": "2023-01-01T18:00:00+09:00"
  }
}
```

## エラーハンドリング

### 位置情報検証エラー
```json
{
  "success": false,
  "message": "オフィスから100m以上離れた場所での打刻はできません"
}
```

### 重複打刻エラー
```json
{
  "success": false,
  "message": "既に出勤打刻済みです"
}
```

## テストケース

### 位置検証テスト
1. オフィス勤務者 - 正常位置での打刻
2. オフィス勤務者 - 範囲外位置での打刻（エラー）
3. 顧客先勤務者 - 正常位置での打刻
4. 顧客先勤務者 - 範囲外位置での打刻（エラー）
5. 位置チェックスキップフラグが立っている場合の打刻

### 重複打刻テスト
1. 同日に複数回出勤打刻（エラー）
2. 同日に複数回退勤打刻（エラー）
3. 5分以内の重複打刻（エラー）

## 実装ファイル

### エンティティ
- [User.java](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/entity/User.java) - ユーザー情報（skipLocationCheckフィールド追加）
- [WorkLocation.java](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/entity/WorkLocation.java) - 勤務地情報

### リポジトリ
- [WorkLocationRepository.java](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/repository/WorkLocationRepository.java) - 勤務地情報取得（findByTypeメソッド追加）

### サービス
- [AttendanceService.java](file:///F:/Company_system_project/company_backend/src/main/java/com/example/companybackend/service/AttendanceService.java) - 位置検証ロジック実装

### テスト
- [AttendanceControllerTest.java](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/controller/AttendanceControllerTest.java) - コントローラーテスト
- [AttendanceServiceLocationTest.java](file:///F:/Company_system_project/company_backend/src/test/java/com/example/companybackend/service/AttendanceServiceLocationTest.java) - 位置検証機能の単体テスト

## 注意点

1. 位置情報は打刻時に必須です
2. 緯度は-90〜90、経度は-180〜180の範囲内でなければなりません
3. 顧客先勤務者の許容範囲は500m、オフィス勤務者は100mです
4. 重複打刻防止のため、5分以内の同種別打刻はエラーになります