package com.example.LibrarySystem.dto;

import com.example.LibrarySystem.model.Book;

public record RecommendationBookDto(
        Long id,
        String title,
        String author,
        String summary,
        boolean available,
        int availableCopies,
        double score
) {
    public static RecommendationBookDto from(Book book, double score) {
        return new RecommendationBookDto(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getSummary(),
                book.isAvailable(),
                book.getAvailableCopies(),
                score
        );
    }
}
