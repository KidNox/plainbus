package plainbus;

public interface ConnectionHandler {

    void onStartListen(Object eventType);

    void onEndListen(Object eventType);

    void onEventRejected(Object type, Object event, Class listenerClass);

    class Stub implements ConnectionHandler {

        @Override public void onStartListen(Object eventType) { }

        @Override public void onEndListen(Object eventType) { }

        @Override public void onEventRejected(Object type, Object event, Class listenerClass) { }
    }
}
