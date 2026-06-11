package com.team04.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record ApiResponse<T>(boolean success, String code, String message, T data) {

    public static <T> ApiResponse<T> ofSuccess(T data) {
        return new ApiResponse<>(true, null, null, data);
    }

    public static ApiResponse<Void> ofSuccessWithoutBody() {
        return new ApiResponse<>(true, null, null, null);
    }

    public static <T> ApiResponse<T> ofFailure(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}