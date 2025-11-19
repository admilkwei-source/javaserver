package com.example.javaserver.model.vo;
import lombok.Data;

@Data
public class BaseVo<T> {
    private boolean success;
    private String message;
    private int code;
    private T data;

    public static <T> BaseVo<T> success(T data) {
        BaseVo<T> baseVo = new BaseVo<T>();
        baseVo.setSuccess(true);
        baseVo.setCode(200);
        baseVo.setMessage("成功");
        baseVo.setData(data);
        return baseVo;
    }
    public static <T> BaseVo<T> error(String message,T data) {
        BaseVo<T> baseVo = new BaseVo<T>();
        baseVo.setSuccess(false);
        baseVo.setCode(-1);
        baseVo.setMessage(message);
        baseVo.setData(data);
        return baseVo;
    }
}
