import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/*

per user/ip

clients hitting the server ------ [rate limiter] -------- service

interface Strategy {
    boolean isAllowed(User user);
}
classes token bucket, leaky bucket, sliding window etc.

class RateLimiter {
    Strategy
    User map
    boolean rateLimit(User user) {
        return strategy.get(type).isAllowed(user);
    }
}

strategy 1: Fixed Window
start window
cnt = 0
capacity
reset when curr hit is > start + size
    start = curr
    cnt = 0
if cnt < capacity:
    cnt++
    return true
return false

strategy 2: Sliding Window
maintain queue of request times
then on each hit
    first remove all the old entries from the queue which are out of window
    push new entry to queue
    check the queue size if its within maxRequest or not

strategy 3: Token Bucket
int bucket fills at steady rate
int capacity = 4
start = 1s
rate = 2 tokens/second
prevRequestTime

now when request comes:
    refill (curr - prevReqestTime) * rate
    if bucket > 0
        bucket--
        return true
    return false

strategy 4: Leaky Bucket
int bucket with capacity
decrements at fixed rate

leak rate = 2 req/sec
cap = 5

now when request comes:
    remove (curr - last request time) * rate tokens
    add to bucket
    if full:
        return false
    return true


classes and interfaces
User, Strategy, window, bucket, RateLimiter
 */
class User {
    final private int userId;
    final private String ip;
    public User(int id, String ip) {
        this.userId = id;
        this.ip = ip;
    }
    public int getId() {return userId;}
    public String getIp() {return ip;}
}
interface Strategy {
    boolean allow(User user);
}
class FixedWindow implements Strategy {
    private static class UserInfo {
        long start;
        int requests;
        UserInfo (long start) {
            this.start = start;
        }
    }
    private ConcurrentHashMap<User, UserInfo> map;
    private int capacity;
    private long windowSize;    // milliseconds
    public FixedWindow(int capacity, long windowSize) {
        this.capacity = capacity;
        this.windowSize = windowSize;
        map = new ConcurrentHashMap<>();
    }
    @Override
    public boolean allow(User user) {
        long currTime = System.currentTimeMillis();
        UserInfo info = map.compute(user, (key, value) -> {
            if (value == null) {
                return new UserInfo(currTime);
            }
            return value;
        });
        UserInfo userInfo = map.get(user);
        synchronized (user) {
            if (currTime > userInfo.start + windowSize) {
                userInfo.start = currTime;
                userInfo.requests = 0;
            }
            if (userInfo.requests < capacity) {
                userInfo.requests++;
                return true;
            }
            return false;
        }
    }
}
class RateLimiter {
    private Strategy strategy;
    public RateLimiter() {}
    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }
    public boolean allow(User user) {
        return strategy.allow(user);
    }
}

public class Main {
    public static void main(String[] args) {
        RateLimiter rl = new RateLimiter();
        rl.setStrategy(new FixedWindow(5, 5000));
        User lakshya = new User(1, "192.168.1.10");
        User jason = new User(2, "192.168.1.15");
        System.out.println(rl.allow(lakshya));
        System.out.println(rl.allow(lakshya));
        System.out.println(rl.allow(lakshya));
        System.out.println(rl.allow(lakshya));
        System.out.println(rl.allow(lakshya));
        System.out.println(rl.allow(lakshya));
        System.out.println(rl.allow(lakshya));
    }
}
