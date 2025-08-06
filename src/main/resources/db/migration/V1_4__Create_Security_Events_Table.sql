-- セキュリティイベントテーブルの作成
CREATE TABLE IF NOT EXISTS security_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    request_uri VARCHAR(500),
    http_method VARCHAR(10),
    reason VARCHAR(1000),
    payload TEXT,
    user_id BIGINT,
    session_id VARCHAR(100),
    severity_level VARCHAR(20),
    action_taken VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT
);

-- インデックスの作成
CREATE INDEX IF NOT EXISTS idx_security_events_event_type ON security_events(event_type);
CREATE INDEX IF NOT EXISTS idx_security_events_ip_address ON security_events(ip_address);
CREATE INDEX IF NOT EXISTS idx_security_events_created_at ON security_events(created_at);
CREATE INDEX IF NOT EXISTS idx_security_events_severity ON security_events(severity_level);
CREATE INDEX IF NOT EXISTS idx_security_events_user_id ON security_events(user_id);

-- 複合インデックス（アラート検出用）
CREATE INDEX IF NOT EXISTS idx_security_events_alert_check 
ON security_events(ip_address, event_type, created_at);

-- パーティション用のインデックス（将来の拡張用）
CREATE INDEX IF NOT EXISTS idx_security_events_monthly 
ON security_events(event_type, date_trunc('month', created_at));

-- コメント追加
COMMENT ON TABLE security_events IS 'セキュリティイベント記録テーブル';
COMMENT ON COLUMN security_events.event_type IS 'イベントタイプ（CSRF_VIOLATION, XSS_ATTEMPT等）';
COMMENT ON COLUMN security_events.ip_address IS 'クライアントIPアドレス';
COMMENT ON COLUMN security_events.severity_level IS '重要度レベル（LOW, MEDIUM, HIGH, CRITICAL）';
COMMENT ON COLUMN security_events.action_taken IS '実行されたアクション（BLOCKED, ALLOWED, LOGGED等）';
COMMENT ON COLUMN security_events.metadata IS '追加メタデータ（JSON形式）';