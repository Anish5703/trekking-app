package com.example.trekking_app.dto.global;

import com.example.trekking_app.model.ErrorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ErrorResponse {
    private ErrorType error;
    private String details;
}
