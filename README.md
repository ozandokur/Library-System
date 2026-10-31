# Library Management System

Full-stack library management application with a **Java / Spring Boot REST API**, **Angular frontend**, **MySQL database**, and a separate **Python recommendation microservice**.

This repository brings together application engineering and a small, explainable machine-learning component.

## Technology stack

| Layer | Technologies |
| --- | --- |
| Frontend | Angular 17, TypeScript, Bootstrap |
| Backend | Java 17, Spring Boot 3, Spring Data JPA, Spring Security |
| Database | MySQL |
| ML service | Python, FastAPI, scikit-learn, TF-IDF, cosine similarity |

## Features

- Screens for managing books, members, loans, and librarian workflows.
- REST-based backend, database persistence, and an authentication layer.
- Content-based recommendations for similar books using **title, author, and summary**.
- Member-oriented recommendations based on **borrowing history**.
- Recommendation filtering that excludes previously borrowed or unavailable books.

The recommendation model uses TF-IDF with word unigrams and bigrams, followed by cosine similarity. It does **not** use collaborative filtering, neural networks, or an LLM. For members with no loan history, the personalized recommendation endpoint returns an empty list.

## Repository structure

```text
LibrarySystem/            # Spring Boot backend
MyLibraryFrontend/        # Angular app
ml-recommendation-service/# FastAPI recommendation API
ML_RECOMMENDATION_SETUP.md
```

## Run locally

Prerequisites: Java 17, Node.js/npm, Python, and a running MySQL instance.

**1. Backend and database**

Create a MySQL database and supply the following environment variables to the Spring Boot process:

```text
DB_URL=jdbc:mysql://localhost:3306/YOUR_DATABASE
DB_USERNAME=YOUR_DB_USER
DB_PASSWORD=YOUR_DB_PASSWORD
JWT_SECRET=YOUR_LONG_RANDOM_JWT_SECRET
```

These are **placeholders**, not working credentials. Never commit real passwords or JWT signing secrets. The backend reads them from `LibrarySystem/src/main/resources/application.properties`.

```bash
cd LibrarySystem
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd spring-boot:run`.

**2. Angular frontend** (from the repository root, in a separate terminal)

```bash
cd MyLibraryFrontend
npm ci
npm start
```

The Angular development server normally runs at `http://localhost:4200`. Configure the frontend/backend proxy as needed for your local environment.

**3. Optional ML recommendation service**

```bash
cd ml-recommendation-service
python -m venv .venv
pip install -r requirements.txt
uvicorn main:app --reload --port 8001
```

Point the backend at `http://localhost:8001` using `app.mlServiceUrl` in Spring configuration.

The machine-learning service exposes:

- `GET /health`
- `POST /recommend/books/similar`
- `POST /recommend/users/personalized`

Spring Boot integration endpoints and a more detailed setup walkthrough are documented in [ML_RECOMMENDATION_SETUP.md](ML_RECOMMENDATION_SETUP.md).

## Notes

- This is a portfolio/course project; no production deployment or public live demo is claimed.
- The ML recommendation approach relies on descriptive metadata and borrowing history. Recommendation quality depends on the completeness of book summaries.
- Configuration values must be supplied via environment variables in the current version.
