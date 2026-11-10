package com.ferry.system.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAuthorize<S, T> {

    @Schema(description = "授权id")
    private List<S> authorizeIds;

    @Schema(description = "被授权id")
    private List<T> authorizedIds;
}
