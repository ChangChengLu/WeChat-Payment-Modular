package com.example.wechatpaymentdemo.service;

import com.example.wechatpaymentdemo.model.entity.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wechatpaymentdemo.model.enums.OrderStatus;
import org.aspectj.weaver.ast.Or;

import java.util.List;

/**
* @author 21237
* @description 针对表【t_order_info】的数据库操作Service
* @createDate 2023-06-03 14:10:15
*/
public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 创建订单
     * @param productId
     * @return
     */
    OrderInfo createOrderInfoByProductId(Long productId);

    /**
     * 存储codeUrl
     * @param orderNo
     * @param codeUrl
     */
    void saveCodeUrl(String orderNo, String codeUrl);

    /**
     * 获取订单列表
     * @return
     */
    List<OrderInfo> getOrderInfoListByCreatTimeByDesc();

    /**
     * 更新订单状态
     * @param orderNo
     * @param status
     */
    void updateStatusByOrderNo(String orderNo, OrderStatus status);

    /**
     * 获取业务数据状态，处理重复通知
     * @param orderNo
     * @return
     */
    String getOrderStatus(String orderNo);

    /**
     * 查村超时订单
     * @param minutes
     * @return
     */
    List<OrderInfo> getNoPayOrderByDuration(int minutes);

    /**
     * 根据订单号查询订单
     * @param orderNo
     * @return
     */
    OrderInfo getOrderInfoByOrderNo(String orderNo);
}
