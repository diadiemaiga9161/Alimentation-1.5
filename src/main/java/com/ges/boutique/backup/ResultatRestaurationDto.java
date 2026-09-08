package com.ges.boutique.backup;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResultatRestaurationDto {
    private boolean success;
    private String message;

    public static ResultatRestaurationDto succes(String message) {
        return new ResultatRestaurationDto(true, message);
    }

    public static ResultatRestaurationDto echec(String message) {
        return new ResultatRestaurationDto(false, message);
    }
}
