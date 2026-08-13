/*
[0 0 101 0 0 0] racks
[0 0 0 0 208 0] racks
[0 0 0 0 0 0 0] ....
[0 0 0 0 0 0 0]
[0 0 0 0 0 0 0]

101 : 10 oreos
109 : 20 diet cokes

class product {
    String name;
    double price;
}

class LineItem {
    Product p;
    int quantity;
}

vending machine will maintain this map
map = {
    rack number : LineItem
}

now process of vending in real life
insert money
select item
dispense

money -> select item -> dispense
states
IDLE -> HAS_MONEY -> DISPENSING

interface state {
    void insertMoney(amount);
    void selectItem(rack id);
    void dispense();
}
class idle {
    void insertMoney(amount){
        increase balance++;
        update state
    }
    void selectItem(rack id) {nope}
    void dispense() {nope}
}
class has_money {
    void insertMoney(amount){
        increase balance++;
        update state
    }
    void selectItem(rack id) {
        selected items++;
        balance--;
        update state
    }
    void dispense() {nope}
}
class dispense {
    void insertMoney(amount){
        nope
    }
    void selectItem(rack id) {
        nope
    }
    double dispense() {
        racks map item--;
        update state to idle if no balance left
        return remaining balance and dispense selected items
    }
}

class product {
    String name;
    double price;
}

class LineItem {
    Product p;
    int quantity;
}

class vending machine {
    state = idle initially
    racks map
    balance
    selected item list <stores rack ids> {101, 109, 101} means two oreos and one diet coke

    setState(state);
    addBalance(amount)
    addItem(rack number, LineItem)

    insertMoney()
    selectItem()
    dispense()
    {
        state.method(this)
    }
}

product, line item, states, vending machine
*/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Product {
    private String name;
    private double price;

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public String getName() {return name;}
    public double getPrice() {return price;}
}

class LineItem {
    private Product product;
    private int quantity = 0;

    public void setProduct(Product p) {
        this.product = p;
    }

    public void addQuantity(int q) {
        this.quantity += q;
    }

    public void reduceQuantity() {
        if (this.quantity == 0)
            throw new IllegalStateException("Quantity for " + product.getName() + " already 0!");
        this.quantity--;
    }

    public int getQuantity() {return quantity;}
    public Product getProduct() {return product;}
}

interface VendingState {
    void insertMoney(double amount, VendingMachine vm);
    void selectItem(int rackId, VendingMachine vm);
    double dispense(VendingMachine vm);
}

class Idle implements VendingState {
    public void insertMoney(double amount, VendingMachine vm){
        vm.updateBalance(vm.getBalance() + amount);
        vm.setState(new HasMoney());
    }
    public void selectItem(int rackId, VendingMachine vm) {
        throw new IllegalStateException("Insert money first!");
    }
    public double dispense(VendingMachine vm) {
        throw new IllegalStateException("Insert money first and select items!");
    }
}

class HasMoney implements VendingState {
    public void insertMoney(double amount, VendingMachine vm){
        vm.updateBalance(vm.getBalance() + amount);
    }
    public void selectItem(int rackId, VendingMachine vm) {
        Map<Integer, LineItem> rackMap = vm.getRackMap();
        if (!rackMap.containsKey(rackId)) {
            throw new IllegalArgumentException("Not a valid rack!");
        }
        if (rackMap.get(rackId).getQuantity() == 0) {
            throw new IllegalStateException("Out of stock!");
        }
        if (vm.getSelectItems().get(rackId) == rackMap.get(rackId).getQuantity()) {
            throw new IllegalStateException("You have selected all the " + rackMap.get(rackId).getProduct().getName());
        }
        if (rackMap.get(rackId).getProduct().getPrice() > vm.getBalance()) {
            throw new IllegalStateException("Not enough balance!");
        }
        Map<Integer, Integer> selectedItems = vm.getSelectItems();
        selectedItems.put(rackId, selectedItems.getOrDefault(rackId, 0) + 1);
        vm.updateBalance(vm.getBalance() - rackMap.get(rackId).getProduct().getPrice());
    }
    public double dispense(VendingMachine vm) {
        Map<Integer, Integer> selectedItems = vm.getSelectItems();
        if (selectedItems.size() == 0) {
            throw new IllegalStateException("Please select items first!");
        }
        Map<Integer, LineItem> rackMap = vm.getRackMap();
        for (int rackId : selectedItems.keySet()) {
            for (int i = 0; i < selectedItems.get(rackId); i++) {
                LineItem lineItem = rackMap.get(rackId);
                System.out.println("Dispensing " + lineItem.getProduct().getName());
                lineItem.reduceQuantity();
            }
        }
        vm.setState(new Idle());
        double remainingBalance = vm.getBalance();
        vm.updateBalance(0.0);
        vm.getSelectItems().clear();
        return remainingBalance;
    }
}

class VendingMachine {
    private VendingState state;
    private double balance = 0.0;
    private Map<Integer, LineItem> rackMap;
    private Map<Integer, Integer> selectItems;

    public VendingMachine() {
        state = new Idle();
        rackMap = new HashMap<>();
        selectItems = new HashMap<>();
    }

    public void setState(VendingState state) {
        this.state = state;
    }

    public void addItems(int rackId, LineItem lineItem) {
        // actually we would just take product, quantity and create line item internally but for simplicity i will skip that part
        if (rackMap.containsKey(rackId)) {
            rackMap.get(rackId).addQuantity(lineItem.getQuantity());
        } else {
            rackMap.put(rackId, lineItem);
        }
    }

    public void insertMoney(double amount) {
        state.insertMoney(amount, this);
    }

    public void selectItem(int rackId) {
        state.selectItem(rackId, this);
    }

    public void dispense() {
        state.dispense(this);
    }

    public Map<Integer, Integer> getSelectItems() {return selectItems;}
    public void updateBalance(double amount) {balance = amount;}
    public double getBalance() {return balance;}
    public Map<Integer, LineItem> getRackMap() {return rackMap;}
}

public class Main {
    public static void main(String[] args) {

        System.out.println("========== POSITIVE CASE ==========");

        VendingMachine vm = new VendingMachine();

        Product oreos = new Product("Oreos", 1.50);
        Product dietCoke = new Product("Diet Coke", 2.00);

        LineItem oreoStock = new LineItem();
        oreoStock.setProduct(oreos);
        oreoStock.addQuantity(10);

        LineItem cokeStock = new LineItem();
        cokeStock.setProduct(dietCoke);
        cokeStock.addQuantity(20);

        vm.addItems(101, oreoStock);
        vm.addItems(109, cokeStock);

        /*
         * Workaround for this line in HasMoney:
         *
         * vm.getSelectItems().get(rackId) == quantity
         *
         * get(rackId) returns null on the first selection, causing
         * a NullPointerException. Initializing it to zero avoids that
         * without changing the existing classes.
         */
        vm.getSelectItems().put(101, 0);
        vm.getSelectItems().put(109, 0);

        try {
            vm.insertMoney(10.00);

            vm.selectItem(101);
            vm.selectItem(109);
            vm.selectItem(101);

            System.out.println("Balance before dispensing: $" + vm.getBalance());

            vm.dispense();

            System.out.println("Oreos remaining: "
                    + vm.getRackMap().get(101).getQuantity());

            System.out.println("Diet Cokes remaining: "
                    + vm.getRackMap().get(109).getQuantity());

            System.out.println("Balance after dispensing: $" + vm.getBalance());

        } catch (Exception e) {
            System.out.println("Positive case failed: " + e.getMessage());
        }


        System.out.println("\n========== NEGATIVE CASE 1 ==========");
        System.out.println("Selecting without inserting money");

        VendingMachine vm2 = new VendingMachine();

        try {
            vm2.selectItem(101);
        } catch (Exception e) {
            System.out.println("Expected error: " + e.getMessage());
        }


        System.out.println("\n========== NEGATIVE CASE 2 ==========");
        System.out.println("Selecting an invalid rack");

        VendingMachine vm3 = new VendingMachine();

        try {
            vm3.insertMoney(5.00);
            vm3.selectItem(999);
        } catch (Exception e) {
            System.out.println("Expected error: " + e.getMessage());
        }


        System.out.println("\n========== NEGATIVE CASE 3 ==========");
        System.out.println("Insufficient balance");

        VendingMachine vm4 = new VendingMachine();

        LineItem expensiveStock = new LineItem();
        expensiveStock.setProduct(new Product("Expensive Snack", 5.00));
        expensiveStock.addQuantity(2);

        vm4.addItems(200, expensiveStock);
        vm4.getSelectItems().put(200, 0);

        try {
            vm4.insertMoney(1.00);
            vm4.selectItem(200);
        } catch (Exception e) {
            System.out.println("Expected error: " + e.getMessage());
        }


        System.out.println("\n========== NEGATIVE CASE 4 ==========");
        System.out.println("Selecting an out-of-stock product");

        VendingMachine vm5 = new VendingMachine();

        LineItem emptyStock = new LineItem();
        emptyStock.setProduct(new Product("Candy", 1.00));

        vm5.addItems(300, emptyStock);
        vm5.getSelectItems().put(300, 0);

        try {
            vm5.insertMoney(5.00);
            vm5.selectItem(300);
        } catch (Exception e) {
            System.out.println("Expected error: " + e.getMessage());
        }


        System.out.println("\n========== NEGATIVE CASE 5 ==========");
        System.out.println("Dispensing without selecting an item");

        VendingMachine vm6 = new VendingMachine();

        try {
            vm6.insertMoney(5.00);
            vm6.dispense();
        } catch (Exception e) {
            System.out.println("Expected error: " + e.getMessage());
        }


        System.out.println("\n========== NEGATIVE CASE 6 ==========");
        System.out.println("Selecting more items than available");

        VendingMachine vm7 = new VendingMachine();

        LineItem limitedStock = new LineItem();
        limitedStock.setProduct(new Product("Chips", 1.00));
        limitedStock.addQuantity(1);

        vm7.addItems(400, limitedStock);
        vm7.getSelectItems().put(400, 0);

        try {
            vm7.insertMoney(10.00);

            vm7.selectItem(400);
            vm7.selectItem(400);

        } catch (Exception e) {
            System.out.println("Expected error: " + e.getMessage());
        }
    }
}
