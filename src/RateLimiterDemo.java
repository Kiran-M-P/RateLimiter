import strategies.FixedWindowStrategy;
import strategies.RateLimitingStrategy;
import strategies.TokenBucketStrategy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RateLimiterDemo
{
    public static void main(String[] args)
    {
        String userId1 = "user123";

        System.out.println("=== Fixed Window Demo ===");
        runFixedWindowDemo(userId1);

        String userId2 = "user456";
        System.out.println("\n === Token Bucket Demo ===");
        runTokenBucketDemo(userId2);
    }


    private static void runFixedWindowDemo(String userId)
    {
        int maxRequest = 5;
        int windowSizeInSeconds = 10;

        RateLimitingStrategy fixedWindowStrategy = new FixedWindowStrategy(maxRequest, windowSizeInSeconds);
        RateLimiterService service = RateLimiterService.getInstance();
        service.setRateLimitingStrategy(fixedWindowStrategy);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int i = 0; i < 10; i++)
        {
            executor.submit(() -> service.handleRequest(userId));
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        executor.shutdown();
    }

    private static void runTokenBucketDemo(String userId)
    {
        int capacity = 5;
        int refillRatePerSecond = 1;

        RateLimitingStrategy tokenBucketStrategy = new TokenBucketStrategy(capacity, refillRatePerSecond);
        RateLimiterService service = RateLimiterService.getInstance();
        service.setRateLimitingStrategy(tokenBucketStrategy);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 10; i++)
        {
            executor.submit(() -> service.handleRequest(userId));

            try
            {
                Thread.sleep(300);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        executor.shutdown();
    }
}
