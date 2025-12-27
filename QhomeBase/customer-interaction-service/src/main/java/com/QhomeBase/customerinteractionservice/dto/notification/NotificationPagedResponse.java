package com.QhomeBase.customerinteractionservice.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPagedResponse {
    private List<NotificationResponse> content;
    private int currentPage;      // 0-based page number
    private int pageSize;          // Number of items per page (7)
    private long totalElements;    // Total number of notifications
    private int totalPages;       // Total number of pages
    private boolean hasNext;       // Has next page
    private boolean hasPrevious;   // Has previous page
    private boolean isFirst;       // Is first page
    private boolean isLast;        // Is last page
}

