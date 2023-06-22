package com.example.wechatpaymentdemo.service;

import com.example.wechatpaymentdemo.model.entity.RefundInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 21237
* @description 针对表【t_refund_info】的数据库操作Service
* @createDate 2023-06-03 14:10:14
*/
public interface RefundInfoService extends IService<RefundInfo> {

    /**
     * 创建退款记录
     * @param orderNo
     * @param refund
     * @param reason
     * @return
     */
    RefundInfo createRefundInfoByOrderNo(String orderNo, Integer refund, String reason);

    /**
     * 更新退款记录
     * @param bodyAsString
     */
    void updateRefund(String bodyAsString);

    /**
     * 获取超时退款单
     * @param minutes
     * @return
     */
    List<RefundInfo> getNoRefundOrderByDuration(int minutes);
}
