package com.example.companybackend.security.test.sql;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MaliciousFileNameFactory {

    private static final List<String> MALICIOUS_FILE_NAMES = Arrays.asList(
            "../../../etc/passwd",
            "..\\..\\..\\windows\\system32\\drivers\\etc\\hosts",
            "../../../../../../../../etc/passwd",
            "../../../../../../../../windows/system32/drivers/etc/hosts",
            "..%2F..%2F..%2Fetc%2Fpasswd",
            "..%5C..%5C..%5Cwindows%5Csystem32%5Cdrivers%5Cetc%5Chosts",
            "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd",
            "%2e%2e%5c%2e%2e%5c%2e%2e%5cwindows%5csystem32%5cdrivers%5cetc%5chosts",
            "..%252F..%252F..%252Fetc%252Fpasswd",
            "..%255C..%255C..%255Cwindows%255Csystem32%255Cdrivers%255Cetc%255Chosts",
            "/etc/passwd%00.jpg",
            "C:\\boot.ini%00.jpg",
            "../../../boot.ini",
            "../../../../../../../../boot.ini",
            "..%2F..%2F..%2Fboot.ini",
            "%2e%2e%2f%2e%2e%2f%2e%2e%2fboot.ini",
            "..%252F..%252F..%252Fboot.ini",
            "....//....//....//etc/passwd",
            "....\\\\....\\\\....\\\\windows\\\\system32\\\\drivers\\\\etc\\\\hosts",
            "test.jsp%00.jpg",
            "test.php%00.png",
            "test.asp%00.gif",
            "test.aspx%00.bmp",
            "shell.jsp",
            "shell.php",
            "shell.asp",
            "shell.aspx",
            "backdoor.jsp",
            "backdoor.php",
            "evil.jsp",
            "evil.php"
    );

    private static final Random RANDOM = new Random();

    public static String getRandomMaliciousFileName() {
        return MALICIOUS_FILE_NAMES.get(RANDOM.nextInt(MALICIOUS_FILE_NAMES.size()));
    }

    public static List<String> getAllMaliciousFileNames() {
        return MALICIOUS_FILE_NAMES;
    }
}