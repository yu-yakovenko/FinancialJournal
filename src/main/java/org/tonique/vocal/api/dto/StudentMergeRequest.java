package org.tonique.vocal.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StudentMergeRequest(@NotEmpty List<Long> studentIds) {
}
