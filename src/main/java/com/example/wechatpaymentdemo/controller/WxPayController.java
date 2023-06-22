package com.example.wechatpaymentdemo.controller;

import com.example.wechatpaymentdemo.common.BaseResponse;
import com.example.wechatpaymentdemo.service.WxPayService;
import com.example.wechatpaymentdemo.util.HttpUtils;
import com.example.wechatpaymentdemo.util.ResultUtil;
import com.example.wechatpaymentdemo.util.WechatPay2ValidatorForRequest;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.google.gson.Gson;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ChangCheng Lu
 * @date 2023/6/20 21:33
 */
@CrossOrigin
@Api(tags = "网站微信支付API")
@RestController
@RequestMapping("/wx-pay")
@Slf4j
public class WxPayController {

    @Resource
    private WxPayService wxPayService;

    @Resource
    private Verifier verifier;


    @ApiOperation("调用统一下单API，生成支付二维码")
    @PostMapping("/native/{productId}")
    public BaseResponse<Map<String, String>> nativePay(@PathVariable Long productId) {
        log.info("发起支付请求");
        // 返回二维码地址和订单号
        return ResultUtil.ok(wxPayService.nativePay(productId));
    }

    @PostMapping("/native/notify")
    public BaseResponse<String> nativeNotify(HttpServletRequest request, HttpServletResponse response) {
        Gson gson = new Gson();

        Map<String, String> resultMap = new HashMap<>();

        String body = HttpUtils.readData(request);
        Map<String, Object> bodyMap = gson.fromJson(body, HashMap.class);
        String requestId = (String) bodyMap.get("id");
        log.info("支付通知ID: {}", bodyMap.get("id"));
        log.info("支付通知完整数据: {}", body);

        // 签名验证
        WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest = new WechatPay2ValidatorForRequest(verifier, requestId, body);
        try {
            boolean validate = wechatPay2ValidatorForRequest.validate(request);
            if (!validate) {
                // 失败应答
                resultMap.put("code", "ERROR");
                resultMap.put("message", "失败");
                return ResultUtil.ok(gson.toJson(resultMap));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 处理订单
        wxPayService.processOrder(bodyMap);
        // 成功应答
        resultMap.put("code", "SUCCESS");
        resultMap.put("message", "成功");

        return ResultUtil.ok(gson.toJson(resultMap));
    }

    @ApiOperation("查询订单：测试订单状态用")
    @GetMapping("/query/{orderNo}")
    public BaseResponse<String> queryOrder(@PathVariable String orderNo) throws Exception {
        log.info("查询订单");
        String bodyAsString = wxPayService.queryOrder(orderNo);
        return ResultUtil.ok(bodyAsString);
    }

    @ApiOperation("申请退款")
    @PostMapping("/refunds/{orderNo}/{refund}/{reason}")
    public BaseResponse<String> refunds(@PathVariable String orderNo, @PathVariable Integer refund, @PathVariable String reason) {
        wxPayService.refunds(orderNo, refund, reason);

        return ResultUtil.ok();
    }

    @ApiOperation("查询退款：测试用")
    @GetMapping("/query-refund/{refundNo}")
    public BaseResponse<String> queryRefund(@PathVariable String refundNo) throws Exception {
        log.info("查询退款");
        String result = wxPayService.queryRefund(refundNo);
        return ResultUtil.ok(result);
    }

    @PostMapping("/refunds/notify")
    public String refundsNotify(HttpServletRequest request, HttpServletResponse response) {
        log.info("退款通知执行");
        Gson gson = new Gson();
        Map<String, String> map = new HashMap<>();

        try {
            //处理通知参数
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = gson.fromJson(body, HashMap.class);
            String requestId = (String)bodyMap.get("id");
            log.info("支付通知的id ===> {}", requestId);
            //签名的验证
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest
                    = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if(!wechatPay2ValidatorForRequest.validate(request)){
                log.error("通知验签失败");
                //失败应答
                response.setStatus(500);
                map.put("code", "ERROR");
                map.put("message", "通知验签失败");
                return gson.toJson(map);
            }
            log.info("通知验签成功");
            //处理退款单
            wxPayService.processRefund(bodyMap);
            //成功应答
            response.setStatus(200);
            map.put("code", "SUCCESS");
            map.put("message", "成功");
            return gson.toJson(map);
        } catch (IOException e) {
            e.printStackTrace();
            //失败应答
            response.setStatus(500);
            map.put("code", "ERROR");
            map.put("message", "失败");
            return gson.toJson(map);
        }
    }
}
