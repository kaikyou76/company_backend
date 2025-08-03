# 📘 `CRUSH.md`（勤怠管理システム用）

> This file defines essential guidelines and commands for contributors and agentic tools working in this repository.

---

## 🛠 Build / Lint / Test

* **Build**
  Spring Boot 3.4.x / Java 21 を使用。Maven Wrapper を利用してビルドします。

  ```bash
  ./mvnw clean install
  ```

* **Run App**
  アプリケーション起動（開発用）：

  ```bash
  ./mvnw spring-boot:run
  ```

* **Run Tests**
  単一のテストクラスを実行：

  ```bash
  ./mvnw test -Dtest=AttendanceServiceTest
  ```

* **Lint**
  Spotless によるコード整形を実施：

  ```bash
  ./mvnw spotless:apply
  ```

---

## 🧾 Code Style Guidelines

* **Language**: Java 21 / Spring Boot 3.4.x
* **Indentation**: 4 spaces
* **Naming**:

  * クラス: `PascalCase`
  * 変数・関数: `camelCase`
  * 定数: `UPPER_SNAKE_CASE`
* **Package Structure**:

  * `controller`, `service`, `repository`, `dto`, `entity`, `config`, `batch`, `util`, `command`, `query`
* **DTO・Entity クラス**では `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` を活用
* **REST APIの命名規則**：`/api/{resource}` 形式（例：`/api/attendance`, `/api/leave-requests`）

---

## 🧪 Error Handling & Logging

* 例外処理は `@ControllerAdvice` により一元管理
* ログ出力は `Slf4j` を使用し、ログレベルを適切に分けて使用：

  ```java
  log.info("正常系ログ");
  log.warn("注意ログ");
  log.error("例外発生", e);
  ```

---

## ⚙ External Tools & Configs

* **打刻位置確認**：HTML5 Geolocation API（緯度経度で100m圏内検証）
* **データベース**：PostgreSQL 16 + Supabase（DDL: `schema.sql`）
* **環境設定**：`application-{env}.properties` による環境ごとの切り替え
* **バッチ**：Spring Batch ベース、構成は `batch/config`, `reader`, `writer`, `processor`, `listener`

---

## 🗂 ディレクトリ構造（一部）

* `controller/`：REST API 層（例：`AttendanceController`）
* `service/`：ビジネスロジック（例：`LeaveService`）
* `repository/`：JPAリポジトリ（例：`UserRepository`）
* `entity/`：JPAエンティティ（例：`AttendanceRecord.java`）
* `dto/`：リクエスト/レスポンス/バッチ用DTO
* `batch/`：バッチ処理（config, reader, writer, processorなど）
* `config/`：セキュリティやWeb設定
* `test/`：単体・結合・バッチのテストコード

詳細は [`README.md`](./README.md) および [`docs/`](./docs/) 参照。

---

## 📁 Agent Rule Integration

* Cursor / Copilotルールは以下から読み取ってください：

  * `.cursor/rules/` または `.github/copilot-instructions.md`
* `.crush/` ディレクトリは `.gitignore` に追加済み

---




