package com.QhomeBase.customerinteractionservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyFeedbackRequest {
    @NotBlank(message = "Note is required")
    private String note;
}


