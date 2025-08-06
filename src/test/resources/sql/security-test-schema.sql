-- セキュリティテスト専用テーブル
-- 既存のcomsys_testデータベースに追加するテーブル

-- セキュリティテスト用ユーザーテーブル
CREATE TABLE IF NOT EXISTS security_test_users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    full_name VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    location_type VARCHAR(50) DEFAULT 'office',
    department_id INTEGER,
    position_id INTEGER,
    manager_id INTEGER,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- セキュリティテスト用セッションテーブル
CREATE TABLE IF NOT EXISTS security_test_sessions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES security_test_users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    token_type VARCHAR(50) NOT NULL DEFAULT 'ACCESS', -- ACCESS, REFRESH
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP
);

-- レート制限テスト用ログテーブル
CREATE TABLE IF NOT EXISTS security_rate_limit_log (
    id SERIAL PRIMARY KEY,
    ip_address INET NOT NULL,
    user_id INTEGER REFERENCES security_test_users(id),
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    request_count INTEGER DEFAULT 1,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    is_blocked BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- XSS攻撃テスト用ログテーブル
CREATE TABLE IF NOT EXISTS security_xss_test_log (
    id SERIAL PRIMARY KEY,
    attack_pattern TEXT NOT NULL,
    input_data TEXT NOT NULL,
    sanitized_data TEXT,
    is_blocked BOOLEAN NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    user_id INTEGER REFERENCES security_test_users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- CSRF攻撃テスト用ログテーブル
CREATE TABLE IF NOT EXISTS security_csrf_test_log (
    id SERIAL PRIMARY KEY,
    csrf_token VARCHAR(255),
    is_valid_token BOOLEAN NOT NULL,
    origin_header VARCHAR(255),
    referer_header VARCHAR(255),
    is_blocked BOOLEAN NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    user_id INTEGER REFERENCES security_test_users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- SQLインジェクション攻撃テスト用ログテーブル
CREATE TABLE IF NOT EXISTS security_sql_injection_test_log (
    id SERIAL PRIMARY KEY,
    attack_pattern TEXT NOT NULL,
    input_parameter VARCHAR(255) NOT NULL,
    query_attempted TEXT,
    is_blocked BOOLEAN NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    user_id INTEGER REFERENCES security_test_users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- セキュリティテスト実行結果テーブル
CREATE TABLE IF NOT EXISTS security_test_results (
    id SERIAL PRIMARY KEY,
    test_suite_name VARCHAR(255) NOT NULL,
    test_case_name VARCHAR(255) NOT NULL,
    test_type VARCHAR(50) NOT NULL, -- JWT, XSS, CSRF, RATE_LIMIT, SQL_INJECTION
    status VARCHAR(20) NOT NULL, -- PASSED, FAILED, SKIPPED, ERROR
    execution_time_ms BIGINT,
    error_message TEXT,
    test_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- セキュリティテスト設定テーブル
CREATE TABLE IF NOT EXISTS security_test_config (
    id SERIAL PRIMARY KEY,
    config_key VARCHAR(255) UNIQUE NOT NULL,
    config_value TEXT NOT NULL,
    config_type VARCHAR(50) NOT NULL, -- STRING, INTEGER, BOOLEAN, JSON
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- インデックス作成
CREATE INDEX IF NOT EXISTS idx_security_test_users_username ON security_test_users(username);
CREATE INDEX IF NOT EXISTS idx_security_test_sessions_user_id ON security_test_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_security_test_sessions_token_hash ON security_test_sessions(token_hash);
CREATE INDEX IF NOT EXISTS idx_security_rate_limit_log_ip_endpoint ON security_rate_limit_log(ip_address, endpoint);
CREATE INDEX IF NOT EXISTS idx_security_test_results_test_type ON security_test_results(test_type);
CREATE INDEX IF NOT EXISTS idx_security_test_results_status ON security_test_results(status);
CREATE INDEX IF NOT EXISTS idx_security_test_results_created_at ON security_test_results(created_at);

-- 初期設定データ挿入
INSERT INTO security_test_config (config_key, config_value, config_type, description) VALUES
('jwt.test.expiration', '300000', 'INTEGER', 'JWT テスト用有効期限 (5分)'),
('rate.limit.requests.per.minute', '10', 'INTEGER', 'レート制限: 1分あたりのリクエスト数'),
('rate.limit.requests.per.hour', '100', 'INTEGER', 'レート制限: 1時間あたりのリクエスト数'),
('xss.protection.enabled', 'true', 'BOOLEAN', 'XSS保護機能の有効/無効'),
('csrf.protection.enabled', 'true', 'BOOLEAN', 'CSRF保護機能の有効/無効'),
('sql.injection.protection.enabled', 'true', 'BOOLEAN', 'SQLインジェクション保護機能の有効/無効')
ON CONFLICT (config_key) DO NOTHING;

-- テスト用ユーザーデータ挿入
INSERT INTO security_test_users (username, password_hash, email, full_name, role, location_type) VALUES
('security_test_admin', '$2a$10$test.hash.for.security.admin.user', 'security.admin@company.test', 'Security Test Admin', 'ADMIN', 'office'),
('security_test_user', '$2a$10$test.hash.for.security.normal.user', 'security.user@company.test', 'Security Test User', 'USER', 'office'),
('security_test_manager', '$2a$10$test.hash.for.security.manager.user', 'security.manager@company.test', 'Security Test Manager', 'MANAGER', 'office')
ON CONFLICT (username) DO NOTHING;