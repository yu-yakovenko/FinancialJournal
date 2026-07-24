package org.tonique.vocal.api.dto;

public record IngestResponse(int matched, int needsReview, int skipped) {
}
