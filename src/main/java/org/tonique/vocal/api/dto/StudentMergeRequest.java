package org.tonique.vocal.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** targetId is optional — when omitted the survivor is auto-picked (fullest name wins). */
public record StudentMergeRequest(@NotEmpty List<Long> studentIds, Long targetId) {
}
