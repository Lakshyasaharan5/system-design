/*
chain of responsibility and singleton
 */
enum LogLevel { DEBUG, INFO, ERROR }   // ordinal: DEBUG=0 < INFO=1 < ERROR=2

abstract class LogHandler {
    protected LogLevel level;
    protected LogHandler next;          // the "next link" — this is what makes it a chain

    public LogHandler(LogLevel level) { this.level = level; }

    public LogHandler setNext(LogHandler next) {   // returns next to allow chaining
        this.next = next;
        return next;
    }

    public void log(LogLevel msgLevel, String message) {
        if (msgLevel.ordinal() >= this.level.ordinal()) {   // is this handler responsible?
            write(message);
        }
        if (next != null) next.log(msgLevel, message);       // pass down the chain
    }

    protected abstract void write(String message);           // each handler writes differently
}

class ConsoleHandler extends LogHandler {
    public ConsoleHandler(LogLevel level) { super(level); }
    protected void write(String message) { System.out.println("[Console] " + message); }
}
class FileHandler extends LogHandler {
    public FileHandler(LogLevel level) { super(level); }
    protected void write(String message) { System.out.println("[File] " + message); /* real: write to file */ }
}
class ErrorHandler extends LogHandler {
    public ErrorHandler(LogLevel level) { super(level); }
    protected void write(String message) { System.out.println("[ALERT] " + message); }
}

class Logger {
    private static Logger instance;              // the single instance
    private final LogHandler chain;

    private Logger() {                            // private constructor — nobody else can `new` it
        // build the chain once: console logs everything, file logs INFO+, alert logs ERROR only
        LogHandler console = new ConsoleHandler(LogLevel.DEBUG);
        console.setNext(new FileHandler(LogLevel.INFO))
                .setNext(new ErrorHandler(LogLevel.ERROR));
        this.chain = console;
    }

    public static synchronized Logger getInstance() {   // thread-safe lazy init
        if (instance == null) instance = new Logger();
        return instance;
    }

    public void log(LogLevel level, String message) { chain.log(level, message); }
    // convenience:
    public void debug(String m) { log(LogLevel.DEBUG, m); }
    public void info(String m)  { log(LogLevel.INFO, m); }
    public void error(String m) { log(LogLevel.ERROR, m); }
}

public class Main {
    public static void main(String[] args) {
        Logger log = Logger.getInstance();
        log.debug("connecting to db");      // only Console fires (DEBUG < INFO, < ERROR)
        log.info("user logged in");         // Console + File fire
        log.error("payment failed");        // Console + File + ALERT all fire
    }
}

