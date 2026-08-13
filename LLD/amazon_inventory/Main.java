import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
An inventory management system tracks product stock across multiple warehouse locations.
When inventory arrives, the system records it. When orders ship, the system deducts stock.
The system can also transfer inventory between locations and alert managers when stock runs low.

     inventory1  =  [p1, p2, p3......]
     inventory2  =  [p1, _, p3......]
     inventory3  =  [p1, p2, p3......]

     service {i1, i2, i3}

     Item (id, desc)

     OrderItem {id, quantity}  // shirt,10    shoes2,25,    shoes1,50

     I1{
        shoe2 : 50,
        shoe1 : 100
     }
     I2{
        shoe2 : 50,
        shoe1 : 100
     }
     Inventory{
        Map<id, quantity>,
        meta details,
        {
            shoe2 : 50,
            shoe1 : 100
        }
        location
        threshold map {
            shoe2 : 50,
            shoe1 : 100
        }
        alert(){map.size()}
        orderArrived(List<Item>)
        List<OrderItem> orderShip(List<OrderItem>)

     }
     Service {
        Map<id, Inventory>
        order(int inventory id, List<OrderItem>){
            ship from there
        }
        transfer(Inventory from, to, List<item id>) {
            list items  = from.orderShip(id)
            to.orderArrived(items)
        }
     }

     Item -> Inventory -> Service
      \----> OrderItem
 */
class Item {
    private UUID itemId;
    private String desc;
    public Item(String desc) {
        this.itemId = UUID.randomUUID();
        this.desc = desc;
    }
    public UUID getItemId() {return itemId;}
    public String getDesc() {return desc;}
}
class OrderItem {
    private Item item;
    private int quantity;
    public OrderItem(Item item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }
    public Item getItem() {return item;}
    public int getQuantity() {return quantity;}
}
class Inventory {
    private int id;
    private Map<Integer, Item> itemMap;
    private Map<Integer, Item> itemThresholdMap;
    public Inventory(int id) {
        this.id = id;
        itemMap = new HashMap<>();
        itemThresholdMap = new HashMap<>();
    }
    public int getId() {return id;}
    public void updateThreshold(List<OrderItem> orderItemList) {
        // TODO: fill the threshold map
    }
    public void orderArrived(List<OrderItem> orderItemList) {
        // TODO: fill the item map
    }
    List<Item> orderShip(List<OrderItem> orderItemList) {
        // scan the map
        // pick up the items
        // alert if threshold crossed
        // return list items
        return null;
    }
}
class InventoryService {
    private Map<Integer, Inventory> inventoryMap;
    public InventoryService() {
        inventoryMap = new HashMap<>();
    }
    public void addInventory(Inventory newInventory) {
            if (inventoryMap.containsKey(newInventory.getId()))
                throw new IllegalArgumentException("Inventory already exists");
            inventoryMap.put(newInventory.getId(), newInventory);
    }
    public void order(int id, List<OrderItem> orderItemList){
        Inventory inventory = inventoryMap.get(id);

    }
}

public class Main {
    public static void main(String[] args) {

    }
}
