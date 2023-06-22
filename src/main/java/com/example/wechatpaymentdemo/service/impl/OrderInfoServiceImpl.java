package com.example.wechatpaymentdemo.service.impl;

import cn.hutool.db.sql.Order;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wechatpaymentdemo.common.ErrorCode;
import com.example.wechatpaymentdemo.exception.BusinessException;
import com.example.wechatpaymentdemo.mapper.ProductMapper;
import com.example.wechatpaymentdemo.model.entity.OrderInfo;
import com.example.wechatpaymentdemo.model.entity.Product;
import com.example.wechatpaymentdemo.model.enums.OrderStatus;
import com.example.wechatpaymentdemo.service.OrderInfoService;
import com.example.wechatpaymentdemo.mapper.OrderInfoMapper;
import com.example.wechatpaymentdemo.util.OrderNoUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.weaver.ast.Or;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
* @author 21237
* @description 针对表【t_order_info】的数据库操作Service实现
* @createDate 2023-06-03 14:10:15
*/
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo>
    implements OrderInfoService{

    @Resource
    private ProductMapper productMapper;

    @Resource
    private OrderInfoMapper orderInfoMapper;

    @Override
    public OrderInfo createOrderInfoByProductId(Long productId) {
        // 防止用户重复点击、不断下单，造成服务器压力，选择未支付订单。
        OrderInfo noPayOrderInfoByProductId = getNoPayOrderInfoByProductId(productId);
        if (noPayOrderInfoByProductId != null) {
            return noPayOrderInfoByProductId;
        }

        // 获取商品信息
        Product product = productMapper.selectById(productId);

        // 生成订单
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setTitle(product.getTitle());
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setProductId(productId);
        orderInfo.setTotalFee(product.getPrice());
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());

        orderInfoMapper.insert(orderInfo);

        return orderInfo;
    }

    @Override
    public void saveCodeUrl(String orderNo, String codeUrl) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCodeUrl(codeUrl);

        int update = orderInfoMapper.update(orderInfo, queryWrapper);

        if (update != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    @Override
    public List<OrderInfo> getOrderInfoListByCreatTimeByDesc() {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        List<OrderInfo> orderInfos = orderInfoMapper.selectList(queryWrapper);
        if (orderInfos == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        return orderInfos;
    }

    @Override
    public void updateStatusByOrderNo(String orderNo, OrderStatus status) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(OrderStatus.SUCCESS.getType());

        orderInfoMapper.update(orderInfo, queryWrapper);
    }

    @Override
    public String getOrderStatus(String orderNo) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);

        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);

        if (orderInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        return orderInfo.getOrderStatus();
    }

    @Override
    public List<OrderInfo> getNoPayOrderByDuration(int minutes) {
        Instant instant = Instant.now().minus(Duration.ofMinutes(5));

        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_status", OrderStatus.NOTPAY.getType());
        queryWrapper.lt("create_time", instant);

        return orderInfoMapper.selectList(queryWrapper);
    }

    @Override
    public OrderInfo getOrderInfoByOrderNo(String orderNo) {
        if (StringUtils.isBlank(orderNo)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("oder_no", orderNo);

        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);

        if (orderInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        return orderInfo;
    }

    private OrderInfo getNoPayOrderInfoByProductId(Long productId) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        queryWrapper.eq("order_status", 1);
        return getOne(queryWrapper);
    }
}




