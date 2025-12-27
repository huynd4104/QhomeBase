package com.QhomeBase.marketplaceservice.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private BigDecimal price;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    @Size(max = 200, message = "Location must not exceed 200 characters")
    private String location;

    private ContactInfoRequest contactInfo;

    // IDs of images to delete
    private List<String> imagesToDelete;

    // ID of video to delete (only one video allowed per post)
    private String videoToDelete;
    
    // Video URL (if video was uploaded to data-docs-service separately)
    private String videoUrl;
}

