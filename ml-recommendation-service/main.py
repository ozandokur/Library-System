from __future__ import annotations

from typing import List, Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

app = FastAPI(title="Library ML Recommendation Service")


class BookIn(BaseModel):
    id: int
    title: Optional[str] = ""
    author: Optional[str] = ""
    summary: Optional[str] = ""
    available: bool = True
    availableCopies: int = 0


class RecommendationRequest(BaseModel):
    books: List[BookIn]
    targetBookId: Optional[int] = None
    borrowedBookIds: List[int] = Field(default_factory=list)
    limit: int = Field(default=5, ge=1, le=50)


class RecommendationOut(BaseModel):
    bookId: int
    score: float


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


def book_text(book: BookIn) -> str:
    """
    Content-based recommendation text.

    We combine metadata already available in the Spring Boot Book entity:
    title + author + summary.
    """
    text = " ".join(
        part.strip()
        for part in [
            book.title or "",
            book.author or "",
            book.summary or "",
        ]
        if part and part.strip()
    )

    # Prevent "empty vocabulary" if a book has no metadata.
    return text if text else f"book_{book.id}"


def similarity_matrix(books: List[BookIn]):
    corpus = [book_text(book) for book in books]

    vectorizer = TfidfVectorizer(
        lowercase=True,
        ngram_range=(1, 2),
        min_df=1,
    )

    tfidf = vectorizer.fit_transform(corpus)
    return cosine_similarity(tfidf)


def sort_candidates(scores, books: List[BookIn], excluded_ids: set[int], limit: int) -> List[RecommendationOut]:
    ranked = []

    for index, book in enumerate(books):
        if book.id in excluded_ids:
            continue

        # Do not recommend unavailable books unless you intentionally want that.
        if not book.available or book.availableCopies <= 0:
            continue

        ranked.append(
            RecommendationOut(
                bookId=book.id,
                score=round(float(scores[index]), 4),
            )
        )

    ranked.sort(key=lambda item: item.score, reverse=True)
    return ranked[:limit]


@app.post("/recommend/books/similar", response_model=List[RecommendationOut])
def recommend_similar_books(request: RecommendationRequest) -> List[RecommendationOut]:
    if not request.books:
        return []

    if request.targetBookId is None:
        raise HTTPException(status_code=400, detail="targetBookId is required")

    id_to_index = {book.id: index for index, book in enumerate(request.books)}

    if request.targetBookId not in id_to_index:
        raise HTTPException(status_code=404, detail="Target book not found in book list")

    matrix = similarity_matrix(request.books)
    target_index = id_to_index[request.targetBookId]
    scores = matrix[target_index]

    return sort_candidates(
        scores=scores,
        books=request.books,
        excluded_ids={request.targetBookId},
        limit=request.limit,
    )


@app.post("/recommend/users/personalized", response_model=List[RecommendationOut])
def recommend_for_user(request: RecommendationRequest) -> List[RecommendationOut]:
    if not request.books:
        return []

    borrowed_ids = set(request.borrowedBookIds)

    if not borrowed_ids:
        # No borrowing history means there is no user profile yet.
        return []

    id_to_index = {book.id: index for index, book in enumerate(request.books)}
    borrowed_indexes = [
        id_to_index[book_id]
        for book_id in borrowed_ids
        if book_id in id_to_index
    ]

    if not borrowed_indexes:
        return []

    matrix = similarity_matrix(request.books)

    # User profile = average similarity to the books this user borrowed before.
    user_profile_scores = matrix[borrowed_indexes].mean(axis=0)

    return sort_candidates(
        scores=user_profile_scores,
        books=request.books,
        excluded_ids=borrowed_ids,
        limit=request.limit,
    )
