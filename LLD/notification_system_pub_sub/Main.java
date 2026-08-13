import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*

        publisher {
            map <topic : queue> // later for v2
            map <topic : set<subscriber>>
            subscribe(sub, topic)
            unsubscribe(sub, topic)
            publish(topic, message)
        }

        subscriber {
            onMessage(message)
        }
 */
class Message {
    private int id;
    private String content;
    public Message(int id, String content) {
        this.id = id;
        this.content = content;
    }
    public int getId() {return id;}
    public String getContent() {return content;}
}
class Topic {
    private String name;
    public Topic(String name) {this.name = name;}
    public String getName() {return name;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (((Topic)o).equals(this.name)) return true;
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
interface Subscriber {
    void onMessage(Message message);
}
class OrderPostprocessor implements Subscriber {    // after order is completed it will notify customer and update the status in db
    @Override
    public void onMessage(Message message) {
        System.out.println("Order is completed, sending email with order details: " + message.getContent());
    }
}
class OrderPreprocessor implements Subscriber {     // when new order is created it will log and update the status in db
    @Override
    public void onMessage(Message message) {
        System.out.println("Logging new order details: " + message.getContent());
    }
}
interface Publisher {
    void subscribe(Subscriber sub, Topic topic);
    void unsubscribe(Subscriber sub, Topic topic);
    void publish(Message message, Topic topic);
}
class OrderService implements Publisher {
    // 1. Concurrent map for the topic->subscribers structure
    private final Map<Topic, Set<Subscriber>> topicToSubscriberMap;

    // 2. subscribe: atomic get-or-create + thread-safe set
    public void subscribe(Subscriber sub, Topic topic) {
        topicToSubscriberMap.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet()).add(sub);
    }

    private final ExecutorService deliveryPool = Executors.newFixedThreadPool(4);

    public OrderService() {
        topicToSubscriberMap = new ConcurrentHashMap<>();
    }


    @Override
    public void unsubscribe(Subscriber sub, Topic topic) {
        Set<Subscriber> subscribers = topicToSubscriberMap.get(topic);
        if (subscribers == null || !subscribers.contains(sub)) {
            throw new IllegalArgumentException(sub + " subscriber was not subscribed to topic " + topic);
        }
        subscribers.remove(sub);
    }

    @Override
    public void publish(Message message, Topic topic) {
        Set<Subscriber> subscribers = topicToSubscriberMap.getOrDefault(topic, Set.of());
        for (Subscriber sub : subscribers) {
            // instead of sub.onMessage(message) directly...
            deliveryPool.submit(() -> sub.onMessage(message));   // hand it to the pool
        }
    }
}
public class Main {
    public static void main(String[] args) {
        Topic orderCreated = new Topic("order-created");
        Topic orderCompleted = new Topic("order-completed");
        Subscriber orderPostProcessor = new OrderPostprocessor();
        Subscriber orderPreprocessor = new OrderPreprocessor();
        Publisher orderService = new OrderService();
        orderService.subscribe(orderPreprocessor, orderCreated);
        orderService.subscribe(orderPostProcessor, orderCompleted);
        orderService.publish(new Message(1, "Order 1889 has been created"), orderCreated);
        orderService.publish(new Message(2, "Order 1790 has been completed"), orderCompleted);
    }
}

