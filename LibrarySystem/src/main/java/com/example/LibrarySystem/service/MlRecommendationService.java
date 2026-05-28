package com.example.LibrarySystem.service;

import com.example.LibrarySystem.dto.MlBookDto;
import com.example.LibrarySystem.dto.MlRecommendationRequest;
import com.example.LibrarySystem.dto.MlRecommendationResult;
import com.example.LibrarySystem.dto.RecommendationBookDto;
import com.example.LibrarySystem.exception.ResourceNotFoundException;
import com.example.LibrarySystem.model.Book;
import com.example.LibrarySystem.model.Loan;
import com.example.LibrarySystem.repository.BookRepository;
import com.example.LibrarySystem.repository.LoanRepository;
import com.example.LibrarySystem.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MlRecommendationService {
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.mlServiceUrl:http://localhost:8001}")
    private String mlServiceUrl;

    public MlRecommendationService(
            BookRepository bookRepository,
            LoanRepository loanRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public List<RecommendationBookDto> getSimilarBooks(Long bookId, int limit) {
        bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "Id", bookId));

        List<Book> books = bookRepository.findAll();
        if (books.size() <= 1) {
            return Collections.emptyList();
        }

        MlRecommendationRequest request = new MlRecommendationRequest(
                toMlBooks(books),
                bookId,
                Collections.emptyList(),
                safeLimit(limit)
        );

        List<MlRecommendationResult> results = callMlService("/recommend/books/similar", request);
        return toRecommendationDtos(results, books);
    }

    public List<RecommendationBookDto> getPersonalizedRecommendations(Long memberId, int limit) {
        if (!userRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Member", "Id", memberId);
        }

        List<Loan> memberLoans = loanRepository.findLoansByMemberIdWithBooks(memberId);
        List<Long> borrowedBookIds = memberLoans.stream()
                .map(loan -> loan.getBook().getId())
                .distinct()
                .toList();

        if (borrowedBookIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Book> books = bookRepository.findAll();

        MlRecommendationRequest request = new MlRecommendationRequest(
                toMlBooks(books),
                null,
                borrowedBookIds,
                safeLimit(limit)
        );

        List<MlRecommendationResult> results = callMlService("/recommend/users/personalized", request);
        return toRecommendationDtos(results, books);
    }

    private List<MlBookDto> toMlBooks(List<Book> books) {
        return books.stream()
                .map(MlBookDto::from)
                .toList();
    }

    private List<RecommendationBookDto> toRecommendationDtos(
            List<MlRecommendationResult> results,
            List<Book> books
    ) {
        Map<Long, Book> bookById = books.stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));

        return results.stream()
                .map(result -> {
                    Book book = bookById.get(result.bookId());
                    if (book == null) {
                        return null;
                    }
                    return RecommendationBookDto.from(book, result.score());
                })
                .filter(dto -> dto != null)
                .sorted(Comparator.comparingDouble(RecommendationBookDto::score).reversed())
                .toList();
    }

    private List<MlRecommendationResult> callMlService(String path, MlRecommendationRequest requestBody) {
        try {
            String baseUrl = mlServiceUrl.endsWith("/")
                    ? mlServiceUrl.substring(0, mlServiceUrl.length() - 1)
                    : mlServiceUrl;

            String json = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "ML service returned HTTP " + response.statusCode() + ": " + response.body()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    new TypeReference<List<MlRecommendationResult>>() {}
            );

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "ML recommendation service is not reachable. Start the Python service on port 8001.",
                    ex
            );
        }
    }

    private int safeLimit(int limit) {
        if (limit < 1) {
            return 5;
        }
        return Math.min(limit, 50);
    }
}
