package plainbus.perf;


import org.openjdk.jmh.annotations.*;
import plainbus.Bus;
import plainbus.Listener;
import plainbus.Utils;

@State(Scope.Thread)
public class ListenerBenchmark {

    private Bus bus;
    private Utils.EventsCollector eventsCollector;

    @Setup
    public void setUp() {
        bus = new Bus.Builder().build();
        eventsCollector = new Utils.EventsCollector(10000);
    }

    @Benchmark
    @Warmup(iterations = 5)
    @Measurement(iterations = 5)
    @Fork(1)
    public void measure() {
        new StandardTask(bus, eventsCollector).run();
    }

    @TearDown
    public void doAfter() {
        assert Utils.connectionsCount(bus) == 0;
        assert Utils.eventListenersCount(bus) == 0;
    }

    static class StandardTask {
        private final Bus bus;
        private final Utils.EventsCollector eventsCollector;

        StandardTask(Bus bus, Utils.EventsCollector eventsCollector) {
            this.bus = bus;
            this.eventsCollector = eventsCollector;
        }

        void run() {
            bus.connect(this)
                    .listen("string type", (Listener<String>) eventsCollector::collect)
                    .listen(Utils.Event.class, eventsCollector::collect);
            bus.connect(eventsCollector).listen(Utils.Event.class, eventsCollector::collect);
            bus.post("string type", "event1");
            bus.post("wrong string type", "event2");
            bus.post(new Utils.Event());
            bus.disconnect(this);
            bus.disconnect(eventsCollector);
        }
    }
}
