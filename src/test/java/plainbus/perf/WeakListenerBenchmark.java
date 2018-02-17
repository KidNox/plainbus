package plainbus.perf;

import org.openjdk.jmh.annotations.*;
import plainbus.Bus;
import plainbus.Utils;

@State(Scope.Thread)
public class WeakListenerBenchmark {

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
    public void measure() {
        new ListenerBenchmark.StandardTask(bus, eventsCollector).run();
    }

    @TearDown
    public void doAfter() {
        assert Utils.connectionsCount(bus) == 0;
        assert Utils.eventListenersCount(bus) == 0;
    }
}
