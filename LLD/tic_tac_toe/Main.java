/*
           0 1 2
         0 x x x
         1 x o _
         2 _ _ o

         Game
            --> board --> cell --> X/O
            --> player 1,2


         player1: 0,1 X
         player2: 2,2 O

         Cell[][] = new Cell[2][2]
         Cell.value = X/O

         currentPlayer = p1
            loop:
                get row, col from currentPlayer

                if move is invalid:
                    ask SAME player again

                if move is valid:
                    check if currentPlayer won

                    if won:
                        end game

                    otherwise:
                        switch currentPlayer
 */
import java.util.Scanner;

class Cell {
    char ch = '_';
}

class Board {
    Cell[][] cells;
    int n;

    public Board(int n) {
        this.n = n;
        cells = new Cell[n][n];

        // Important: create each Cell object
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                cells[i][j] = new Cell();
            }
        }
    }

    public boolean makeMove(int row, int col, char ch) {

        // invalid index
        if (row < 0 || col < 0 || row >= n || col >= n) {
            return false;
        }

        // already occupied
        if (cells[row][col].ch != '_') {
            return false;
        }

        cells[row][col].ch = ch;
        return true;
    }

    public boolean checkIfWon(int row, int col, char ch) {

        // check row
        boolean won = true;

        for (int j = 0; j < n; j++) {
            if (cells[row][j].ch != ch) {
                won = false;
                break;
            }
        }

        if (won) return true;


        // check column
        won = true;

        for (int i = 0; i < n; i++) {
            if (cells[i][col].ch != ch) {
                won = false;
                break;
            }
        }

        if (won) return true;


        // check main diagonal
        // only needed if row == col
        if (row == col) {
            won = true;

            for (int i = 0; i < n; i++) {
                if (cells[i][i].ch != ch) {
                    won = false;
                    break;
                }
            }

            if (won) return true;
        }


        // check opposite diagonal
        // positions satisfy row + col == n - 1
        if (row + col == n - 1) {
            won = true;

            for (int i = 0; i < n; i++) {
                if (cells[i][n - 1 - i].ch != ch) {
                    won = false;
                    break;
                }
            }

            if (won) return true;
        }

        return false;
    }

    public void printBoard() {
        for (int i = 0; i < n; i++) {

            for (int j = 0; j < n; j++) {
                System.out.print(cells[i][j].ch + " ");
            }

            System.out.println();
        }
    }
}


class Player {
    int id;
    String name;
    char ch;

    public Player(int id, String name, char ch) {
        this.id = id;
        this.name = name;
        this.ch = ch;
    }
}


class Game {
    Board board;
    Player p1;
    Player p2;

    Scanner scanner = new Scanner(System.in);

    public Game(int n, Player p1, Player p2) {
        board = new Board(n);
        this.p1 = p1;
        this.p2 = p2;
    }

    public void startGame() {

        Player curr = p1;

        while (true) {

            board.printBoard();

            System.out.println(
                    curr.name + " (" + curr.ch + "), enter row and col:"
            );

            int row = scanner.nextInt();
            int col = scanner.nextInt();

            // try move
            boolean validMove = board.makeMove(row, col, curr.ch);

            if (!validMove) {
                System.out.println("Invalid move. Try again.");
                continue;
            }

            // check winner
            if (board.checkIfWon(row, col, curr.ch)) {

                board.printBoard();

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

        Player p1 = new Player(1, "Alice", 'X');
        Player p2 = new Player(2, "Bob", 'O');

        Game game = new Game(3, p1, p2);

        game.startGame();
    }
}
