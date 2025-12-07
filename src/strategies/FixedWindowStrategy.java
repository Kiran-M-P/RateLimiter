package strategies;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowStrategy implements RateLimitingStrategy
{
    private final int maxRequest;
    private final long windowSizeInMillis;
    private final Map<String, UserRequestInfo> userRequestMap = new ConcurrentHashMap<>();

    public FixedWindowStrategy(int maxRequest, long windowSizeInSeconds)
    {
        this.maxRequest = maxRequest;
        this.windowSizeInMillis = windowSizeInSeconds * 1000;
    }


    @Override
    public boolean canAllowRequest(String userId)
    {
        long currentTimeInMills = System.currentTimeMillis();
        UserRequestInfo userRequestInfo = userRequestMap.computeIfAbsent(userId, k -> new UserRequestInfo(currentTimeInMills));

        synchronized (userRequestInfo)
        {
            if (currentTimeInMills - userRequestInfo.windowStartTimeInMills >= windowSizeInMillis)
            {
                userRequestInfo.reset(currentTimeInMills);
            }

            if (userRequestInfo.requestCount.get() < maxRequest)
            {
                userRequestInfo.requestCount.incrementAndGet();

                return true;
            }

            return false;
        }
    }


    private static class UserRequestInfo
    {
        long windowStartTimeInMills;
        AtomicInteger requestCount;

        UserRequestInfo(long windowStartTimeInMills)
        {
            this.windowStartTimeInMills = windowStartTimeInMills;
            this.requestCount = new AtomicInteger(0);
        }

        void reset(long newWindowStartTimeInMills)
        {
            this.windowStartTimeInMills = newWindowStartTimeInMills;
            this.requestCount.set(0);
        }

    }
}
