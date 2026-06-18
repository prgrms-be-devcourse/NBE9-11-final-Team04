package com.team04.domain.auth.dto.request;

import com.team04.domain.user.entity.Role;
import jakarta.validation.constraints.*;

public record SignupRequest(
        @Email @NotBlank String email,
        @NotBlank @Pattern(
                regexp =  "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&]).{8,20}$",
                message = "비밀번호는 8~20자, 영문·숫자·특수문자를 포함해야 합니다")
        String password,
        @NotBlank String name,
        @NotBlank String nickname,
        @Min(19) @Max(150) int age,
        @NotNull Role role
) {
}
