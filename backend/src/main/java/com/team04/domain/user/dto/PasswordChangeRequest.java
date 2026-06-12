package com.team04.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordChangeRequest(
        @NotBlank
        String currentPassword,
        @NotBlank
        @Pattern(
                regexp =  "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&]).{8,20}$",
                message = "비밀번호는 8~20자, 영문·숫자·특수문자를 포함해야 합니다"
        )
        String newPassword
) {
}