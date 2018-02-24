package plainbus;

import java.util.concurrent.atomic.AtomicReference;

public class Utils {
    public static class Event {
    }

    public static <T> Listener<T> getStub() {
        return t -> {

        };
    }

    public static <T> Listener<T> getListenerWithReferenceHolder(AtomicReference reference) {
        return reference::set;
    }

    public static class EventsCollector {
        private final Object[] array;
        private int currentIndex;
        private int fullCycles;

        public EventsCollector(int size) {
            array = new Object[size];
        }

        public void collect(Object event) {
            int currentIndex = this.currentIndex;
            if (currentIndex == array.length) {
                currentIndex = this.currentIndex = 0;
                fullCycles++;
            }
            array[currentIndex] = event;
            this.currentIndex++;
        }

        public long collectInvocationsCount() {
            return (fullCycles * array.length) + currentIndex;
        }
    }

    public static int connectionsCount(Bus bus) {
        return ((BusImpl) bus).connectionsCount();
    }

    public static int eventListenersCount(Bus bus) {
        return ((BusImpl) bus).eventListenersCount();
    }
}
