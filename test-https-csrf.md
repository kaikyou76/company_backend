# HTTPS/CSRF修正機能テストガイド

## 実装完了項目

✅ **1. バックエンドHTTPS設定**
- 環境別設定ファイル（dev/prod）作成
- SecurityProperties と CsrfProperties クラス実装
- HTTPSリダイレクト設定実装

✅ **2. SecurityConfig HTTPS対応**
- 環境別CORS設定実装
- CSRF Cookie の環境別制御実装

✅ **3. フロントエンドAPI設定更新**
- 環境変数での自動プロトコル選択実装
- 混合コンテンツエラーの検出・処理実装

✅ **4. CSRFトークン取得修正**
- 専用CSRFコントローラー実装
- デバッグ情報付きレスポンス実装

✅ **5. エラーハンドリング・ログ改善**
- セキュリティ設定ログ出力実装
- 詳細なデバッグ情報実装

✅ **6. 統合テスト**
- HTTPS/CSRF統合テスト実装
- フロントエンド診断ユーティリティ実装

## テスト手順

### 1. 開発環境でのテスト

```bash
# バックエンド起動（開発環境）
cd company_backend
./mvnw spring-boot:run

# 別ターミナルでCSRFエンドポイントテスト
curl -X GET http://localhost:8080/api/csrf/token -H "Accept: application/json" -v

# CSRFステータス確認
curl -X GET http://localhost:8080/api/csrf/status -H "Accept: application/json" -v
```

### 2. 本番環境設定でのテスト

```bash
# 本番プロファイルで起動
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"

# HTTPSエンドポイントテスト（SSL証明書設定後）
curl -X GET https://localhost:8443/api/csrf/token -H "Accept: application/json" -v
```

### 3. フロントエンドテスト

```bash
# フロントエンド起動
cd company_frontend
npm run dev

# ブラウザで https://localhost:3000 にアクセス
# 開発者ツールでネットワークタブを確認
# CSRFトークン取得リクエストが成功することを確認
```

## 期待される結果

### 開発環境
- HTTP通信でCSRFトークンが正常に取得される
- 混合コンテンツエラーが発生しない
- セキュリティログが適切に出力される

### 本番環境
- HTTPS通信でCSRFトークンが正常に取得される
- HTTPからHTTPSへのリダイレクトが動作する
- セキュアCookieが適切に設定される

## トラブルシューティング

### 1. データベース接続エラー
- application-dev.properties でH2データベース設定を確認
- 必要に応じてMySQL/PostgreSQL設定を追加

### 2. SSL証明書エラー
- 本番環境でのSSL証明書配置を確認
- keystore.p12ファイルの存在と権限を確認

### 3. CORS エラー
- SecurityConfig のallowedOriginsを確認
- フロントエンドのオリジンが正しく設定されているか確認

## 次のステップ

1. **SSL証明書の設定**: 本番環境でのSSL証明書配置
2. **環境変数の設定**: 本番環境での適切な環境変数設定
3. **負荷テスト**: HTTPS環境での性能テスト
4. **セキュリティテスト**: CSRF保護機能の検証