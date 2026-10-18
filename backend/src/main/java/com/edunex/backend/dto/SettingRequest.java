package com.edunex.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SettingRequest(@NotNull @Size(max = 2000) String value) {
}