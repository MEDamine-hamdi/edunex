package com.edunex.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AccountDto {

    private AccountDto() {
    }

    public record Forgot(@NotBlank @Email @Size(max = 254) String email) {
    }

    public record Reset(
            @NotBlank @Size(max = 200) String token,
            @NotBlank @Size(min = 10, max = 72, message = "must be 10 to 72 characters")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "must contain letters and digits") String newPassword) {
    }

    public record Token(@NotBlank @Size(max = 200) String token) {
    }

    public record EmailChange(@NotBlank @Email @Size(max = 254) String newEmail,
                              @NotBlank @Size(max = 128) String password) {
    }
}