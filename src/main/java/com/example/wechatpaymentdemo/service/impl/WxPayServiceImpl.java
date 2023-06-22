package com.example.wechatpaymentdemo.service.impl;

import cn.hutool.core.lang.hash.Hash;
import cn.hutool.db.sql.Order;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.wechatpaymentdemo.common.ErrorCode;
import com.example.wechatpaymentdemo.config.WxPayConfig;
import com.example.wechatpaymentdemo.controller.WxPayController;
import com.example.wechatpaymentdemo.exception.BusinessException;
import com.example.wechatpaymentdemo.model.entity.OrderInfo;
import com.example.wechatpaymentdemo.model.entity.Product;
import com.example.wechatpaymentdemo.model.entity.RefundInfo;
import com.example.wechatpaymentdemo.model.enums.OrderStatus;
import com.example.wechatpaymentdemo.model.enums.wxpay.WxApiType;
import com.example.wechatpaymentdemo.model.enums.wxpay.WxNotifyType;
import com.example.wechatpaymentdemo.model.enums.wxpay.WxRefundStatus;
import com.example.wechatpaymentdemo.model.enums.wxpay.WxTradeState;
import com.example.wechatpaymentdemo.service.*;
import com.example.wechatpaymentdemo.util.OrderNoUtils;
import com.google.gson.Gson;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author ChangCheng Lu
 * @date 2023/6/20 21:34
 */
@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Resource
    private WxPayConfig wxPayConfig;

    @Resource
    private CloseableHttpClient httpClient;

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private PaymentInfoService paymentInfoService;

    @Resource
    private RefundInfoService refundInfoService;

    @Resource
    private ProductService productService;

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public Map<String, String> nativePay(Long productId) {
        // 1. 生成订单
        OrderInfo orderInfo = orderInfoService.createOrderInfoByProductId(productId);

        if (orderInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 调用统一下单 API
        HttpPost httpPost = new HttpPost(wxPayConfig.getDomain().concat(WxApiType.NATIVE_PAY.getType()));
        // 请求body参数
        Gson gson = new Gson();
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("appid", wxPayConfig.getAppid());
        paramsMap.put("mchid", wxPayConfig.getMchId());
        paramsMap.put("description", orderInfo.getTitle());
        paramsMap.put("out_trade_no", orderInfo.getOrderNo());
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.NATIVE_NOTIFY.getType()));

        Map<String, Object> amountMap = new HashMap<>();
        amountMap.put("total", orderInfo.getTotalFee());
        amountMap.put("currency", "CNY");

        paramsMap.put("amount", amountMap);

        String jsonParams = gson.toJson(paramsMap);

        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        //完成签名并执行请求
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int statusCode = response.getStatusLine().getStatusCode();
            //处理成功
            if (statusCode == 200) {
                log.info("success,return body = " + EntityUtils.toString(response.getEntity()));
            } else if (statusCode == 204) {
                //处理成功，无返回Body
                log.info("success");
            } else {
                System.out.println("failed,resp code = " + statusCode + ",return body = " + EntityUtils.toString(response.getEntity()));
                throw new IOException("request failed");
            }

            Map<String, String> resultMap = gson.fromJson(EntityUtils.toString(response.getEntity()), Map.class);
            String codeUrl = resultMap.get("code_url");

            if (codeUrl == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }

            // 更新订单数据
            orderInfoService.saveCodeUrl(orderInfo.getOrderNo(), codeUrl);

            orderInfo.setCodeUrl(codeUrl);

            Map<String, String> returnMap = new HashMap<>();
            returnMap.put("codeUrl", codeUrl);
            returnMap.put("orderNo", orderInfo.getOrderNo());

            return returnMap;
        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR);
    }

    @Override
    public void processOrder(Map<String, Object> bodyMap) {
        log.info("处理订单");
        // 解密报文
        String plainText = decryptFromResource(bodyMap);
        // 将明文转换为 map
        Gson gson = new Gson();
        Map<String, String> plainMap = gson.fromJson(plainText, HashMap.class);
        String orderNo = plainMap.get("out_trade_no");

        if (lock.tryLock()) {
            try {
                //处理重复通知
                //保证接口调用的幂等性：无论接口被调用多少次，产生的结果是一致的
                String orderStatus = orderInfoService.getOrderStatus(orderNo);
                if (!OrderStatus.NOTPAY.equals(orderStatus)) {
                    return;
                }
                // 更新订单状态
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
                // 记录支付日志
                paymentInfoService.createPaymentInfo(plainText);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void cancelOrder(String orderNo) throws IOException {
        // 调用微信支付系统取消订单API
        closeOrder(orderNo);
        // 更新本地订单数据库状态
        UpdateWrapper<OrderInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo);
        updateWrapper.set("order_status", OrderStatus.CANCEL.getType());
        boolean update = orderInfoService.update(updateWrapper);
        if (!update) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    @Override
    public String queryOrder(String orderNo) throws IOException {
        log.info("查单接口调用 ===> {}", orderNo);
        String url = String.format(WxApiType.ORDER_QUERY_BY_NO.getType(), orderNo);
        url = wxPayConfig.getDomain().concat(url).concat("?mchid=").concat(wxPayConfig.getMchId());
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");
        //完成签名并执行请求
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            String bodyAsString = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log.info("成功, 返回结果 = " + bodyAsString);
            } else if (statusCode == 204) {
                log.info("成功");
            } else {
                log.info("Native下单失败,响应码 = " + statusCode + ",返回结果 = " +
                        bodyAsString);
                throw new IOException("request failed");
            }
            return bodyAsString;
        }
    }

    /**
     * 根据订单号查询微信支付查单接口，核实订单状态
     * 如果订单已支付，则更新商户端订单状态，并记录支付日志
     * 如果订单未支付，则调用关单接口关闭订单，并更新商户端订单状态
     *
     * @param orderNo
     */
    @Override
    public void checkOrderStatus(String orderNo) throws IOException {
        log.warn("根据订单号核实订单状态 ===> {}", orderNo);

        //调用微信支付查单接口
        String result = this.queryOrder(orderNo);
        Gson gson = new Gson();
        Map<String, String> resultMap = gson.fromJson(result, HashMap.class);
        //获取微信支付端的订单状态
        String tradeState = resultMap.get("trade_state");

        if (lock.tryLock()) {
            try {
                //判断订单状态
                if (WxTradeState.SUCCESS.getType().equals(tradeState)) {
                    log.warn("核实订单已支付 ===> {}", orderNo);
                    //如果确认订单已支付则更新本地订单状态
                    orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
                    //记录支付日志
                    paymentInfoService.createPaymentInfo(result);
                }
            } finally {
                lock.unlock();
            }
        }

        if (WxTradeState.NOTPAY.getType().equals(tradeState)) {
            log.warn("核实订单未支付 ===> {}", orderNo);
            //如果订单未支付，则调用关单接口
            this.closeOrder(orderNo);
            //更新本地订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
        }
    }

    @Override
    public void refunds(String orderNo, Integer refund, String reason) {
        // 退款API
        String url = wxPayConfig.getDomain().concat(WxApiType.DOMESTIC_REFUNDS.getType());
        HttpPost httpPost = new HttpPost(url);

        // 创建退款记录
        RefundInfo refundInfo = refundInfoService.createRefundInfoByOrderNo(orderNo, refund, reason);

        // 拼接退款参数
        HashMap<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("transaction_id", orderNo);
        paramsMap.put("out_refund_no", refundInfo.getRefundNo());
        paramsMap.put("reason", reason);
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.REFUND_NOTIFY.getType()));
        paramsMap.put("funds_account", reason);

        HashMap<String, Object> amountMap = new HashMap<>();
        amountMap.put("refund", refund);
        amountMap.put("total", refundInfo.getTotalFee());
        amountMap.put("currency", "CNY");

        Map<String, Object> fromMap = new HashMap<>();
        fromMap.put("account", reason);
        fromMap.put("amount", refundInfo.getTotalFee());

        amountMap.put("from", fromMap);

        // 查询订单信息
        OrderInfo orderInfo = orderInfoService.getOrderInfoByOrderNo(orderNo);
        Long productId = orderInfo.getProductId();
        // 查询商品信息
        Product product = productService.getById(productId);

        Map<String, Object> goodsDetailMap = new HashMap<>();
        goodsDetailMap.put("merchant_goods_id", productId);
        goodsDetailMap.put("wechatpay_goods_id", productId);
        goodsDetailMap.put("goods_name", product.getTitle());
        goodsDetailMap.put("unit_price", product.getPrice());
        goodsDetailMap.put("refund_amount", refund);
        goodsDetailMap.put("refund_quantity", 1);

        paramsMap.put("goods_detail", goodsDetailMap);

        // 格式化为JSON
        Gson gson = new Gson();
        String jsonParams = gson.toJson(paramsMap);

        StringEntity entity = new StringEntity(jsonParams, "UTF-8");
        // 设置请求报文格式
        entity.setContentType("application/json");
        //将请求报文放入请求对象
        httpPost.setEntity(entity);
        //设置响应报文格式
        httpPost.setHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(httpPost);) {
            // 解析响应报文体
            String bodyAsString = EntityUtils.toString(response.getEntity());
            // 判断响应状态
            HashMap<String, Object> responseMap = gson.fromJson(bodyAsString, HashMap.class);
            String status = (String) responseMap.get("status");
            if (WxRefundStatus.SUCCESS.equals(status)) {
                log.info("成功, 退款返回结果 = " + bodyAsString);
            } else {
                // 更新订单状态
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_ABNORMAL);
                throw new RuntimeException("退款异常, 响应码 = " + status + ", 退款返回结果 = " + bodyAsString);
            }
            // 更新订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);
            // 更新退款记录状态
            refundInfoService.updateRefund(bodyAsString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String queryRefund(String refundNo) {
        log.info("查询退款接口调用 ===> {}", refundNo);
        String url = String.format(WxApiType.DOMESTIC_REFUNDS_QUERY.getType(), refundNo);
        url = wxPayConfig.getDomain().concat(url);

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(httpGet);) {
            Gson gson = new Gson();
            String bodyAsString = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log.info("成功, 查询退款返回结果 = " + bodyAsString);
            } else if (statusCode == 204) {
                log.info("成功");
            } else {
                throw new RuntimeException("查询退款异常, 响应码 = " + statusCode + ", 查询退款返回结果 = " + bodyAsString);
            }

            return bodyAsString;
        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new BusinessException(ErrorCode.SYSTEM_ERROR);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void checkRefundStatus(String refundNo) {
        String result = queryRefund(refundNo);

        //组装json请求体字符串
        Gson gson = new Gson();
        Map<String, String> resultMap = gson.fromJson(result, HashMap.class);
        //获取微信支付端退款状态
        String status = resultMap.get("status");
        String orderNo = resultMap.get("out_trade_no");

        if (WxRefundStatus.SUCCESS.getType().equals(status)) {
            log.warn("核实订单已退款成功 ===> {}", refundNo);
            //如果确认退款成功，则更新订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);
            //更新退款单
            refundInfoService.updateRefund(result);
        }
        if (WxRefundStatus.ABNORMAL.getType().equals(status)) {
            log.warn("核实订单退款异常 ===> {}", refundNo);
            //如果确认退款成功，则更新订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_ABNORMAL);
            //更新退款单
            refundInfoService.updateRefund(result);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void processRefund(Map<String, Object> bodyMap) {
        log.info("退款单");
        //解密报文
        String plainText = decryptFromResource(bodyMap);
        //将明文转换成map
        Gson gson = new Gson();
        HashMap plainTextMap = gson.fromJson(plainText, HashMap.class);
        String orderNo = (String) plainTextMap.get("out_trade_no");
        if (lock.tryLock()) {
            try {
                String orderStatus = orderInfoService.getOrderStatus(orderNo);
                if (!OrderStatus.REFUND_PROCESSING.getType().equals(orderStatus)) {
                    return;
                }
                //更新订单状态
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);
                //更新退款单
                refundInfoService.updateRefund(plainText);
            } finally {
                //要主动释放锁
                lock.unlock();
            }
        }
    }

    /**
     * 调用微信支付系统取消订单API
     *
     * @param orderNo
     */
    private void closeOrder(String orderNo) throws IOException {
        String url = String.format(WxApiType.CLOSE_ORDER_BY_NO.getType(), orderNo);
        url = wxPayConfig.getDomain().concat(url);
        HttpPost httpPost = new HttpPost();

        Gson gson = new Gson();
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("mchid", wxPayConfig.getMchId());
        String jsonParams = gson.toJson(paramsMap);
        log.info("请求参数 ===> {}", jsonParams);

        //将请求参数设置到请求对象中
        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        //完成签名并执行请求
        try (CloseableHttpResponse response = httpClient.execute(httpPost);) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log.info("成功200");
            } else if (statusCode == 204) {
                log.info("成功204");
            } else {
                log.info("Native下单失败,响应码 = " + statusCode);
                throw new IOException("request failed");
            }
        }
    }

    private String decryptFromResource(Map<String, Object> bodyMap) {
        log.info("密文解密");
        // 通知数据
        Map<String, String> resourceMap = (Map) bodyMap.get("resource");
        // 数据密文
        String ciphertext = resourceMap.get("ciphertext");
        // 随机串 nonce
        String nonce = resourceMap.get("nonce");
        // 附加数据
        String associatedData = resourceMap.get("associated_data");

        AesUtil aesUtil = new AesUtil(wxPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8));

        String plainText = null;

        try {
            plainText = aesUtil.decryptToString(associatedData.getBytes(StandardCharsets.UTF_8), nonce.getBytes(StandardCharsets.UTF_8), ciphertext);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        return plainText;
    }

}
