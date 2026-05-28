package com.example.LibrarySystem.controller;

import com.example.LibrarySystem.dto.RecommendationBookDto;
import com.example.LibrarySystem.service.MlRecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ml-recommendations")
public class MlRecommendationController {
    private final MlRecommendationService mlRecommendationService;

    public MlRecommendationController(MlRecommendationService mlRecommendationService) {
        this.mlRecommendationService = mlRecommendationService;
    }

    // Example:
    // GET http://localhost:8080/api/ml-recommendations/books/1?limit=5
    @GetMapping("/books/{bookId}")
    public List<RecommendationBookDto> getSimilarBooks(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return mlRecommendationService.getSimilarBooks(bookId, limit);
    }

    // Example:
    // GET http://localhost:8080/api/ml-recommendations/members/1?limit=5
    @GetMapping("/members/{memberId}")
    public List<RecommendationBookDto> getPersonalizedRecommendations(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return mlRecommendationService.getPersonalizedRecommendations(memberId, limit);
    }
}
