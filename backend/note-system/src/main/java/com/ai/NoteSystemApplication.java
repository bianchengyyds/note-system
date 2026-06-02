package com.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.servlet.context.ServletComponentScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@ServletComponentScan // 开启了SpringBoot的Servlet组件的支持
@SpringBootApplication
@MapperScan("com.ai.mapper")  // 扫描 mapper 包
@EnableScheduling
public class NoteSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoteSystemApplication.class, args);
//        // 启动 Spring 容器
//        ConfigurableApplicationContext context = SpringApplication.run(NoteSystemApplication.class, args);
//
//        // 获取 PasswordEncoder
//        PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
//
//        // 生成加密密码
//        String rawPassword = "123456";
//        String encodedPassword = encoder.encode(rawPassword);
//
//        System.out.println("====== 加密后的密码请复制 ======");
//        System.out.println(encodedPassword);
//        System.out.println("==============================");
    }

}
