import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/*
        [s] [s] [s] [s]
        [m] [m] [m] [m]
        [l] [l] [l] [l]
        [l] {-----} [l]
        [l] [l] [l] [l]
        [l] [l] [l] [l]

        Package(item details, address etc.) -> type
        Service -> Locker(isFree) -> type

        map {lockerId : Locker} // for locker look up
        map {barcode : Locker} // for currently occupied lockers

        void assign(package) {
            scan and find first free locker of package type or expired one lazily
            mark it locked
            generate barcode and add expiry time
            email details to the customer

            as for the concurrency, we can make it atomic using locker object,
            but we can't lock until we found the locker so make search+locking both atomic using synchronized(this)
        }

        void pickup(barcode) {
            get the locker from map
            open the gate, customer picks up and closes it
            mark the locker free
            remove the barcode key from map
        }

        Package(item details, address etc.) -> type
        Service -> Locker(isFree) -> type
 */
enum SizeType {
    SMALL, MEDIUM, LARGE
}
class Item {
    private final UUID itemId;
    private final String address;
    private SizeType size;
    public Item(String address, SizeType size) {
        itemId = UUID.randomUUID();
        this.address = address;
        this.size = size;
    }
    public UUID getPackageId() {return itemId;}
    public String getAddress() {return address;}
    public SizeType getSize() {return size;}
}
class Locker {
    private final int lockerId;
    private final SizeType size;
    private boolean isOccupied = false;
    private LocalDateTime expiryTime;
    private Item item;
    public Locker(int id, SizeType size) {
        this.lockerId = id;
        this.size = size;
    }
    public int getLockerId() {return lockerId;}
    public SizeType getSize() {return size;}
    public boolean isOccupied() {return isOccupied;}
    public LocalDateTime getExpiryTime() {return expiryTime;}
    public Item getItem() {return item;}
    public void placePackage(Item item) {
        this.item = item;
        this.isOccupied = true;
        this.expiryTime = LocalDateTime.now().plusDays(2);
    }
    public Item vacate(){
        this.isOccupied = false;
        return item;
    }
    public void vacateIfExpired() {
        if (expiryTime != null && LocalDateTime.now().isAfter(expiryTime)) {
            expiryTime = null;
            vacate();
        }
    }
}
class AmazonLocker {
    private Map<Integer, Locker> lockerMap;
    private ConcurrentHashMap<UUID, Locker> assignedLockersMap;
    public AmazonLocker() {
        lockerMap = new HashMap<>();
        assignedLockersMap = new ConcurrentHashMap<>();
    }
    public void addLocker(Locker newLocker) {
        lockerMap.put(newLocker.getLockerId(), newLocker);
    }
    public UUID assign(Item item) {
        Locker assignedLocker = null;
        synchronized (this) {
            for (Locker locker : lockerMap.values()) {
                locker.vacateIfExpired();
                if (locker.getSize().equals(item.getSize()) && !locker.isOccupied()) {
                    locker.placePackage(item);
                    assignedLocker = locker;
                    break;
                }
            }
        }
        if (assignedLocker == null) {
            throw new IllegalStateException("No locker free");
        }
        UUID barcode = UUID.randomUUID();
        assignedLockersMap.put(barcode, assignedLocker);
        // trigger email as well
        return barcode;
    }
    public Item pickup(UUID barcode) {
        if (!assignedLockersMap.containsKey(barcode)) {
            throw new IllegalArgumentException("Wrong barcode");
        }
        Locker assignedLocker = assignedLockersMap.get(barcode);
        Item item = assignedLocker.vacate();
        assignedLockersMap.remove(barcode);
        return item;
    }
}
public class Main {
    public static void main(String[] args) {
        Item shoes = new Item("250 central ave, newark, nj", SizeType.MEDIUM);
        Item shirt = new Item("250 central ave, newark, nj", SizeType.MEDIUM);

        AmazonLocker service = new AmazonLocker();
        service.addLocker(new Locker(1, SizeType.MEDIUM));

        UUID barcode = service.assign(shoes);
        try {
            service.assign(shirt); // should fail due to available locker
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        Item pickedUp = service.pickup(barcode);
        System.out.println(pickedUp.getPackageId()==shoes.getPackageId()); // match same item
    }
}
