package com.ges.boutique.vente;

import lombok.Data;
import java.util.Map;

@Data
public class ApiResponse {
    private boolean success;
    private String message;
    private Map<String, Object> data;

    public static ApiResponse success(Map<String, Object> data) {
        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage("Opération réussie");
        response.setData(data);
        return response;
    }

    public static ApiResponse success(String message, Map<String, Object> data) {
        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
}