package com.QhomeBase.baseservice.dto;

import java.util.UUID;

public record AssignInspectorRequest(
        UUID inspectorId,
        String inspectorName
) {}



