package plainbus;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Connection {

    private final List<ListenerWithType> listeners = new LinkedList<>();
    final BusImpl bus;

    Connection(BusImpl bus) {
        this.bus = bus;
    }

    public <Event> Connection listen(Object type, Listener<Event> listener) {
        listeners.add(new ListenerWithType(type, listener));
        bus.addListener(type, listener);
        return this;
    }

    public <Event> Connection listen(Class<Event> type, Listener<Event> listener) {
        if (type.isInterface()) {
            throw new IllegalArgumentException("unsupported type(interface): " + type);
        }
        if (type == Object.class) {
            throw new IllegalArgumentException("unsupported type: java.lang.Object");
        }
        listeners.add(new ListenerWithType(type, listener));
        bus.addListener(type, listener);
        return this;
    }

    void close() {
        for (ListenerWithType listenerWithType : listeners) {
            bus.removeListener(listenerWithType.type, listenerWithType.listener);
        }
    }

    interface ConnectionFactory {
        Connection createConnection(BusImpl bus);
    }

    static class ListenerWithType {
        final Object type;
        final Listener listener;

        ListenerWithType(Object type, Listener listener) {
            this.type = type;
            this.listener = listener;
        }
    }

    static class CompoundListener implements Listener {

        static CompoundListener wrap(Object type, Listener current, Listener second, ConnectionHandler handler) {
            CompoundListener compound;
            if (current instanceof CompoundListener) {
                compound = (CompoundListener) current;
            } else {
                compound = new CompoundListener(type, handler);
                compound.add(current);
            }
            compound.add(second);
            return compound;
        }

        private final Set<Listener> listeners = new HashSet<>();
        private final Object type;
        private final ConnectionHandler handler;

        CompoundListener(Object type, ConnectionHandler handler) {
            this.type = type;
            this.handler = handler;
        }

        @SuppressWarnings("unchecked")
        @Override public void onEvent(Object event) {
            for (Listener listener : listeners) {
                try {
                    listener.onEvent(event);
                } catch (Listener.Rejection ignored) {
                    handler.onEventRejected(type, event, listener.getClass());
                }
            }
        }

        private void add(Listener listener) {
            if (!listeners.add(listener)) throw new IllegalStateException("try to compound same listeners");
        }

        boolean remove(Listener listener) {
            listeners.remove(listener);
            return !listeners.isEmpty();
        }
    }
}
