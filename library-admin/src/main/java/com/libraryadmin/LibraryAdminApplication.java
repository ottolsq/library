package com.libraryadmin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.libraryadmin", "com.librarycommon"})
@MapperScan("com.libraryadmin.mapper")
public class LibraryAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryAdminApplication.class, args);
    }

}
