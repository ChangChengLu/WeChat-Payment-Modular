package com.example.wechatpaymentdemo.controller;

import com.example.wechatpaymentdemo.common.BaseResponse;
import com.example.wechatpaymentdemo.config.WxPayConfig;
import com.example.wechatpaymentdemo.util.ResultUtil;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author ChangCheng Lu
 * @date 2023/6/20 15:16
 */
@Api(tags = "测试控制器")
@RestController
@RequestMapping("/test")
public class TestController {

    @Resource
    private WxPayConfig wxPayConfig;

    @GetMapping("/get")
    public BaseResponse<String> getWxPayConfig() {
        String mchId = wxPayConfig.getMchId();
        return ResultUtil.ok(mchId);
    }

}
