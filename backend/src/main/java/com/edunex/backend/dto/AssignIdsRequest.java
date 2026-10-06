package com.edunex.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssignIdsRequest(@NotNull List<Long> ids) {
}
