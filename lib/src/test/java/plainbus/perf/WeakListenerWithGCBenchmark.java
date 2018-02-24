package plainbus.perf;

import org.openjdk.jmh.annotations.*;
import plainbus.Bus;
import plainbus.Listener;
import plainbus.Utils;

@State(Scope.Thread)
public class WeakListenerWithGCBenchmark {

    private Bus bus;
    private Utils.EventsCollector eventsCollector;

    @Setup
    public void setUp() {
        bus = new Bus.Builder().withWeakConnections().build();
        eventsCollector = new Utils.EventsCollector(10000);
    }

    @Benchmark
    @Warmup(iterations = 5)
    @Measurement(iterations = 5)
    @Fork(1)
    public void measureWithGc() {
        for (int i = 0; i < 1000; i++) {
            Object context = new Object();
            bus.connect(context)
                    .listen("string type", (Listener<String>) eventsCollector::collect)
                    .listen(Utils.Event.class, eventsCollector::collect);
            bus.post("string type", "event1");
            bus.post("wrong string type", "event2");
            bus.post(new Utils.Event());
        }
        System.gc();
    }

    @TearDown
    public void doAfter() {
        System.out.println("connections " + Utils.connectionsCount(bus));
        System.out.println("listeners " + Utils.eventListenersCount(bus));
        System.out.println("events " + eventsCollector.collectInvocationsCount());
    }
}
