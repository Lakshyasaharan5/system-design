import java.util.*;

/*
A transaction comes in (amount, user, location, timestamp...).
Evaluate it against a set of risk rules — e.g. amount over a threshold, too many transactions
in a short window (velocity), unusual location. Each rule returns a risk signal;
the engine aggregates them into a decision: ALLOW / REVIEW / BLOCK.
Rules must be easy to add/remove without touching the engine.


rules = [amount threshold, location, velocity, identification]
decision = block/review/allow
engine:
    rules
    boolean verify(transaction):
        go through rules
        pick most restricted decision
*/
enum Decision {
    ALLOW, REVIEW, BLOCK
}
class Transaction {
    private long id;
    private double amount;
    private String location; //coordinates
    public Transaction(long id, double amount, String location) {
        this.id = id;
        this.amount = amount;
        this.location = location;
    }
    public double getAmount() {return amount;}
}
interface Rule {
    Decision verify(Transaction transaction);
}
class AmountThresholdRule implements Rule {
    double blockThreshold = 10000.00;
    double reviewRange = 500.00;
    public Decision verify(Transaction transaction) {
        if (transaction.getAmount() >= blockThreshold) {
            return Decision.BLOCK;
        }
        if (Math.abs(transaction.getAmount() - blockThreshold) <= reviewRange) {
            return Decision.REVIEW;
        }
        return Decision.ALLOW;
    }
}
class LocationRule implements Rule {
    public Decision verify(Transaction transaction) {
        return Decision.ALLOW;
    }
}
class Engine {
    List<Rule> rules;
    public Engine() {
        rules = new ArrayList<>();
    }
    public void addRule(Rule rule) {
        this.rules.add(rule);
    }
    public Decision verify(Transaction transaction) {

        Decision mostRestrictive = Decision.ALLOW;
        for (Rule rule : rules) {
            Decision curr = rule.verify(transaction);
            if (curr.ordinal() > mostRestrictive.ordinal()) {
                mostRestrictive = curr;
            }
        }

        return mostRestrictive;
    }
}
public class Main {
    public static void main(String[] args) {
        Engine engine = new Engine();
        engine.addRule(new AmountThresholdRule());
        engine.addRule(new LocationRule());
        Decision decision = engine.verify(new Transaction(1, 950000.00, "20.10.2.1"));
        System.out.println(decision);
    }
}
