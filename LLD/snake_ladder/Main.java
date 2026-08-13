import java.util.*;

class Player {
    String name;
    int position = 0;

    public Player(String name) {
        this.name = name;
    }
}

class Dice {
    public int roll() {
        return 1 + (int)(Math.random() * 6);
    }
}

class Board {
    int size = 100;

    // key = start, value = end
    Map<Integer, Integer> jumps = new HashMap<>();

    public Board() {
        // ladders
        jumps.put(5, 25);
        jumps.put(20, 60);

        // snakes
        jumps.put(70, 40);
        jumps.put(99, 10);
    }

    public int getFinalPosition(int position) {
        if (jumps.containsKey(position)) {
            return jumps.get(position);
        }

        return position;
    }
}

class Game {
    Board board;
    Dice dice;
    Player p1;
    Player p2;

    public Game(Player p1, Player p2) {
        board = new Board();
        dice = new Dice();

        this.p1 = p1;
        this.p2 = p2;
    }

    public void startGame() {

        Player curr = p1;

        while (true) {

            int diceValue = dice.roll();

            System.out.println(
                    curr.name + " rolled " + diceValue
            );

            int nextPosition = curr.position + diceValue;

            // move only if <= 100
            if (nextPosition <= board.size) {

                nextPosition = board.getFinalPosition(nextPosition);

                curr.position = nextPosition;
            }

            System.out.println(
                    curr.name + " is at " + curr.position
            );

            // winner
            if (curr.position == board.size) {
                System.out.println(curr.name + " WON!");
                break;
            }

            // switch player
            curr = (curr == p1) ? p2 : p1;
        }
    }
}

public class Main {
    public static void main(String[] args) {

        Player p1 = new Player("Alice");
        Player p2 = new Player("Bob");

        Game game = new Game(p1, p2);

        game.startGame();
    }
}
