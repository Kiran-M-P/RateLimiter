package strategies;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenBucketStrategy implements RateLimitingStrategy
{
    private final int capacity;
    private final int refillRatePerSecond;
    private final Map<String, TokenBucket> userBuckets = new ConcurrentHashMap<>();


    public TokenBucketStrategy(int capacity, int refillRatePerSecond)
    {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    @Override
    public boolean canAllowRequest(String userId)
    {
        long currentTimeInMills = System.currentTimeMillis();
        TokenBucket bucket = userBuckets.computeIfAbsent(userId,
                k -> new TokenBucket(capacity, refillRatePerSecond, currentTimeInMills));

        synchronized (bucket)
        {
            bucket.refill(currentTimeInMills);

            if (bucket.tokens > 0)
            {
                bucket.tokens--;
                return true;
            }

            return false;
        }
    }


    private static class TokenBucket
    {
        int tokens;
        final int capacity;
        final int refillRatePerSecond;
        long lastRefillTimeInMills;

        public TokenBucket(int capacity, int refillRatePerSecond, long lastRefillTimeInMills)
        {
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
            this.lastRefillTimeInMills = lastRefillTimeInMills;
        }

        public void refill(long currentTimeInMills)
        {
            long elapsedTimeInMills = currentTimeInMills - lastRefillTimeInMills;
            int tokensToAdd = (int) ((elapsedTimeInMills / 1000) * refillRatePerSecond);

            if (tokensToAdd > 0)
            {
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTimeInMills = currentTimeInMills;
            }

        }



    }
}
