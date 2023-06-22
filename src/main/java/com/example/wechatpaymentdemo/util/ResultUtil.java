package com.example.wechatpaymentdemo.util;

import com.example.wechatpaymentdemo.common.BaseResponse;
import com.example.wechatpaymentdemo.common.ErrorCode;

/**
 * @author ChangCheng Lu
 * @date 2023/6/3 13:26
 */
public class ResultUtil {


    public static  <T> BaseResponse<T> ok(T data) {
        return new BaseResponse<T>(200, "成功", data);
    }

    public static  <T> BaseResponse<T> ok() {
        return new BaseResponse<T>(200, "成功", null);
    }

    public static BaseResponse error(ErrorCode errorCode) {
        return new BaseResponse(errorCode.getCode(), errorCode.getMessage());
    }

    public static BaseResponse error(int code, String message) {
        return new BaseResponse(code, message);
    }

}
