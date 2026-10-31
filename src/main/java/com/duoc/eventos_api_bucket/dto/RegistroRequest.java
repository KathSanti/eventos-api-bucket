package com.duoc.eventos_api_bucket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank String nombre,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password
) {}

