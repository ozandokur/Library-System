package com.example.LibrarySystem.dto;

import java.util.List;

public record MlRecommendationRequest(
        List<MlBookDto> books,
        Long targetBookId,
        List<Long> borrowedBookIds,
        int limit
) {
}
