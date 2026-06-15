package com.myweb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.myweb.repository")
@EnableScheduling
public class MyWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyWebApplication.class, args);
    }
}
