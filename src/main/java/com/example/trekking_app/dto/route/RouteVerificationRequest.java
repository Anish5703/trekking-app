package com.example.trekking_app.dto.route;

import com.example.trekking_app.model.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteVerificationRequest
{
    private VerificationStatus verificationStatus;
    private String verificationRemarks;
}
