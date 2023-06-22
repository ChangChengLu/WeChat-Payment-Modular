package com.example.wechatpaymentdemo.service;

import java.io.IOException;
import java.util.Map;

/**
 * @author ChangCheng Lu
 * @date 2023/6/20 21:34
 */
public interface WxPayService {
    /**
     * 返回二维码地址和订单号
     * @param productId
     * @return
     */
    Map<String, String> nativePay(Long productId);

    /**
     * 处理订单
     * @param bodyMap
     */
    void processOrder(Map<String, Object> bodyMap);

    /**
     * 取消订饭
     * @param orderNo
     */
    void cancelOrder(String orderNo) throws IOException;

    /**
     * 查询微信支付系统订单信息
     * @param orderNo
     * @return
     */
    String queryOrder(String orderNo) throws IOException;

    /**
     * 查询订单状态
     * @param orderNo
     */
    void checkOrderStatus(String orderNo) throws IOException;

    /**
     * 申请退款
     * @param orderNo
     * @param refund
     * @param reason
     */
    void refunds(String orderNo, Integer refund, String reason);

    /**
     * 查询单笔订单
     * @param refundNo
     * @return
     */
    String queryRefund(String refundNo);

    /**
     * 处理超时为退款订单：核实订单状态
     * @param refundNo
     */
    void checkRefundStatus(String refundNo);

    /**
     * 处理退款单
     * @param bodyMap
     */
    void processRefund(Map<String, Object> bodyMap);
}
