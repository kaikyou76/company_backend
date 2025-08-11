-- 完整的用户表结构设计
-- 与User实体类完全匹配

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
    skip_location_check BOOLEAN,
    
    -- 添加约束
    CONSTRAINT users_location_type_check 
        CHECK (location_type IN ('office', 'client'))
);

-- 添加索引以提高查询性能
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_employee_id ON users(employee_id);
CREATE INDEX idx_users_department_id ON users(department_id);
CREATE INDEX idx_users_position_id ON users(position_id);
CREATE INDEX idx_users_manager_id ON users(manager_id);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_is_active ON users(is_active);

-- 添加注释说明各字段用途
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