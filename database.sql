-- ===============================================
-- 会社勤怠管理システム データベーススキーマ
-- enum型を使用しない設計（既存データベースとの互換性を保持）
-- ===============================================

-- 部署テーブル
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    code TEXT NOT NULL UNIQUE,
    manager_id INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 職位テーブル
CREATE TABLE positions (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    level INT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- ユーザーテーブル
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    location_type VARCHAR(255) NOT NULL,
    client_latitude DOUBLE PRECISION,
    client_longitude DOUBLE PRECISION,
    manager_id INTEGER,
    department_id INTEGER,
    position_id INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    employee_id VARCHAR(255),
    full_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(255),
    hire_date DATE,
    is_active BOOLEAN,
    role VARCHAR(255),
    last_login_at TIMESTAMPTZ,
    skip_location_check BOOLEAN
);

-- リフレッシュトークンテーブル
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token TEXT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    revoked BOOLEAN DEFAULT FALSE NOT NULL,
    revoked_at TIMESTAMPTZ
);

-- 勤怠打刻テーブル
CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('in', 'out')),
    timestamp TIMESTAMPTZ NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 休暇申請テーブル
CREATE TABLE leave_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('paid', 'sick', 'special')),
    status VARCHAR(20) DEFAULT 'pending' NOT NULL CHECK (status IN ('pending', 'approved', 'rejected')),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    approver_id INTEGER,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 祝日・休日カレンダーテーブル
CREATE TABLE holidays (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    name TEXT NOT NULL,
    is_recurring BOOLEAN DEFAULT false NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 勤務時間集計テーブル
CREATE TABLE attendance_summaries (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    target_date DATE NOT NULL,
    total_hours DECIMAL(5,2) DEFAULT 0.00 NOT NULL,
    overtime_hours DECIMAL(5,2) DEFAULT 0.00 NOT NULL,
    late_night_hours DECIMAL(5,2) DEFAULT 0.00 NOT NULL,
    holiday_hours DECIMAL(5,2) DEFAULT 0.00 NOT NULL,
    summary_type VARCHAR(20) DEFAULT 'daily' NOT NULL CHECK (summary_type IN ('daily', 'monthly')),
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 残業時間レポートテーブル
CREATE TABLE overtime_reports (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    target_month DATE NOT NULL,
    total_overtime DECIMAL(5,2) DEFAULT 0.00 NOT NULL,
    total_late_night DECIMAL(5,2) DEFAULT 0.00 NOT NULL,
    total_holiday DECIMAL(5,2) DEFAULT 0.00 NOT NULL,
    status VARCHAR(20) DEFAULT 'draft' NOT NULL CHECK (status IN ('draft', 'confirmed', 'approved')),
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 打刻修正申請テーブル
CREATE TABLE time_corrections (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    attendance_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL CHECK (request_type IN ('time', 'type', 'both')),
    before_time TIMESTAMPTZ NOT NULL,
    current_type VARCHAR(10) NOT NULL CHECK (current_type IN ('in', 'out')),
    requested_time TIMESTAMPTZ,
    requested_type VARCHAR(10) CHECK (requested_type IN ('in', 'out')),
    reason TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'pending' NOT NULL CHECK (status IN ('pending', 'approved', 'rejected')),
    approver_id INTEGER,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 勤務場所情報テーブル
CREATE TABLE work_locations (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('office', 'client', 'other')),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    radius INT DEFAULT 100 NOT NULL,
    is_active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- システムログテーブル
CREATE TABLE system_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER,
    action TEXT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('success', 'error', 'warning')),
    ip_address TEXT,
    user_agent TEXT,
    details JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- IP制限設定テーブル
CREATE TABLE ip_whitelist (
    id BIGSERIAL PRIMARY KEY,
    ip_address CIDR NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- 通知メッセージテーブル
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('leave', 'correction', 'system')),
    is_read BOOLEAN DEFAULT false NOT NULL,
    related_id INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- インデックス作成
CREATE INDEX idx_attendance_user_timestamp ON attendance_records (user_id, timestamp);
CREATE UNIQUE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_employee_id ON users (employee_id);
CREATE INDEX idx_users_department_id ON users (department_id);
CREATE INDEX idx_users_position_id ON users (position_id);
CREATE INDEX idx_users_manager_id ON users (manager_id);
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_is_active ON users (is_active);
CREATE INDEX idx_leave_requests_user_status ON leave_requests (user_id, status);
CREATE UNIQUE INDEX idx_holidays_date ON holidays (date);
CREATE INDEX idx_attendance_summaries_user_date ON attendance_summaries (user_id, target_date);
CREATE INDEX idx_overtime_reports_user_month ON overtime_reports (user_id, target_month);
CREATE INDEX idx_time_corrections_user_status ON time_corrections (user_id, status);
CREATE INDEX idx_work_locations_name ON work_locations (name);
CREATE INDEX idx_system_logs_action_date ON system_logs (action, created_at);
CREATE UNIQUE INDEX idx_ip_whitelist_address ON ip_whitelist (ip_address);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read);
-- リフレッシュトークンテーブルのインデックス
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens (expiry_date);

-- ===============================================
-- 外部キー制約の追加（既存データベースと同じ方式）
-- ===============================================

-- 部署テーブルの外部キー（循環参照を避けるため後で追加）
ALTER TABLE departments ADD CONSTRAINT fk_departments_manager
    FOREIGN KEY (manager_id) REFERENCES users(id);

-- ユーザーテーブルの外部キー
ALTER TABLE users ADD CONSTRAINT users_manager_id_fkey
    FOREIGN KEY (manager_id) REFERENCES users(id);
ALTER TABLE users ADD CONSTRAINT users_department_id_fkey
    FOREIGN KEY (department_id) REFERENCES departments(id);
ALTER TABLE users ADD CONSTRAINT users_position_id_fkey
    FOREIGN KEY (position_id) REFERENCES positions(id);

-- ユーザーテーブルの制約
ALTER TABLE users ADD CONSTRAINT users_location_type_check
    CHECK (location_type IN ('office', 'client'));

-- リフレッシュトークンテーブルの外部キー
ALTER TABLE refresh_tokens ADD CONSTRAINT refresh_tokens_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id);

-- 勤怠打刻テーブルの外部キー
ALTER TABLE attendance_records ADD CONSTRAINT attendance_records_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id);

-- 休暇申請テーブルの外部キー
ALTER TABLE leave_requests ADD CONSTRAINT leave_requests_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE leave_requests ADD CONSTRAINT leave_requests_approver_id_fkey
    FOREIGN KEY (approver_id) REFERENCES users(id);

-- 勤務時間集計テーブルの外部キー
ALTER TABLE attendance_summaries ADD CONSTRAINT attendance_summaries_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id);

-- 残業時間レポートテーブルの外部キー
ALTER TABLE overtime_reports ADD CONSTRAINT overtime_reports_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id);

-- 打刻修正申請テーブルの外部キー
ALTER TABLE time_corrections ADD CONSTRAINT time_corrections_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE time_corrections ADD CONSTRAINT time_corrections_attendance_id_fkey
    FOREIGN KEY (attendance_id) REFERENCES attendance_records(id);
ALTER TABLE time_corrections ADD CONSTRAINT time_corrections_approver_id_fkey
    FOREIGN KEY (approver_id) REFERENCES users(id);

-- システムログテーブルの外部キー
ALTER TABLE system_logs ADD CONSTRAINT system_logs_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id);

-- 通知メッセージテーブルの外部キー
ALTER TABLE notifications ADD CONSTRAINT notifications_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id);

-- ユーザーテーブルのコメント
COMMENT ON TABLE users IS '用户信息表';
COMMENT ON COLUMN users.id IS '用户ID';
COMMENT ON COLUMN users.username IS '用户名（登录用）';
COMMENT ON COLUMN users.password_hash IS '密码哈希值';
COMMENT ON COLUMN users.location_type IS '位置类型（office:办公室, client:客户现场）';
COMMENT ON COLUMN users.client_latitude IS '客户现场纬度';
COMMENT ON COLUMN users.client_longitude IS '客户现场经度';
COMMENT ON COLUMN users.manager_id IS '直属经理ID';
COMMENT ON COLUMN users.department_id IS '部门ID';
COMMENT ON COLUMN users.position_id IS '职位ID';
COMMENT ON COLUMN users.created_at IS '创建时间';
COMMENT ON COLUMN users.updated_at IS '更新时间';
COMMENT ON COLUMN users.employee_id IS '员工编号';
COMMENT ON COLUMN users.full_name IS '员工全名';
COMMENT ON COLUMN users.email IS '电子邮箱';
COMMENT ON COLUMN users.phone IS '联系电话';
COMMENT ON COLUMN users.hire_date IS '入职日期';
COMMENT ON COLUMN users.is_active IS '是否激活';
COMMENT ON COLUMN users.role IS '用户角色（admin, manager, employee）';
COMMENT ON COLUMN users.last_login_at IS '最后登录时间';
COMMENT ON COLUMN users.skip_location_check IS '是否跳过位置检查';