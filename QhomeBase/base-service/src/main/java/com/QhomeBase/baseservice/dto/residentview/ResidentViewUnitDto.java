package com.QhomeBase.baseservice.dto.residentview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentViewUnitDto {
    private UUID unitId;
    private String unitCode;
    private Long residentCount;
    // We might not load residents immediately for list view, but the requirement
    // says "Click unit -> open Modal".
    // So maybe just count is enough for the grid, and a separate endpoint for
    // details?
    // User said: "Mỗi ô gồm: Icon căn hộ/ nhà: A101, 4 cư dân".
    // Then "Click vào căn hộ → mở Modal hiển thị bảng".
    // So this DTO is for the grid item.
}
