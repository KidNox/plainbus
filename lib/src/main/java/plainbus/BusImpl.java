package plainbus;

import java.util.HashMap;
import java.util.Map;

public class BusImpl implements Bus {

    private final Map<Object, Listener> eventTypeToListenerMap = new HashMap<>();
    private final Map<Object, Connection> connectionsMap;
    private final Connection.ConnectionFactory connectionFactory;
    private final ConnectionHandler connectionHandler;

    BusImpl(ConnectionHandler connectionHandler,
            Connection.ConnectionFactory connectionFactory,
            Map<Object, Connection> connectionsMap) {
        this.connectionHandler = connectionHandler;
        this.connectionFactory = connectionFactory;
        this.connectionsMap = connectionsMap;
    }

    @Override public synchronized Connection connect(Object context) {
        Connection connection = connectionsMap.get(context);
        if (connection == null) {
            connection = connectionFactory.createConnection(this);
            connectionsMap.put(context, connection);
        }
        return connection;
    }

    @Override public synchronized void disconnect(Object context) {
        try {
            connectionsMap.remove(context).close();
        } catch (NullPointerException ex) {
            throw new IllegalStateException(context + " is not connected");
        }
    }

    @Override public boolean post(Object event) {
        Class type = event.getClass();
        if (type == Object.class) throw new IllegalArgumentException("unsupported type: java.lang.Object");
        boolean delivered = false;
        do {
            delivered = post(type, event) || delivered;
            type = type.getSuperclass();
        } while (type != Object.class);
        return delivered;
    }

    @Override public boolean post(Object type, Object event) {
        Listener listener = eventTypeToListenerMap.get(type);
        if (listener != null) {
            try {
                //noinspection unchecked
                listener.onEvent(event);
                return true;
            } catch (Listener.Rejection ignored) {
                connectionHandler.onEventRejected(type, event, listener.getClass());
            }
        }
        return false;
    }

    synchronized void addListener(Object type, Listener listener) {
        Listener current = eventTypeToListenerMap.put(type, listener);
        if (current != null) {
            eventTypeToListenerMap.put(type,
                    Connection.CompoundListener.wrap(type, current, listener, connectionHandler));
        } else {
            connectionHandler.onStartListen(type);
        }
    }

    synchronized void removeListener(Object type, Listener listener) {
        Listener typeListener = eventTypeToListenerMap.remove(type);
        if (typeListener != listener && ((Connection.CompoundListener) typeListener).remove(listener)) {
            eventTypeToListenerMap.put(type, typeListener);
        } else {
            connectionHandler.onEndListen(type);
        }
    }

    /*visible for testing*/int connectionsCount() {
        return connectionsMap.size();
    }

    /*visible for testing*/int eventListenersCount() {
        return eventTypeToListenerMap.size();
    }
}
