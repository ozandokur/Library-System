# Python ML Recommendation System Setup

This project now includes a Python FastAPI machine learning recommendation service and Spring Boot endpoints that call it.

## Added endpoints

Spring Boot endpoints:

```text
GET /api/ml-recommendations/books/{bookId}?limit=5
GET /api/ml-recommendations/members/{memberId}?limit=5
```

Python ML service endpoints:

```text
GET  /health
POST /recommend/books/similar
POST /recommend/users/personalized
```

## How it works

The Python service uses content-based recommendation:

1. It combines each book's `title`, `author`, and `summary`.
2. It converts the text into TF-IDF vectors.
3. It calculates cosine similarity between books.
4. It returns similar available books.

For personalized recommendations, the Spring Boot backend sends the member's borrowed book ids to the Python service. The service builds a user profile from those borrowed books and recommends similar unread books.

## 1. Add ML service URL to Spring Boot

Add this line to:

```text
LibrarySystem/src/main/resources/application.properties
```

```properties
app.mlServiceUrl=http://localhost:8001
```

This file already contains local secrets, so it was not changed automatically.

## 2. Start the Python ML service

From the project root:

```bash
cd ml-recommendation-service
python -m venv .venv
```

Git Bash on Windows:

```bash
source .venv/Scripts/activate
```

PowerShell on Windows:

```powershell
.venv\Scripts\Activate.ps1
```

Install dependencies:

```bash
pip install -r requirements.txt
```

Run the service:

```bash
uvicorn main:app --reload --port 8001
```

Test it:

```text
http://localhost:8001/health
```

Expected response:

```json
{"status":"ok"}
```

## 3. Start the Spring Boot backend

Open another terminal:

```bash
cd LibrarySystem
mvn spring-boot:run
```

## 4. Test recommendations

Similar books:

```text
http://localhost:8080/api/ml-recommendations/books/1?limit=5
```

Personalized recommendations:

```text
http://localhost:8080/api/ml-recommendations/members/1?limit=5
```

## Notes

- Personalized recommendations require loan history.
- If a member has never borrowed a book, the personalized endpoint returns an empty list.
- Recommendation quality improves when book summaries are filled.
- Unavailable books are filtered out by the Python service.
