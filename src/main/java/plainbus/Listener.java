package plainbus;

public interface Listener<Event> {

    void onEvent(Event event);

    static void reject() {
        throw new Rejection();
    }

    class Rejection extends RuntimeException {
        @Override public Throwable fillInStackTrace() {
            return null;
        }
    }
}
