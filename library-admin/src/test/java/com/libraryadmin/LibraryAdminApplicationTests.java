package com.libraryadmin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "security.jwt.secret=context-test-secret-0123456789")
class LibraryAdminApplicationTests {

    @Test
    void contextLoads() {
    }

}
