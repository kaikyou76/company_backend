package com.example.companybackend.security.test.sql;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class CommandInjectionAttackPatternFactory {

    private static final List<String> COMMAND_INJECTION_PATTERNS = Arrays.asList(
            ";& cat /etc/passwd",
            "| cat /etc/passwd",
            "&& cat /etc/passwd",
            "; cat /etc/passwd",
            "| ls -la",
            "&& ls -la",
            "; ls -la",
            "`cat /etc/passwd`",
            "$(cat /etc/passwd)",
            "| dir",
            "&& dir",
            "; dir",
            "| net user",
            "&& net user",
            "; net user",
            "`dir`",
            "$(dir)",
            "| id",
            "&& id",
            "; id",
            "`id`",
            "$(id)",
            "| whoami",
            "&& whoami",
            "; whoami",
            "`whoami`",
            "$(whoami)",
            "'; DROP TABLE users; --",
            "\"; DROP TABLE users; --",
            "'; SELECT * FROM users WHERE '1'='1",
            "UNION SELECT username, password FROM users",
            "'; EXEC xp_cmdshell('dir') --"
    );

    private static final Random RANDOM = new Random();

    public static String getRandomPattern() {
        return COMMAND_INJECTION_PATTERNS.get(RANDOM.nextInt(COMMAND_INJECTION_PATTERNS.size()));
    }

    public static List<String> getAllPatterns() {
        return COMMAND_INJECTION_PATTERNS;
    }
}