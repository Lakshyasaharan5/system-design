import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/*
    req - [rate limiter] - allow/not

    boolean isAllowed(request)

    FixedWindow
        |----1min----|
        capacity = 10
        10:01:02 : 10
        10:01:34 : 11
        10:02:00 : 1
        now - firstReqTime <= window
            cnt++
            allow
        firstReqTime = now
        cnt = 1

        {user.ip : [cnt, first req time]}


    sliding window
            l   r
        1 2 3 4 5
        11222
        curr.isBefore(now - window) poll
        if queue.size() == full
            return false
        queue.push(now)
        return true

    token bucket
        cap = 5
        [1 1 1 1 1] 1t/s
        bucket [lastTime, cnt=5]
        newTokens = (now - lastTime) * rate
        if (cnt + newTokens > cap) cnt = cap
        else cnt += newTokens
        if cnt == 0
            return false;
        cnt--;
        return true;


    leaky bucket
        cap = 5
        [1 1 1] rate 1t/s
        lastTime = 8
        now = 10
            remove tokens
            if full return false
            add tokens return true

 */
class User {
    String ip;
    public User(String ip) {this.ip = ip;}
}
interface Algo {
    boolean isAllowed(User user);
}
class LeakyBucket implements Algo {
    class Bucket {
        LocalDateTime lastReq;
        int cnt;
        public Bucket(int cnt) {
            this.cnt = cnt;
            lastReq = LocalDateTime.now();
        }
    }
    Map<String, Bucket> map;
    int cap;
    int rate; // draining rate tokens/seconds
    public LeakyBucket(int cap, int rate) {
        this.cap = cap;
        this.rate = rate;
        map = new ConcurrentHashMap<>();
    }
    public boolean isAllowed(User user) {
        Bucket bucket = map.compute(user.ip, (k, v) -> {
            if (v == null) v = new Bucket(0);
            return v;
        });
        synchronized (bucket) {
            LocalDateTime now = LocalDateTime.now();

            long elapsedSeconds =
                    Duration.between(bucket.lastReq, now).getSeconds();

            int remove = (int) (elapsedSeconds * rate);
            
            if (remove > 0) { 
                bucket.cnt = Math.max(0, bucket.cnt - remove);

                /*
                    if last = 8
                    now = 9.5
                    remove = 1.5 * 1 rate = 1.5 to int = 1
                    so we only used 1 second and should not add 0.5 second and preserve it
                 */
                bucket.lastReq =
                        bucket.lastReq.plusSeconds(elapsedSeconds);
            }

            if (bucket.cnt >= cap) {
                return false;
            }

            bucket.cnt++;
            return true;
        }
    }
}
class TokenBucket implements Algo {
    class Bucket {
        long lastReq;
        int cnt;
        public Bucket(int cnt) {
            this.cnt = cnt;
            lastReq = System.currentTimeMillis();
        }
    }
    Map<String, Bucket> map;
    int cap;
    int rate; // tokens/seconds
    public TokenBucket(int cap, int rate) {
        this.cap = cap;
        this.rate = rate;
        map = new ConcurrentHashMap<>();
    }
    public boolean isAllowed(User user) {
        Bucket bucket = map.compute(user.ip, (k, v) -> {
            if (v == null) v = new Bucket(cap);
            return v;
        });
        synchronized (bucket) {
            long now = System.currentTimeMillis();
            int newTokens = (int) (((now - bucket.lastReq) / 1000.0) * rate); // convert from millis to seconds
            if (newTokens + bucket.cnt > cap)
                bucket.cnt = cap;
            else
                bucket.cnt += newTokens;
            // issue here is like if 500ms is passed the newTokens will be 0 so it just resets the time and bucket
            // will never fill
            // solution is to add the passed milliseconds
            // or if interviewer allowed then use localdatetime seconds user Duration.between()
            bucket.lastReq = now;
            if (bucket.cnt == 0) return false;
            bucket.cnt--;
            return true;
        }
    }
}
class SlidingWindow implements Algo {
    Map<String, Queue<LocalDateTime>> userMap;
    int capacity;
    int window;
    public SlidingWindow(int c, int w) {
        userMap = new ConcurrentHashMap<>();
        capacity = c;
        window = w;
    }
    public boolean isAllowed(User user) {
        Queue<LocalDateTime> currUserQueue =
                userMap.computeIfAbsent(user.ip, ip -> new LinkedList<>());
        synchronized (currUserQueue) {
            LocalDateTime now = LocalDateTime.now();
            while (!currUserQueue.isEmpty()) {
                LocalDateTime front = currUserQueue.peek();
                if (front.isBefore(now.minusSeconds(window)))
                    currUserQueue.poll();
                else
                    break;
            }
            if (currUserQueue.size() == capacity) return false;
            currUserQueue.offer(now);
            return true;
        }
    }
}
class FixedWindow implements Algo {

    class UserInfo {
        int cnt;
        LocalDateTime firstReqTime;
        UserInfo(int cnt, LocalDateTime firstReqTime) {
            this.cnt = cnt;
            this.firstReqTime = firstReqTime;
        }
    }

    Map<String, UserInfo> userMap;
    int capacity = 0;
    int window = 1;
    public FixedWindow(int capacity, int window) {
        userMap = new ConcurrentHashMap<>();
        this.capacity = capacity;
        this.window = window;
    }
    public boolean isAllowed(User user) {
        synchronized (new Object()) {
            if (!userMap.containsKey(user.ip)) userMap.put(user.ip, new UserInfo(0, LocalDateTime.now()));
            UserInfo context = userMap.get(user.ip);
            LocalDateTime curr = LocalDateTime.now();
            if (curr.isAfter(context.firstReqTime.plusMinutes(window))) {   // now < first + window
                context.cnt = 1;
                context.firstReqTime = curr;
                return true;
            }
            if (context.cnt < capacity) {
                context.cnt++;
                return true;
            }
            return false;
        }
    }
}
class RateLimiter {
    Algo algo = new LeakyBucket(5, 1);
    public boolean isAllowed(User user) {
        return algo.isAllowed(user);
    }
}

public class Main {
    public static void main(String[] args) throws Exception {
        RateLimiter rl = new RateLimiter();
        User lakshya = new User("192.168.1.10");
        ExecutorService pool = Executors.newFixedThreadPool(10);
        for (int t = 0; t < 10; t++) {
            pool.submit(() -> {
                System.out.println(rl.isAllowed(lakshya));
            });
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

    }
}
