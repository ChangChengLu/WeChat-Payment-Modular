package com.example.wechatpaymentdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wechatpaymentdemo.model.entity.OrderInfo;
import com.example.wechatpaymentdemo.model.entity.RefundInfo;
import com.example.wechatpaymentdemo.model.enums.wxpay.WxRefundStatus;
import com.example.wechatpaymentdemo.service.OrderInfoService;
import com.example.wechatpaymentdemo.service.RefundInfoService;
import com.example.wechatpaymentdemo.mapper.RefundInfoMapper;
import com.example.wechatpaymentdemo.util.OrderNoUtils;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

/**
* @author 21237
* @description 针对表【t_refund_info】的数据库操作Service实现
* @createDate 2023-06-03 14:10:14
*/
@Service
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo>
    implements RefundInfoService{

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private RefundInfoMapper refundInfoMapper;

    @Override
    public RefundInfo createRefundInfoByOrderNo(String orderNo, Integer refund, String reason) {
        // 查询订单
        OrderInfo orderInfo = orderInfoService.getOrderInfoByOrderNo(orderNo);

        // 创建退款记录
        RefundInfo refundInfo = new RefundInfo();

        refundInfo.setOrderNo(orderNo);
        refundInfo.setRefundNo(OrderNoUtils.getRefundNo());
        refundInfo.setTotalFee(orderInfo.getTotalFee());
        refundInfo.setRefund(refund);
        refundInfo.setReason(reason);
        refundInfo.setRefundStatus(WxRefundStatus.PROCESSING.getType());

        // 更新数据库
        refundInfoMapper.insert(refundInfo);

        return refundInfo;
    }

    @Override
    public void updateRefund(String content) {
        Gson gson = new Gson();
        HashMap<String, String> resultMap = gson.fromJson(content, HashMap.class);

        //根据退款单编号修改退款单
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", resultMap.get("out_refund_no"));

        //设置要修改的字段
        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundId(resultMap.get("refund_id"));
        //查询退款和申请退款中的返回参数
        if(resultMap.get("status") != null){
            refundInfo.setRefundStatus(resultMap.get("status"));
            refundInfo.setContentReturn(content);
        }
        //退款回调中的回调参数
        if(resultMap.get("refund_status") != null){
            refundInfo.setRefundStatus(resultMap.get("refund_status"));
            refundInfo.setContentNotify(content);
        }
        //更新退款单
        baseMapper.update(refundInfo, queryWrapper);
    }

    @Override
    public List<RefundInfo> getNoRefundOrderByDuration(int minutes) {
        Instant instant = Instant.now().minus(Duration.ofMinutes(minutes));

        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_status", WxRefundStatus.PROCESSING.getType());
        queryWrapper.le("create_time", instant);

        return baseMapper.selectList(queryWrapper);
    }
}




