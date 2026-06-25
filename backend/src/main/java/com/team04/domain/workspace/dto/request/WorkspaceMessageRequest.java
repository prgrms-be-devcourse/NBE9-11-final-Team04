package com.team04.domain.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceMessageRequest(
        @NotBlank @Size(max = 2000) String content
) {
}
