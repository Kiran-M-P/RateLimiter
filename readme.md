# RateLimiter LLD


## 1. Requirements

---

### 1.1 Functional Requirements
- Support rate limiting on a per-user basis.
- Enforce a fixed number of allowed requests (e.g., 100) within a defined time window (e.g., 60 seconds).
- Reject requests that exceed the allowed limit and return an appropriate response.
- Provide a simple way to simulate requests in a demo or main method.

### 1.2 Non-Functional Requirements
- **Thread-Safety:** 
  - The rate limiter must handle concurrent access from multiple threads without race conditions.

- **Modularity:** 
  - The system should follow object-oriented design principles with clear separation of concerns.

- **Extensibility:** 
  - The design should be flexible enough to support other rate limiting strategies like sliding window or token bucket.

- **Maintainability:** 
  - The codebase should be clean, testable, and easy to extend or debug.

- **Performance:** 
  - The implementation should efficiently support high-frequency request patterns using optimal data structures.


## 2. Core Entities

- Rate limiting algorithms
  - Token bucket
  - Fixed window counter
  - Sliding log



