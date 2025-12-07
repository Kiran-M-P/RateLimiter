import strategies.RateLimitingStrategy;

public class RateLimiterService
{
    private final static RateLimiterService INSTANCE = new RateLimiterService();
    private RateLimitingStrategy rateLimitingStrategy;

    private RateLimiterService()
    {

    }

    public static RateLimiterService getInstance()
    {
        return INSTANCE;
    }


    public void setRateLimitingStrategy(RateLimitingStrategy rateLimitingStrategy)
    {
        this.rateLimitingStrategy = rateLimitingStrategy;
    }


    public void handleRequest(String userId)
    {
        if (rateLimitingStrategy.canAllowRequest(userId))
        {
            System.out.println("Request from user " + userId + " is allowed");
        }
        else
        {
            System.out.println("Request from user " + userId + " is rejected: Rate limit exceeded");
        }
    }


}
