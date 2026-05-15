package com.example.trekking_app.dto.admin;

import io.swagger.v3.oas.annotations.media.SchemaProperty;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AccountStatusResetRequest {

    private int userId;
    @SchemaProperty(name = "isActive")
    private boolean isActive;
}
