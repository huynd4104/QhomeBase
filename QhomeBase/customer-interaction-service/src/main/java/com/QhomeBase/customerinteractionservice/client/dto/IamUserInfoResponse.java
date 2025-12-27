package com.QhomeBase.customerinteractionservice.client.dto;

import java.util.List;

public record IamUserInfoResponse(
        String id,
        String username,
        String email,
        List<String> roles,
        List<String> permissions
) {
}





