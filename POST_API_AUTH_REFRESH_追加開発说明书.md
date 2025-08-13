# POST /api/auth/refresh 追加開発詳細流程说明书

## 1. 概要

この文書は、会社勤怠管理システムにおける `POST /api/auth/refresh` エンドポイントの追加開発に関する詳細な流れを記録したものです。以前のJWT自包含型トークン方式から、データベースベースのトークン管理方式への変更を含みます。

## 2. 開発背景

### 2.1 問題点
- 以前の実装では、リフレッシュトークン機能が未実装（TODO状態）
- JWT自包含型トークン方式では、セキュリティ上の問題が存在
  - トークンの取り消しができない
  - トークン漏洩時の対応が困難
  - ユーザー無効化時のトークン管理ができない

### 2.2 改善目標
- リフレッシュトークン機能の完全実装
- データベースベースのトークン管理によるセキュリティ向上
- トークンの取り消し・無効化機能の実装

## 3. データベース変更

### 3.1 refresh_tokens テーブル追加

```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token TEXT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    revoked BOOLEAN DEFAULT FALSE NOT NULL,
    revoked_at TIMESTAMPTZ
);
```

### 3.2 インデックス追加

```sql
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens (expiry_date);
```

### 3.3 外部キー制約

```sql
ALTER TABLE refresh_tokens ADD CONSTRAINT refresh_tokens_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id);
```

## 4. エンティティクラス追加

### 4.1 RefreshToken クラス (com.example.companybackend.entity)

主なフィールド:
- `id`: トークンID（主キー）
- `token`: トークン文字列（ユニーク）
- `userId`: ユーザーID（外部キー）
- `expiryDate`: 有効期限
- `createdAt`: 作成日時
- `revoked`: 取り消しフラグ
- `revokedAt`: 取り消し日時

主なメソッド:
- `isExpired()`: トークンが期限切れかどうか
- `isRevoked()`: トークンが取り消されているかどうか
- `isValid()`: トークンが有効かどうか

## 5. Repository インターフェース追加

### 5.1 RefreshTokenRepository (com.example.companybackend.repository)

主なメソッド:
- `findByToken(String token)`: トークン文字列で検索
- `findValidByToken(@Param("token") String token)`: 有効なトークンを検索
- `deleteByUserId(Long userId)`: ユーザーIDでトークンを削除

## 6. Service 層の変更

### 6.1 AuthService (com.example.companybackend.service)

#### 6.1.1 依存関係の追加
```java
private final RefreshTokenRepository refreshTokenRepository;
```

#### 6.1.2 コンストラクタの変更
```java
public AuthService(AuthenticationManager authenticationManager,
                   UserRepository userRepository,
                   PositionRepository positionRepository,
                   DepartmentRepository departmentRepository,
                   PasswordEncoder passwordEncoder,
                   JwtTokenProviderService tokenProvider,
                   RefreshTokenRepository refreshTokenRepository) {
    // 既存のフィールド初期化...
    this.refreshTokenRepository = refreshTokenRepository;
}
```

#### 6.1.3 新規メソッドの追加

##### authenticateUserWithTokens(String username, String password)
- ユーザー認証とトークン生成を同時に行う
- アクセストークンとリフレッシュトークンの両方を生成
- リフレッシュトークンをデータベースに保存

##### createRefreshToken(User user)
- ユーザー用の新しいリフレッシュトークンを作成
- 既存のトークンを削除して新しいトークンを保存

##### validateRefreshToken(String token)
- リフレッシュトークンの検証を行う
- トークンの有効性（期限、取り消し状態）を確認

##### generateToken(String username)
- ユーザー名からアクセストークンを生成

## 7. Controller 層の変更

### 7.1 AuthController (com.example.companybackend.controller)

#### 7.1.1 loginUser メソッドの変更
- `authenticateUserWithTokens` メソッドを使用するように変更
- レスポンスにリフレッシュトークンを含める

#### 7.1.2 refreshToken メソッドの実装

##### リクエスト処理フロー:
1. リクエストボディからリフレッシュトークンを取得
2. トークンの形式を検証
3. `authService.validateRefreshToken()` でトークンを検証
4. ユーザー情報を取得
5. 新しいアクセストークンとリフレッシュトークンを生成
6. 成功レスポンスを返す

##### エラーハンドリング:
- トークンが無効な場合: 401 Unauthorized
- システムエラーの場合: 500 Internal Server Error

## 8. API エンドポイント仕様

### 8.1 リクエスト
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

### 8.2 成功レスポンス
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400
}
```

### 8.3 エラーレスポンス
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "success": false,
  "message": "リフレッシュトークンが無効です"
}
```

## 9. セキュリティ対策

### 9.1 トークン管理
- リフレッシュトークンはデータベースに保存
- トークンの有効期限を設定（7日間）
- トークンの取り消し機能を実装

### 9.2 トークン検証
- トークンの形式検証
- 有効期限チェック
- 取り消し状態チェック
- ユーザー存在チェック

### 9.3 ログ出力
- トークンリフレッシュの成功/失敗をログ出力
- セキュリティ監査のために詳細なログを記録

## 10. テストケース

### 10.1 正常系
- 有効なリフレッシュトークンでのトークン更新
- 新しいアクセストークンとリフレッシュトークンの取得

### 10.2 異常系
- 無効なリフレッシュトークン
- 期限切れのリフレッシュトークン
- 取り消されたリフレッシュトークン
- 空のリフレッシュトークン

## 11. バージョン差異

### 11.1 以前の実装（バージョン1.0）
- JWT自包含型トークン方式
- リフレッシュトークン機能未実装
- セキュリティ上の制限あり

### 11.2 現在の実装（バージョン2.0）
- データベースベースのトークン管理
- 完全なリフレッシュトークン機能
- トークンの取り消し・無効化機能
- 改善されたセキュリティ

## 12. 保守性向上のためのポイント

### 12.1 コード構造
- 明確な責務分離（Entity, Repository, Service, Controller）
- 再利用可能なメソッドの実装
- 一貫性のあるエラーハンドリング

### 12.2 ドキュメント
- API仕様書との整合性維持
- コード内コメントの充実
- この说明书による変更履歴の記録

### 12.3 拡張性
- トークンポリシーの柔軟な変更が可能
- 追加のセキュリティ機能の実装が容易
- 監査ログ機能の拡張が可能

## 13. デプロイ手順

### 13.1 データベース更新
1. `refresh_tokens` テーブルの作成
2. 関連するインデックスの作成
3. 外部キー制約の追加

### 13.2 アプリケーションデプロイ
1. 新規クラスファイルのデプロイ
2. 既存クラスの更新
3. 依存関係の確認

### 13.3 テスト
1. 単体テストの実行
2. 結合テストの実行
3. セキュリティテストの実行

## 14. 注意事項

- データベーススキーマ変更は慎重に実施すること
- 既存のクライアントアプリケーションへの影響を考慮すること
- 本番環境へのデプロイは計画的に実施すること