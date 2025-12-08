package com.ecommerce.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    private int code = 1000;

    private String message;
    private T result;

    public static <T> ApiResponse<T> success(T result){
        return ApiResponse.<T>builder()
                .result(result)
                .build();
    }

    public static <T> ApiResponse<T> content(T result){
        return success(result);
    }
}
