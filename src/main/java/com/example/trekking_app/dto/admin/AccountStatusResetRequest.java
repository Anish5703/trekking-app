package com.example.trekking_app.dto.admin;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AccountStatusResetRequest {

    private int userId;
    private boolean isActive;
}
