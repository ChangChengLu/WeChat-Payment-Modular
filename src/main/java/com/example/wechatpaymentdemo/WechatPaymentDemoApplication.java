package com.example.wechatpaymentdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.wechatpaymentdemo.mapper")
@EnableScheduling
public class WechatPaymentDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(WechatPaymentDemoApplication.class, args);
    }

}
