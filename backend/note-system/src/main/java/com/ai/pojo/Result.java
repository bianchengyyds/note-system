package com.ai.pojo;

import lombok.Data;

/**
 * 后端统一返回结果（泛型版）
 */
@Data
public class Result<T> {

    private Integer code; // 编码：1成功，0为失败
    private String msg; // 错误信息
    private T data; // 数据（泛型类型）

    // 成功返回（无数据）
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = "success";
        return result;
    }

    // 成功返回（带数据）
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.data = data;
        result.code = 1;
        result.msg = "success";
        return result;
    }

    // 失败返回
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.msg = msg;
        result.code = 0;
        return result;
    }
}