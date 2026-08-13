import java.time.LocalDateTime;
import java.util.*;

/*
taking the simplest case
let me walk the app as an user

open the app
I will see many shows
select one show
choose the timing
choose the seat
pay the price
I will get my ticket with seat confirmed and time(let's not worry about screens yet)

alright so
Movie {details}
Show {Movie, seat map local to a show}
enum SeatStatus {AVAILABLE, HELD, BOOKED}
Seat {number, price, SeatStatus}
Ticket {show details, timing, seat number, price references etc.}

this map will be inside the app service and we have method to add shows
{
Movie : list<Show>
...
}

booking process:
select the movie first
Skyfall let's say
select the show now
show2 8pm-10pm
select the seat now
seat will be held now
confirm(show, seat, payment, user) returns ticket

about holding the seat
keep a separate object
Hold { holdId, seats, user, show, expiresAt }

BookingService {
    Map<String,Hold> activeHolds
    hold(show, seatNumbers, user) -> Hold       // all-or-nothing, marks seats HELD
    confirm(holdId, payment) -> Ticket          // HELD -> BOOKED, checks not expired
    expireHold(holdId)                          // HELD -> AVAILABLE, removes hold (v0: call manually)
}

alright so here are the classes and interfaces and enums
user, seat status, seat, movie, show, ticket, hold, booking service
 */
class User {
    private int id;
    private String name;
    public User(int id, String name) {
        this.id = id;
        this.name = name;
    }
    public int getId() {return id;}
    public String getName() {return name;}
}

enum SeatStatus {AVAILABLE, HELD, BOOKED}

class Seat {
    private final String seatId;
    private SeatStatus status = SeatStatus.AVAILABLE;
    private final double price;
    public Seat(String seatId, double price) {
        this.seatId = seatId;
        this.price = price;
    }
    public void updateStatus(SeatStatus status) {
        this.status = status;
    }
    public String getSeatId() {return seatId;}
    public SeatStatus getStatus() {return status;}
    public double getPrice() {return price;}
}

class Ticket {
    private UUID ticketId;
    private List<Seat> bookedSeats;
    private Show show;
    public Ticket(){}
}

class Movie {
    private int movieId;
    private String name;
    // skipping rest of the fields for simplicity
    public Movie(int movieId, String name) {
        this.movieId = movieId;
        this.name = name;
    }
    public int getMovieId() {return movieId;}
    public String getName() {return name;}
}

class Show {
    private Movie movie;
    private Map<String, Seat> seatMap;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    public Show(Movie movie, LocalDateTime startTime, LocalDateTime endTime) {
        this.movie = movie;
        seatMap = new HashMap<>();
        this.startTime = startTime;
        this.endTime = endTime;
    }
    public void addSeat(Seat seat) {
        seatMap.put(seat.getSeatId(), seat);
    }
    public Movie getMovie() {return movie;}
    public Map<String, Seat> getSeatMap() {return seatMap;}
    public LocalDateTime getStartTime() {return startTime;}
    public LocalDateTime getEndTime() {return endTime;}
}

// ticket

class Hold {
    private UUID holdId;
    private Show show;
    private List<Seat> seats;
    private User user;
    private LocalDateTime expiresAt;
    public Hold(Show show, List<Seat> selectedSeats, User user) {
        holdId = UUID.randomUUID();
        this.show = show;
        this.seats = selectedSeats;
        this.user = user;
        expiresAt = LocalDateTime.now().plusMinutes(5);
    }
    public UUID getHoldId() {return holdId;}
    public Show getShow() {return show;}
    public List<Seat> getSeats() {return seats;}
    public User getUser() {return user;}
    public LocalDateTime getExpiresAt() {return expiresAt;}
}

class BookingService {
    private Map<Integer, List<Show>> showMap;
    private List<Movie> movieList;
    private Map<UUID, Hold> holdMap;
    public BookingService() {
        showMap = new HashMap<>();
        movieList = new ArrayList<>();
        holdMap = new HashMap<>();
    }
    public void addMovie(Movie movie) {
        this.movieList.add(movie);
    }
    public void addShow(int movieId, Show show) {
        showMap.compute(movieId, (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.add(show);
            return v;
        });
    }
    // i am skipping showing movie list and then showing show list
    public Hold hold(Show show, List<String> seatIds, User user) {
        // verify if the ALL seats are actually available or not
        // this part will require thread safety
        synchronized (show) {
            Map<String, Seat> seatMap = show.getSeatMap();
            for (String id : seatIds) {
                if (seatMap.get(id).getStatus() != SeatStatus.AVAILABLE) {
                    throw new IllegalStateException("Seat not available!");
                }
            }
            // now hold them
            List<Seat> heldSeats = new ArrayList<>();
            for (String id : seatIds) {
                Seat currSeat = seatMap.get(id);
                currSeat.updateStatus(SeatStatus.HELD);
                heldSeats.add(currSeat);
            }
            Hold hold = new Hold(show, heldSeats, user);
            holdMap.put(hold.getHoldId(), hold);
            return hold;
        }
    }

    public Ticket confirm(Hold hold, double paymentAmount) {
        if (hold.getExpiresAt().isBefore(LocalDateTime.now())) {
            releaseHold(hold.getHoldId());
            throw new IllegalStateException("hold expired, please try again");
        }
        for (Seat s : hold.getSeats()) {
            s.updateStatus(SeatStatus.BOOKED);
        }
        holdMap.remove(hold.getHoldId());
         return new Ticket(); // later
    }

    public void releaseHold(UUID holdId) {          // unconditional
        Hold hold = holdMap.get(holdId);
        synchronized (hold.getShow()) {
            if (hold == null) return;                    // already gone
            for (Seat s : hold.getSeats()) s.updateStatus(SeatStatus.AVAILABLE);
            holdMap.remove(holdId);
        }
    }

    public void expireHold(UUID holdId) {
        // goes through the list of seats and marks status AVAILABLE
        // also deletes entry from hold map
        Hold hold = holdMap.get(holdId);
        synchronized (hold.getShow()) {
            if (hold.getExpiresAt().isAfter(LocalDateTime.now())) return;
            for (Seat s : hold.getSeats()) {
                s.updateStatus(SeatStatus.AVAILABLE);
            }
            holdMap.remove(holdId);
        }
    }
 }

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // ---- setup ----
        Movie skyfall = new Movie(1, "Skyfall");
        Show show = new Show(skyfall,
                LocalDateTime.of(2026, 8, 12, 9, 0),
                LocalDateTime.of(2026, 8, 12, 11, 0));
        show.addSeat(new Seat("A1", 100));   // ONE seat, ten people want it

        BookingService service = new BookingService();
        service.addMovie(skyfall);
        service.addShow(skyfall.getMovieId(), show);

        // ---- create ten threads, each a different user chasing A1 ----
        List<Thread> threads = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            User u = new User(i, "User" + i);
            Thread t = new Thread(new BookingTask(service, show, "A1", u));
            threads.add(t);
        }

        // ---- start them all (they now race) ----
        for (Thread t : threads) {
            t.start();
        }

        // ---- wait for all to finish ----
        for (Thread t : threads) {
            t.join();
        }

        // ---- verify: A1 must be HELD by exactly one, and only one GOT line above ----
        System.out.println("Final status of A1: " + show.getSeatMap().get("A1").getStatus());
    }

}

class BookingTask implements Runnable {
    private final BookingService service;
    private final Show show;
    private final String seatId;
    private final User user;

    public BookingTask(BookingService service, Show show, String seatId, User user) {
        this.service = service;
        this.show = show;
        this.seatId = seatId;
        this.user = user;
    }

    @Override
    public void run() {
        // this is what ONE thread does
        try {
            service.hold(show, new ArrayList<>(Arrays.asList(seatId)), user);
            System.out.println(user.getName() + " GOT the seat");
        } catch (Exception e) {
            System.out.println(user.getName() + " FAILED: " + e.getMessage());
        }
    }
}
