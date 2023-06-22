package com.example.wechatpaymentdemo.common;

import lombok.Data;

/**
 * @author ChangCheng Lu
 * @date 2023/6/3 13:22
 */
@Data
public class BaseResponse <T> {

    private int code;

    private String message;

    private T data;

    public BaseResponse(int code) {
        this(code, null, null);
    }

    public BaseResponse(int code, String message) {
        this(code, message, null);
    }

    public BaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
