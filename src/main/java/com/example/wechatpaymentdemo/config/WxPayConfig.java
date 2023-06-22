package com.example.wechatpaymentdemo.config;

import com.example.wechatpaymentdemo.exception.BusinessException;
import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.auth.PrivateKeySigner;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Credentials;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Validator;
import com.wechat.pay.contrib.apache.httpclient.cert.CertificatesManager;
import com.wechat.pay.contrib.apache.httpclient.exception.HttpCodeException;
import com.wechat.pay.contrib.apache.httpclient.exception.NotFoundException;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import lombok.Data;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;


@Configuration
@PropertySource("classpath:wxpay.properties")
@ConfigurationProperties(prefix = "wxpay")
@Data
public class WxPayConfig {

    /**
     * 商户号
     */
    private String mchId;

    /**
     * 商户API证书序列号
     */
    private String mchSerialNo;

    /**
     * 商户私钥文件
     */
    private String privateKeyPath;

    /**
     * APIv3密钥
     */
    private String apiV3Key;

    /**
     * APPID
     */
    private String appid;

    /**
     * 微信服务器地址
     */
    private String domain;

    /**
     * 接收结果通知地址
     */
    private String notifyDomain;

    /**
     * 获取商户私钥文件
     *
     * @return
     */
    private PrivateKey getPrivateKey(String fileName) {
        try {
            return PemUtil.loadPrivateKey(new FileInputStream(fileName));
        } catch (FileNotFoundException e) {
            throw new BusinessException("私钥文件不存在");
        }
    }

    /**
     * 时更新平台证书功能, 获取微信平台公钥
     *
     * @return
     */
    @Bean
    public CloseableHttpClient getWxPayClient() {
        WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create()
                .withMerchant(mchId, mchSerialNo, getPrivateKey(privateKeyPath))
                .withValidator(new WechatPay2Validator(getVerifier()));
        // 通过WechatPayHttpClientBuilder构造的HttpClient，会自动的处理签名和验签，并进行证书自动更新
        return builder.build();
    }

    /**
     * 获取签名验证器
     * @return
     */
    @Bean
    public Verifier getVerifier() {
        // 获取证书管理器实例
        CertificatesManager certificatesManager = CertificatesManager.getInstance();
        // 向证书管理器增加需要自动更新平台证书的商户信息
        try {
            certificatesManager.putMerchant(mchId, new WechatPay2Credentials(mchId,
                    new PrivateKeySigner(mchSerialNo, getPrivateKey(privateKeyPath))), apiV3Key.getBytes(StandardCharsets.UTF_8));
            // 从证书管理器中获取verifier
            return certificatesManager.getVerifier(mchId);
        } catch (IOException | GeneralSecurityException | HttpCodeException | NotFoundException e) {
            e.printStackTrace();
        }
        throw new BusinessException("证书不存在");
    }
}
