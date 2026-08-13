import java.util.*;

enum Direction { UP, DOWN, IDLE }

class Elevator {
    private int currentFloor;
    private Direction direction;

    // Core structures: stops above the car served on up-sweeps, below on down-sweeps.
    private final TreeSet<Integer> upStops = new TreeSet<>();
    private final TreeSet<Integer> downStops = new TreeSet<>();

    public Elevator(int startFloor) {
        this.currentFloor = startFloor;
        this.direction = Direction.IDLE;
    }

    // Cab call: passenger inside pressed a destination.
    public void requestDestination(int floor) { addStop(floor); }

    // Hall call: passenger on `floor` wants to go `dir`.
    // v0 single car: only the floor matters (where to stop). `dir` matters for
    // multi-car dispatch (v1), kept in the signature so v1 needs no API change.
    public void requestPickup(int floor, Direction dir) { addStop(floor); }

    private void addStop(int floor) {
        if (floor == currentFloor) return;
        if (floor > currentFloor) upStops.add(floor);
        else downStops.add(floor);
        if (direction == Direction.IDLE) {
            direction = (floor > currentFloor) ? Direction.UP : Direction.DOWN;
        }
    }

    public boolean hasWork() { return !upStops.isEmpty() || !downStops.isEmpty(); }

    public void step() {
        if (!hasWork()) { direction = Direction.IDLE; return; }

        Integer target = nextTarget();
        if (target == null) {            // nothing ahead in this direction -> flip
            flipDirection();
            target = nextTarget();
            if (target == null) { direction = Direction.IDLE; return; }
        }

        // Move ONE floor toward the target (toward target, not blindly by direction:
        // this makes "travel up to fetch a high floor while committed to a down-sweep"
        // work without any special case).
        if (target > currentFloor) currentFloor++;
        else currentFloor--;

        // Serve the stop if this floor was requested (from either set).
        boolean served = upStops.remove(currentFloor);
        served = downStops.remove(currentFloor) | served;
        if (served) System.out.println("  -> stopped at floor " + currentFloor);

        // Sweep exhausted but work remains on the other side -> flip.
        if (nextTarget() == null) {
            if (hasWork()) flipDirection();
            else direction = Direction.IDLE;
        }
    }

    // Next floor to serve in the current sweep, or null if this side is done.
    private Integer nextTarget() {
        if (direction == Direction.UP)   return upStops.ceiling(currentFloor + 1);
        if (direction == Direction.DOWN) return downStops.floor(currentFloor - 1);
        return null;
    }

    private void flipDirection() {
        direction = (direction == Direction.UP) ? Direction.DOWN : Direction.UP;
    }

    public int getCurrentFloor() { return currentFloor; }
    public Direction getDirection() { return direction; }
}

public class Main {
    public static void main(String[] args) {
        Elevator e = new Elevator(5);

        // Car at 5; passengers inside heading to 8 and 10.
        e.requestDestination(8);
        e.requestDestination(10);
        // Mid-sweep hall calls: UP from 3 (behind), DOWN from 12 (above).
        e.requestPickup(3, Direction.UP);
        e.requestPickup(12, Direction.DOWN);

        int tick = 0;
        while (e.hasWork() && tick < 40) {
            tick++;
            e.step();
            System.out.println("tick " + tick + ": floor " + e.getCurrentFloor()
                    + " dir " + e.getDirection());
        }
        System.out.println(e.hasWork() ? "STILL PENDING (bug!)" : "All served in " + tick + " ticks.");
    }
}
