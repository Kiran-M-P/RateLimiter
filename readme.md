# Design an In-Memory Rate Limiter (LLD)

This repository contains the **low-level design and implementation** of an **in-memory rate limiter** in Java.

A **rate limiter** is a system component used to **control the rate of operations** (API calls, logins, messages, etc.) performed by a user or client over a given duration. It is widely used to:

- Throttle API requests
- Restrict login attempts
- Limit messaging/notifications
- Protect systems from abuse or DoS-style traffic :contentReference[oaicite:0]{index=0}

This project focuses on the **LLD thought process** as much as the code itself.

---

## 1. Clarifying Requirements

Before jumping into classes and code, we clarify *what exactly we’re designing*.

### 1.1 Problem Statement

> Design an in-memory rate limiter that restricts the number of allowed requests per client within a specified time window. :contentReference[oaicite:1]{index=1}

### 1.2 Discussion & Assumptions

During an LLD interview, you would ask questions like:

- **Per what entity are we limiting?**  
  → Per *user* (identified by user ID / token).
- **Which algorithms should we support?**  
  → Start with **Fixed Window** and **Token Bucket**. Sliding Window can be added later.
- **Are limits same for all users?**  
  → Yes, assume global rule like **100 requests / 60 seconds**.
- **What happens when the limit is exceeded?**  
  → Request should be **rejected with a clear response** that limit is exceeded.
- **Do we need concurrency handling?**  
  → Yes, implementation should be **thread-safe**.

### 1.3 Functional Requirements

- Rate limiting should be **per user**.
- Support a configurable rule:
  - e.g. `N` requests per `T` seconds (100 req / 60s).
- If the limit is breached:
  - Reject the request.
  - Indicate that the user is rate-limited.
- Provide a simple **demo / main** method to simulate requests. :contentReference[oaicite:2]{index=2}

### 1.4 Non-Functional Requirements

- **Thread-Safety**: safe under concurrent requests.
- **Modularity**: clear separation of concerns.
- **Extensibility**: easy to plug in new algorithms (like Sliding Window Log).
- **Maintainability**: clean, testable, readable code.
- **Performance**: efficient for high-frequency requests using optimal data structures. :contentReference[oaicite:3]{index=3}

---

## 2. Identifying Core Entities

There is no single “best” rate-limiting algorithm; each has trade-offs. This is a great fit for the **Strategy Design Pattern**. :contentReference[oaicite:4]{index=4}

We define a **`RateLimitingStrategy`** interface and concrete implementations for each algorithm. The main `RateLimiterService` delegates to whichever strategy is configured.

### 2.1 Algorithms Considered

- **Token Bucket**
  - A bucket has a fixed capacity of tokens.
  - Tokens are refilled at a constant rate.
  - Each request consumes one token.
  - If empty → request rejected.

- **Fixed Window Counter**
  - Count requests in the current time window (e.g. this minute).
  - Reset count at the start of a new window.
  - Weakness: edge bursts (e.g. many at 00:59 + many at 01:00).

- *(Extensible)* **Sliding Window Log**
  - Keeps timestamps of recent requests and drops old ones.
  - Accurate but can be memory-heavy.

### 2.2 Key Entities / Classes

- **`RateLimiterService`** (Facade / Context / Singleton)
  - Main entry point for clients.
  - Holds the current `RateLimitingStrategy`.

- **`RateLimitingStrategy`** (Interface)
  - Method like: `boolean allowRequest(String userId);`

- **`FixedWindowStrategy`** (Concrete Strategy)
  - Maintains per-user request count in a fixed time window.

- **`TokenBucketStrategy`** (Concrete Strategy)
  - Maintains a per-user token bucket.

- **`Rule`**
  - Configuration: max requests, window size, refill rate, etc.

- **`UserRequestInfo`** (inner data class)
  - Used by fixed window: `windowStart`, `requestCount`.

- **`TokenBucket`** (inner data class)
  - Used by token bucket: `capacity`, `tokens`, `refillRatePerSecond`, `lastRefillTimestamp`. :contentReference[oaicite:5]{index=5}

---

## 3. Designing Classes and Relationships

This section shows how the system is structured and how objects interact.

### 3.1 Data Classes

- `UserRequestInfo`
  - Tracks:
    - `windowStart` (epoch time for start of current window)
    - `requestCount` (AtomicInteger for thread safety)
- `TokenBucket`
  - Tracks:
    - `capacity`
    - `tokens`
    - `refillRatePerSecond`
    - `lastRefillTimestamp` :contentReference[oaicite:6]{index=6}

### 3.2 Core Classes

- `RateLimitingStrategy` (interface)
  ```java
  interface RateLimitingStrategy {
      boolean allowRequest(String userId);
  }
* `FixedWindowStrategy` (implements `RateLimitingStrategy`)

  * Uses a `ConcurrentHashMap<String, UserRequestInfo>` per user.
  * For each request:

    * Check if current time is still in the same window.
    * If yes, increment count and compare to limit.
    * If no, reset window start and count.

* `TokenBucketStrategy` (implements `RateLimitingStrategy`)

  * Uses a `ConcurrentHashMap<String, TokenBucket>`.
  * On each request:

    * Refill tokens based on time since `lastRefillTimestamp`.
    * If `tokens > 0`, decrement and allow.
    * Else, reject.

* `RateLimiterService` (Singleton + Facade)

  * Holds a `RateLimitingStrategy`.
  * API method like:

    ```java
    public boolean handleRequest(String userId) {
        return strategy.allowRequest(userId);
    }
    ```
  * Client code only depends on this, not on individual strategies.

### 3.3 Relationships

* **Composition**

  * `RateLimiterService` “has-a” `RateLimitingStrategy`.
  * `FixedWindowStrategy` “has-a” map of `UserRequestInfo`.
  * `TokenBucketStrategy` “has-a” map of `TokenBucket`.

* **Inheritance**

  * `FixedWindowStrategy` and `TokenBucketStrategy` both implement `RateLimitingStrategy`.

* **Dependency**

  * Client (`RateLimiterDemo` or controller) → depends on `RateLimiterService`.
  * `RateLimiterService` → depends on `RateLimitingStrategy` interface, not concrete classes.

### 3.4 Key Design Patterns

* **Strategy Pattern**

  * Allows runtime choice of algorithm.
  * New strategies (e.g. Sliding Window) can be added without changing the service.

* **Singleton Pattern**

  * `RateLimiterService` is a singleton: single source of truth for rate limiting in the app.

* **Facade Pattern**

  * `RateLimiterService` hides complexity and exposes a simple `handleRequest(userId)` API.

---

## 4. Implementation Overview

> Note: See source files in this repo (`strategy`, `service`, `demo` packages) for full implementation.

### 4.1 Strategy Interface

```java
public interface RateLimitingStrategy {
    boolean allowRequest(String userId);
}
```

### 4.2 Example: Fixed Window Strategy (High-Level Logic)

```java
public class FixedWindowStrategy implements RateLimitingStrategy {

    private final long windowSizeInMillis;
    private final int maxRequests;
    private final ConcurrentHashMap<String, UserRequestInfo> userRequestMap = new ConcurrentHashMap<>();

    @Override
    public boolean allowRequest(String userId) {
        long currentTime = System.currentTimeMillis();

        UserRequestInfo info = userRequestMap.computeIfAbsent(
                userId,
                id -> new UserRequestInfo(currentTime, new AtomicInteger(0))
        );

        synchronized (info) {
            if (currentTime - info.windowStart >= windowSizeInMillis) {
                info.windowStart = currentTime;
                info.requestCount.set(0);
            }

            if (info.requestCount.incrementAndGet() <= maxRequests) {
                return true;    // allowed
            } else {
                return false;   // rate limited
            }
        }
    }

    private static class UserRequestInfo {
        long windowStart;
        AtomicInteger requestCount;

        UserRequestInfo(long windowStart, AtomicInteger requestCount) {
            this.windowStart = windowStart;
            this.requestCount = requestCount;
        }
    }
}
```

*(Exact code in the repo may differ slightly, but follows this design.)*

### 4.3 Example: RateLimiterService (Facade + Singleton)

```java
public class RateLimiterService {

    private static final RateLimiterService INSTANCE = new RateLimiterService();

    private final RateLimitingStrategy strategy;

    private RateLimiterService() {
        // choose strategy here: FixedWindowStrategy or TokenBucketStrategy
        this.strategy = new FixedWindowStrategy(/* config */);
    }

    public static RateLimiterService getInstance() {
        return INSTANCE;
    }

    public boolean handleRequest(String userId) {
        return strategy.allowRequest(userId);
    }
}
```

---

## 5. How to Run

### 5.1 Prerequisites

* Java 8+
* Maven or Gradle (if using a build tool), or plain `javac`/`java`.

### 5.2 Clone the Repository

```bash
git clone https://github.com/Kiran-M-P/RateLimiter.git
cd RateLimiter
```

## 5. How to Approach This in an Interview

1. Start with **clarifying requirements** (Section 1).
2. Discuss **algorithms and trade-offs** (Section 2).
3. Move into **entities, patterns, and class design** (Section 3).
4. Finally, sketch **key methods and concurrency concerns** (Section 4).

This repo is structured to reflect that exact flow so you can **explain your thought process**, not just show code.

---

## 6. Possible Extensions

* Add **Sliding Window Log** or **Sliding Window Counter** strategy.
* Add **different rules per user / plan**.
* Expose this as a **REST API**.
* Plug into a **distributed cache** (Redis) for multi-node rate limiting.

---