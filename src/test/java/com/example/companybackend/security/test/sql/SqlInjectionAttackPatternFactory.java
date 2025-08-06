package com.example.companybackend.security.test.sql;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SqlInjectionAttackPatternFactory {

    private static final List<String> SQL_INJECTION_PATTERNS = Arrays.asList(
            "'; DROP TABLE users; --",
            "'; SELECT * FROM users WHERE '1'='1",
            "'; INSERT INTO users (username, password) VALUES ('admin', 'hacked') --",
            "'; UPDATE users SET password='newpass' WHERE username='admin' --",
            "'; DELETE FROM users WHERE '1'='1",
            "UNION SELECT username, password FROM users",
            "'; EXEC xp_cmdshell('dir') --",
            "' OR '1'='1' --",
            "' OR 1=1 --",
            "'; DROP DATABASE test; --",
            "'; CREATE USER hacker IDENTIFIED BY 'password'; --",
            "'; GRANT DBA TO hacker; --",
            "'; ALTER TABLE users ADD COLUMN ssn VARCHAR(11); --",
            "'; COMMIT; --",
            "'; ROLLBACK; --"
    );

    private static final Random RANDOM = new Random();

    public static String getRandomPattern() {
        return SQL_INJECTION_PATTERNS.get(RANDOM.nextInt(SQL_INJECTION_PATTERNS.size()));
    }

    public static List<String> getAllPatterns() {
        return SQL_INJECTION_PATTERNS;
    }
}