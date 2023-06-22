package com.example.wechatpaymentdemo.controller;

import com.example.wechatpaymentdemo.common.BaseResponse;
import com.example.wechatpaymentdemo.config.WxPayConfig;
import com.example.wechatpaymentdemo.model.entity.OrderInfo;
import com.example.wechatpaymentdemo.model.enums.OrderStatus;
import com.example.wechatpaymentdemo.service.OrderInfoService;
import com.example.wechatpaymentdemo.service.WxPayService;
import com.example.wechatpaymentdemo.util.ResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * @author ChangCheng Lu
 * @date 2023/6/21 14:35
 */
@CrossOrigin
@Api(tags = "订单API")
@RestController
@RequestMapping("/order-info")
@Slf4j
public class OrderInfoController {

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private WxPayService wxPayService;

    @PostMapping("/order-list")
    public BaseResponse<List<OrderInfo>> getOrderInfoList() {
        return ResultUtil.ok(orderInfoService.getOrderInfoListByCreatTimeByDesc());
    }

    @ApiOperation("查询本地订单状态")
    @GetMapping("/query-order-status/{orderNo}")
    public BaseResponse queryOrderStatus(@PathVariable String orderNo) {
        String orderStatus = orderInfoService.getOrderStatus(orderNo);
        return OrderStatus.SUCCESS.equals(orderStatus) ? ResultUtil.ok(null) : new BaseResponse(101, "支付中");
    }

    @ApiOperation("用户取消订单")
    @PostMapping("/cancel/{orderNo}")
    public BaseResponse<String> cancelOrder(@PathVariable String orderNo) throws IOException {
        log.info("取消订单");
        wxPayService.cancelOrder(orderNo);
        return ResultUtil.ok();
    }

}
