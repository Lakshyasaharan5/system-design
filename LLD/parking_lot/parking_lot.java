/*
Parking Lot

parking lot building
-----floor
-------spot
----------type

 [
 floor 3
 floor 2
 floor 1 List<spot>  {L S M L L S S} id doesn't have to be fixed, for flexibility it can be any order based on initialization
 ]

 enum type {
 small, medium, large
 }
 if we wanna extend types so just add new enum type

 class vehicle {
    enum type
    string number plate
 }
 vehicle classes can implement it [IS-A relationship]

 class spot {
    spot id
    isOccupied
    vehicle (HAS-A)
 }

 class floor {
    List<spots>
    will be initialized through empty constructor but the spots will be added using addSpot function
    addSpot(Spot){
        list.append(spot)
    }
 }

 class ticket {
    ticket id
    spot
    entry time
    exit time (will be set during unpark)
 }

interface PaymentStrategy {
    double calculateFees(ticket)
}
different payment strategies can implement it

class ParkingLotService {
    Map<String, Spot> maps id to spot
    Map<Type, Integer> freq count of available spots
    also add vehicle type to payment strategy map
    List<floors>
    will be initialized through empty constructor but the floors will be added using addFloor function

    addFloor(Floor){
        list.append(Floor)
        // scan and update spot id map and freq spots availability map
    }

    int availability(type) {
        naive version
        I can scan all the floors and check the free spots for each type

        maybe better
        keep track of free spots from the start using something like freq hashmap of each type
    }

    ticket park(vehicle) {
        handle availability (throw exception)
        find empty spot and park the vehicle based on its type
        update freq map
        create and return ticket
    }

    double unpark(ticket) {
        free the parking spot
        update freq map
        return payment fee calculated using strategy
    }
 }
 */

import java.time.LocalDateTime;
import java.util.*;

enum VehicleType {
    SMALL, MEDIUM, LARGE
}

class Vehicle {
    private final VehicleType type;
    private final String numberPlate;

    public Vehicle (VehicleType type, String numberPlate) {
        this.type = type;
        this.numberPlate = numberPlate;
    }

    public VehicleType getType() {
        return type;
    }

    public String getNumberPlate() {
        return numberPlate;
    }
}

class Spot {
    private String spotId;
    private Vehicle vehicle;
    private boolean occupied = false;
    private final VehicleType vehicleType;

    public Spot(String spotId, VehicleType vehicleType) {
        this.spotId = spotId;
        this.vehicleType = vehicleType;
    }

    public String getSpotId() {
        return spotId;
    }

    public void occupy(Vehicle vehicle) {
        this.vehicle = vehicle;
        occupied = true;
    }

    public void vacate() {
        this.vehicle = null;
        occupied = false;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public VehicleType getType() {
        return vehicleType;
    }
}

class Floor {
    private List<Spot> spotList;

    public Floor() {
        spotList = new ArrayList<>();
    }

    public void addSpot(Spot spot) {
        spotList.add(spot);
    }

    public List<Spot> getSpotList() {
        return spotList;
    }
}

class Ticket {
    private final String ticketId;
    private final Spot spot;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;

    public Ticket(Spot spot) {
        ticketId = UUID.randomUUID().toString();
        this.spot = spot;
        entryTime = LocalDateTime.now();
    }

    public void setExitTime(LocalDateTime exitTime) {
        // TODO: later add checks if exitTime is valid
        this.exitTime = exitTime;
    }

    public String getTicketId() {
        return ticketId;
    }

    public Spot getSpot() {
        return spot;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }
}

interface PaymentStrategy {
    double calculateFee(Ticket ticket);
}

class SmallTypePayment implements PaymentStrategy {
    @Override
    public double calculateFee(Ticket ticket) {
        // assuming hourly rate is $10
        // skipping the calculation logic for now
        return 10.0;
    }
}

class LargeTypePayment implements PaymentStrategy {
    @Override
    public double calculateFee(Ticket ticket) {
        // assuming hourly rate is $40
        // skipping the calculation logic for now
        return 40.0;
    }
}

class ParkingLotService {
    private List<Floor> floors;
    private Map<String, Spot> spotMap;
    private Map<VehicleType, Integer> availabilityMap;
    private Map<String, Ticket> ticketMap; // it's like db to store the tickets
    private Map<VehicleType, PaymentStrategy> paymentStrategyMap;

    public ParkingLotService() {
        floors = new ArrayList<>();
        spotMap = new HashMap<>();
        availabilityMap = new HashMap<>();
        ticketMap = new HashMap<>();
        paymentStrategyMap = new HashMap<>();
    }

    public void addPaymentStrategy(VehicleType type, PaymentStrategy ps) {
        paymentStrategyMap.put(type, ps);
    }

    public void addFloor(Floor floor) {
        floors.add(floor);
        for (Floor currFloor : floors) {
            for (Spot spot : currFloor.getSpotList()) {
                if (spot.isOccupied()) continue;
                availabilityMap.compute(spot.getType(), (k, v) -> v == null ? 1 : v + 1);
                spotMap.computeIfAbsent(spot.getSpotId(), (k) -> spot);
            }
        }
    }

    public Ticket park(Vehicle vehicle) {
        VehicleType currType = vehicle.getType();
        if (!availabilityMap.containsKey(currType) || availabilityMap.get(currType) == 0) {
            throw new IllegalStateException("No free spot available");
        }

        Spot currSpot = null;
        for (Floor floor : floors) {
            for (Spot spot : floor.getSpotList()) {
                if (spot.getType() == currType) {
                    spot.occupy(vehicle);
                    currSpot = spot;
                }
            }
        }

        availabilityMap.compute(currType, (k, v) -> {
            if (v == null || v == 0) {
                throw new IllegalStateException("No availability for " + currType);
            }
            return v - 1;
        });

        Ticket ticket = new Ticket(currSpot);
        ticketMap.put(ticket.getTicketId(), ticket);
        return ticket;
    }

    public double unpark(Ticket ticket) {
        if (!ticketMap.containsKey(ticket.getTicketId())) {
            throw new IllegalArgumentException("Not a valid ticket");
        }

        availabilityMap.compute(ticket.getSpot().getType(), (k, v) -> {
            return v - 1;
        });

        Spot currSpot = ticket.getSpot();
        currSpot.vacate();
        ticket.setExitTime(LocalDateTime.now());

        PaymentStrategy ps = paymentStrategyMap.get(currSpot.getType());
        double fee = ps.calculateFee(ticket);
        return fee;
    }
}

public class Main {
    public static void main(String[] args) {
        //set up our parking lot
        ParkingLotService service = new ParkingLotService();

        Floor firstFloor = new Floor();
        Spot smallSpot = new Spot("F1-SM", VehicleType.SMALL);
        firstFloor.addSpot(smallSpot);

        Floor secondFloor = new Floor();
        Spot largeSpot = new Spot("F2-LG", VehicleType.LARGE);
        secondFloor.addSpot(largeSpot);

        service.addFloor(firstFloor);
        service.addFloor(secondFloor);

        //add payment strategies
        service.addPaymentStrategy(VehicleType.SMALL, new SmallTypePayment());
        service.addPaymentStrategy(VehicleType.LARGE, new LargeTypePayment());

        //vehicles
        Vehicle bike = new Vehicle(VehicleType.SMALL, "NJ-123");
        Vehicle car = new Vehicle(VehicleType.MEDIUM, "CA-999"); // it shouldn't be able to  park
        Vehicle truck = new Vehicle(VehicleType.LARGE, "NY-556");

        //park
        Ticket bikeTicket = service.park(bike);
        Ticket truckTicket = service.park(truck);
        try {
            Ticket carTicket = service.park(car);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        //unpark
        double bikeFee = service.unpark(bikeTicket);
        double truckFee = service.unpark(truckTicket);

        System.out.println("Bike fee: " + bikeFee);
        System.out.println("Truck fee: " + truckFee);
    }
}

