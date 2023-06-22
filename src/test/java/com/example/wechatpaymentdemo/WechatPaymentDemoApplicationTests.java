package com.example.wechatpaymentdemo;

import com.example.wechatpaymentdemo.config.WxPayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class WechatPaymentDemoApplicationTests {

    @Resource
    private WxPayConfig wxPayConfig;

    @Test
    void contextLoads() {
    }

    @Test
    public void getSecretKey() {
        String privateKeyPath = wxPayConfig.getPrivateKeyPath();
        System.out.println(wxPayConfig.getPrivateKey(privateKeyPath));
    }


}
