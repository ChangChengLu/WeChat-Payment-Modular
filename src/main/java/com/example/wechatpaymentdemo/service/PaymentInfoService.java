package com.example.wechatpaymentdemo.service;

import com.example.wechatpaymentdemo.model.entity.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 21237
* @description 针对表【t_payment_info】的数据库操作Service
* @createDate 2023-06-03 14:10:14
*/
public interface PaymentInfoService extends IService<PaymentInfo> {

    /**
     * 记录支付日志
     * @param plainText
     */
    void createPaymentInfo(String plainText);
}
