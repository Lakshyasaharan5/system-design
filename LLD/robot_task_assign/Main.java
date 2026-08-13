import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
/*
tasks comes, service will put it into queue
service takes a task
finds robot using strategy
assigns tasks

Robots -> {  [] [] [] [] []  }

Robot {IDLE/BUSY, location, speed(slow, fast), task}

service {
    assigning strategy (nearest, speed)
    task queue = [task1, t2, t3...]
    robot map {id : robot}

    robotId assign(task) {
        pick a robot using strategy
        if (robot == null) add task to queue
        robot.start(task)
        return robotId;
    }

    void finishTask(robotId) {
        free the robot
        if tasks in queue:
            assign(task) // i am not going to do the job of assigning because that would mean
                        // return the robot id and everything which is assign method's job
    }
}
              /-------->interface strategy (nearest, capable)
status -> robot ---> service
task ----/
 */
class Task {
    private final UUID taskId;
    private final String task;    // for example "bring box2 from dock21"
    public Task(String task) {
        this.taskId = UUID.randomUUID();
        this.task = task;
    }
    public UUID getTaskId() {return taskId;}
    public String getTask() {return task;}
}
enum RobotStatus {
    IDLE, BUSY
}
class Robot {
    private int robotId;
    private Task task;
    private RobotStatus robotStatus = RobotStatus.IDLE;
    private int location;  // simulating geo distance from service sensor
    public Robot(int robotId) {
        this.robotId = robotId;
    }
    public void startTask(Task task) {
        this.task = task;
        this.robotStatus = RobotStatus.BUSY;
    }
    public void finishTask() {
        this.robotStatus = RobotStatus.IDLE;
        this.location = 100; // in prod this will be the location after robot goes idle
    }
    public int getRobotId() {return robotId;}
    public RobotStatus getStatus() {return robotStatus;}
}
interface RobotSelectionStrategy {
    Robot selectRobot(List<Robot> idleRobots);
}
class RobotService {
    private RobotSelectionStrategy strategy;
    private Queue<Task> queue;
    Map<Integer, Robot> robotMap;
    public RobotService() {
        queue = new ConcurrentLinkedQueue<>();
        robotMap = new ConcurrentHashMap<>();
    }
    public void setStrategy(RobotSelectionStrategy strategy) {
        this.strategy = strategy;
    }
    public Integer assign(Task task) {
        synchronized (this) {
            List<Robot> idle = idleRobots();
            Robot robot = strategy.selectRobot(idle);
            if (robot == null) { queue.add(task); return null; }  // no one free -> queue
            robot.startTask(task);
            return robot.getRobotId();
        }
    }

    public void finishTask(int robotId) {
        synchronized (this) {
            Robot robot = robotMap.get(robotId);
            robot.finishTask();                    // free FIRST (back in idle pool)
            if (!queue.isEmpty()) {
                assign(queue.poll());              // reuse; freed robot is now eligible
            }
        }
    }

    private List<Robot> idleRobots() {
        List<Robot> idle = new ArrayList<>();
        for (Robot r : robotMap.values())
            if (r.getStatus() == RobotStatus.IDLE) idle.add(r);
        return idle;
    }
}
public class Main {
    public static void main(String[] args) {

    }
}
