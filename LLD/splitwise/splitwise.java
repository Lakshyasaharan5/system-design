/*
A, B, C

A pays $60 for hotel
B owes A $20
C owes A $20

C pays $30 for food later
A owes C $10
B owes C $10

Users = [A, B, C]

ledger map = {
    A : {B:0, C:10}
    B : {A:20, C:10}
    C : {A:20, B:0}
}

void expense(payer, amount, participants) {
    update the map
}

void settle(from, to, amount) {
    update the map
}

double balance(person) {
    scan the map and check
}
**************************************
User {
    private String name;
    just keeping it simple as of now; will add other fields later
}
participants = [A,B,C]
amount = 100

Equal
details = null
100/3
decimal to first one

Exact
details = {A:60,B:20,C:20}
A: 60
B: 20
C: 20

Percent
details = {A:60%,B:20%,C:20%}
A: 60% of 100
B: 20% 0f 100
C: 20% of 100

interface SplitStrategy {
    map computeShares(amount, participants, details){}
} split types will implement it

Splitwise service {
    ledger map
    split strategy map

    void expense (payer, totalAmount, split type, participants, details) {
        split strategy = map(type)
        curr shares map = strategy(totalAmount, participants, details)
        update the ledger map accordingly based on payer and who owes whom
    }

    void settle(from, to, amount) {
        update the ledger map accordingly
    }

    double balance(user) {
        create dynamically from ledger map
    }

    debtSimplifiction() {
        compute every user's net balance (one number each: total owed-to minus owes);
        creditors and debtors form two sides; greedily match largest debtor to largest creditor,
        record that payment, repeat. Result: everyone's net settles in ≤ n−1 transactions,
        and the pairwise history is discarded — which is why real Splitwise makes it opt-in
        (people want to know who they owe). Name "net balances + greedy matching" and you've answered it.
    }
}
 */

import java.util.*;

class User {
    private UUID id;
    private String name;
    public User (String name) {
        id = UUID.randomUUID();
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public UUID getId() {
        return id;
    }
}

enum SplitType {
    EQUAL, EXACT, PERCENT
}

interface SplitStrategy {
    Map<User, Double> computeShares(double amount, List<User> participants, Map<User, Double> details);
}

class EqualStrategy implements SplitStrategy {
    @Override
    public Map<User, Double> computeShares(double amount, List<User> participants, Map<User, Double> details) {
        return null;
    }
}

class ExactStrategy implements SplitStrategy {
    @Override
    public Map<User, Double> computeShares(double amount, List<User> participants, Map<User, Double> details) {
        //sum == amount on doubles can fail for valid input (0.1+0.2 problem)
        //so use Math.abs(sum - amount) > 0.01
        return null;
    }
}

class PercentStrategy implements SplitStrategy {
    @Override
    public Map<User, Double> computeShares(double amount, List<User> participants, Map<User, Double> details) {
        Map<User, Double> shares = new HashMap<>();
        for (User user : participants) {
            shares.put(user, amount * details.get(user) / 100);
        }
        return shares;
    }
}

class SplitWise {
    Map<User, Map<User, Double>> ledger;
    Map<SplitType, SplitStrategy> splitStrategyMap;
    // TODO: List<Expense> later for command pattern

    public SplitWise() {
        ledger = new HashMap<>();
        splitStrategyMap = new HashMap<>();
    }

    public void addStrategy (SplitType type, SplitStrategy splitStrategy) {
        splitStrategyMap.put(type, splitStrategy);
    }

    public void addUser(User user) {
        ledger.put(user, new HashMap<>());
    }

    public void expense(User payer, double amount, SplitType type, List<User> participants, Map<User, Double> details) {
        Map<User, Double> shares = splitStrategyMap.get(type).computeShares(amount, participants, details);
        // update ledger map using shares
        /*
        A:10
        B:20
        C:10 payer
        A : {payer: 10}
        B : {payer: 98 + 20}
        {
            A: {}
            B: {}
            C: {}
        }
        */
        for (User borrower : shares.keySet()) {
            if (!ledger.containsKey(borrower)) {
                ledger.put(borrower, new HashMap<>());
            }
            if (borrower.getId().equals(payer.getId())) continue;
            Map<User, Double> currShares = ledger.get(borrower);
            if (!currShares.containsKey(payer))
                currShares.put(payer, 0.0);
            currShares.compute(payer, (k, v) -> v + shares.get(borrower));
        }
    }

    public void settle(User from, User to, double amount) {

    }

    public double balance(User user) {
        return 0.0;
    }
}

public class Main {
    public static void main(String[] args) {

    }
}

