package com.example.LibrarySystem.dto;

import com.example.LibrarySystem.model.Book;

public record MlBookDto(
        Long id,
        String title,
        String author,
        String summary,
        boolean available,
        int availableCopies
) {
    public static MlBookDto from(Book book) {
        return new MlBookDto(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getSummary(),
                book.isAvailable(),
                book.getAvailableCopies()
        );
    }
}
