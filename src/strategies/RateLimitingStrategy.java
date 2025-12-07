package strategies;

public interface RateLimitingStrategy
{
    boolean canAllowRequest(String userId);
}
