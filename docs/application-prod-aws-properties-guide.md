# application-prod-aws.properties 設定案内書

## 概要
このドキュメントは、AWS本番環境用の設定ファイル `application-prod-aws.properties` の各設定項目について、その役割、関連するJavaクラス、使用場所を詳しく説明します。

---

## 📊 データベース設定

### PostgreSQL基本設定
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/comsys
spring.datasource.username=postgres
spring.datasource.password=AM2023japan
spring.datasource.driver-class-name=org.postgresql.Driver
```

**役割**: PostgreSQLデータベースへの接続設定
**使用場所**: 
- Spring Bootの自動設定により`DataSource` Beanが作成される
- `org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration`
- JPA/Hibernateがデータベース操作時に使用

**セキュリティ注意**: パスワードは環境変数での管理を推奨

### JPA/Hibernate設定
```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
```

**役割**: 
- `ddl-auto=validate`: データベーススキーマの検証のみ（本番環境では安全）
- `show-sql=false`: SQLログを非表示（本番環境でのパフォーマンス向上）
- `format_sql=false`: SQL整形を無効化

**使用場所**: 
- `org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration`
- 全てのJPAエンティティ操作時に適用

### PostgreSQL最適化設定
```properties
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true
spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Tokyo
spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=true
```

**役割**: 
- `lob.non_contextual_creation=true`: PostgreSQLのLOB作成問題を回避
- `time_zone=Asia/Tokyo`: 日本時間での時刻処理
- `allow_jdbc_metadata_access=true`: JDBCメタデータアクセスを許可

**使用場所**: Hibernateの内部処理で使用

### HikariCP接続プール設定
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000
```

**役割**: データベース接続プールの最適化
- `maximum-pool-size=20`: 最大20接続まで
- `minimum-idle=5`: 最低5接続を維持
- `idle-timeout=300000`: 5分間未使用で接続を閉じる
- `connection-timeout=20000`: 20秒でタイムアウト

**使用場所**: `com.zaxxer.hikari.HikariDataSource`

---

## 📝 ログ設定

```properties
logging.level.com.example.companybackend=WARN
logging.level.org.springframework.security=WARN
logging.level.org.hibernate.SQL=WARN
```

**役割**: 本番環境でのログレベル制御（パフォーマンス向上）
**使用場所**: 
- `SecurityLoggingConfig.java` でログ出力
- 全てのLoggerインスタンスに適用

---

## 🔒 セキュリティ設定

### Spring Security基本設定
```properties
spring.security.debug=false
```

**役割**: セキュリティデバッグモードを無効化（本番環境）
**使用場所**: `SecurityConfig.java`

### JWT設定
```properties
jwt.secret=your_secure_jwt_secret_key_here_that_is_long_enough
jwt.expiration=86400
jwt.refresh.expiration=604800
jwt.issuer=company-backend-prod
```

**役割**: JWT認証トークンの設定
- `secret`: JWT署名用の秘密鍵
- `expiration=86400`: アクセストークン有効期限（24時間）
- `refresh.expiration=604800`: リフレッシュトークン有効期限（7日間）
- `issuer`: トークン発行者識別子

**使用場所**: JWTユーティリティクラス（現在は未実装のようです）

---

## 🌐 HTTPS/SSL設定

### SSL基本設定
```properties
server.port=443
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD:changeit}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat
```

**役割**: HTTPS通信の有効化
**使用場所**: 
- Spring Bootの組み込みTomcatサーバー
- `HttpsRedirectConfigProdAws.java` でHTTPSポートとして参照

### HTTP→HTTPSリダイレクト設定
```properties
server.http.port=8080
server.redirect.https=true
```

**役割**: HTTPリクエストをHTTPSにリダイレクト
**使用場所**: `HttpsRedirectConfigProdAws.java`
```java
@Value("${server.http.port:8080}")
private int httpPort;
```

### HSTS設定
```properties
server.ssl.hsts.enabled=true
server.ssl.hsts.max-age=31536000
server.ssl.hsts.include-subdomains=true
```

**役割**: HTTP Strict Transport Security（HSTS）の設定
- `max-age=31536000`: 1年間HSTSを有効
- `include-subdomains=true`: サブドメインも含める

**使用場所**: Spring Bootの自動設定

---

## 📧 メール設定

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.notification.email.enabled=false
```

**役割**: メール送信機能（現在は無効）
**使用場所**: 
- `org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration`
- メール通知サービス（将来実装予定）

---

## 🔄 CORS設定

```properties
app.security.allowed-origins=https://main.d1inikqen7hbn4.amplifyapp.com
```

**役割**: Cross-Origin Resource Sharing（CORS）の許可オリジン設定
**使用場所**: 
- `SecurityProperties.java`
```java
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private String[] allowedOrigins = { "http://localhost:3000" };
}
```
- `SecurityConfig.java` の `corsConfigurationSource()` メソッド

---

## 🍪 Cookie設定

```properties
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=strict
```

**役割**: セッションCookieのセキュリティ設定
- `secure=true`: HTTPS接続でのみCookieを送信
- `same-site=strict`: 同一サイトからのリクエストでのみCookieを送信

**使用場所**: Spring Bootの自動設定

---

## 🛡️ CSRF設定

```properties
app.csrf.cookie-secure=true
app.csrf.cookie-domain=.amplifyapp.com
```

**役割**: CSRF保護のCookie設定
**使用場所**: 
- `CsrfProperties.java`
```java
@ConfigurationProperties(prefix = "app.csrf")
public class CsrfProperties {
    private boolean cookieSecure = false;
    private String cookieDomain = "localhost";
}
```
- `SecurityConfig.java` の `createCookieCsrfTokenRepository()` メソッド

### CSRF保護詳細設定
```properties
app.security.csrf.enabled=true
app.security.csrf.monitoring-mode=true
app.security.csrf.warning-mode=true
app.security.csrf.origin-validation-enabled=true
```

**役割**: CSRF保護の段階的導入設定
**使用場所**: 現在は設定のみ定義（実装は今後予定）

---

## 📋 設定チェックリスト

### 本番環境デプロイ前の確認事項

1. **データベース設定**
   - [ ] パスワードが環境変数で管理されている
   - [ ] 接続プールサイズが適切に設定されている

2. **セキュリティ設定**
   - [ ] JWT秘密鍵が十分に長く複雑である
   - [ ] SSL証明書が正しく設定されている
   - [ ] CORS設定が本番ドメインに合っている

3. **ログ設定**
   - [ ] ログレベルがWARN以上に設定されている
   - [ ] 機密情報がログに出力されていない

4. **Cookie/CSRF設定**
   - [ ] セキュアフラグが有効になっている
   - [ ] ドメイン設定が正しい

---

## 🔧 トラブルシューティング

### よくある問題と解決方法

1. **データベース接続エラー**
   - 接続URL、ユーザー名、パスワードを確認
   - PostgreSQLサービスが起動しているか確認

2. **HTTPS接続エラー**
   - キーストアファイルの存在確認
   - キーストアパスワードの確認

3. **CORS エラー**
   - `app.security.allowed-origins` の設定確認
   - フロントエンドのドメインが正しく設定されているか確認

4. **CSRF エラー**
   - CSRFトークンが正しく送信されているか確認
   - Cookie設定（secure, domain）が適切か確認

---

## 📚 関連ドキュメント

- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/documentation/head/connect.html)

---

**最終更新**: 2024年12月
**対象環境**: AWS EC2 本番環境
**Spring Boot バージョン**: 3.x