package com.heben.client.model;

public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public boolean isSuccess() { return code == 200; }
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
