package plainbus.test;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import plainbus.Bus;
import plainbus.ConnectionHandler;
import plainbus.Utils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static plainbus.Listener.reject;

public class ListenerTest {

    private ConnectionHandler connectionHandler;
    private Bus bus;

    @Before public void setUp() {
        connectionHandler = Mockito.mock(ConnectionHandler.class);
        bus = new Bus.Builder().withConnectionHandler(connectionHandler).build();
    }

    @Test public void oneListenTest() {
        AtomicReference reference = new AtomicReference();
        verifyZeroInteractions(connectionHandler);
        bus.connect(this).listen(Integer.class, Utils.getListenerWithReferenceHolder(reference));
        verify(connectionHandler).onStartListen(Integer.class);
        verify(connectionHandler, never()).onEndListen(any());
        assertTrue(bus.post(42));
        assertFalse(bus.post(""));
        assertEquals(42, reference.get());
        bus.disconnect(this);
        verify(connectionHandler).onEndListen(Integer.class);
        assertFalse(bus.post(100));
        assertEquals(42, reference.get());
        verify(connectionHandler, times(1)).onStartListen(Integer.class);
        verify(connectionHandler, times(1)).onEndListen(Integer.class);
    }

    @Test public void oneListenSimpleTest() {
        AtomicReference<String> reference = new AtomicReference<>();
        bus.connect(this).listen("event1", reference::set);
        verify(connectionHandler).onStartListen("event1");
        verify(connectionHandler, never()).onEndListen(any());
        assertFalse(bus.post("test"));
        assertFalse(bus.post(42));
        assertTrue(bus.post("event1", "test value"));
        assertEquals("test value", reference.get());
        bus.disconnect(this);
        verify(connectionHandler).onEndListen("event1");
        assertFalse(bus.post("event1", ""));
    }

    @Test(expected = ClassCastException.class)
    public void wrongTypeTest() {
        AtomicReference<String> reference = new AtomicReference<>();
        bus.connect(this).listen("event1", reference::set);
        bus.post("event1", 42);
    }

    @Test public void multiplyListenersTest() {
        AtomicReference integerReference = new AtomicReference();
        AtomicReference numberReference = new AtomicReference();
        bus.connect(this)
                .listen(Integer.class, Utils.getListenerWithReferenceHolder(integerReference))
                .listen(Number.class, Utils.getListenerWithReferenceHolder(numberReference));
        bus.post(42);
        assertEquals(42, integerReference.get());
        assertEquals(42, numberReference.get());
        assertTrue(bus.post(11.1));
        assertEquals(42, integerReference.get());
        assertEquals(11.1, numberReference.get());
        bus.disconnect(this);
        assertFalse(bus.post(100));
        assertEquals(42, integerReference.get());
        assertEquals(11.1, numberReference.get());
    }

    @Test public void multiplyConnectionsTest() {
        AtomicReference integerReference = new AtomicReference();
        AtomicReference numberReference = new AtomicReference();
        bus.connect(this).listen(Integer.class, Utils.getListenerWithReferenceHolder(integerReference));
        verify(connectionHandler).onStartListen(Integer.class);
        bus.connect(this).listen(Number.class, Utils.getListenerWithReferenceHolder(numberReference));
        verify(connectionHandler).onStartListen(Number.class);
        verify(connectionHandler, never()).onEndListen(any());
        bus.post(42);
        assertEquals(42, integerReference.get());
        assertEquals(42, numberReference.get());
        bus.disconnect(this);
        assertFalse(bus.post(100));
        assertEquals(42, integerReference.get());
        assertEquals(42, numberReference.get());
        verify(connectionHandler, times(1)).onStartListen(Integer.class);
        verify(connectionHandler, times(1)).onEndListen(Integer.class);
        verify(connectionHandler, times(1)).onStartListen(Number.class);
        verify(connectionHandler, times(1)).onEndListen(Number.class);
    }

    @Test public void multiplyDisconnectionTest() {
        bus.connect(this);
        bus.disconnect(this);
        try {
            bus.disconnect(this);
            fail();
        } catch (IllegalStateException ex) {
            assertEquals(this + " is not connected", ex.getMessage());
        }
    }

    @Test public void stubConnectionHandlerTest() {
        bus = new Bus.Builder().build();

        bus.connect(this).listen(String.class, s -> {

        });
        bus.connect(this)
                .listen(String.class, Utils.getStub())
                .listen(Integer.class, Utils.getStub());
        bus.post("");
        bus.disconnect(this);
    }

    @Test public void rejectTest() {
        AtomicBoolean reference = new AtomicBoolean();
        bus.connect(this).listen(Boolean.class, aBoolean -> {
            reference.set(aBoolean);
            if (!aBoolean) {
                reject();
                fail();
            }
        });
        assertTrue(bus.post(true));
        assertEquals(true, reference.get());
        assertFalse(bus.post(false));
        assertEquals(false, reference.get());
    }

    @Test public void complexTest() {
        bus.connect(this)
                .listen(String.class, Utils.getStub())
                .listen(Integer.class, Utils.getStub())
                .listen(Number.class, Utils.getStub())
                .listen(Boolean.class, success -> {
                    if (!success) {
                        reject();
                    }
                });
        assertTrue(bus.post("hello"));
        assertTrue(bus.post(1));
        assertTrue(bus.post(1.1));
        assertTrue(bus.post(true));
        assertFalse(bus.post(false));
        bus.disconnect(this);
        assertFalse(bus.post("hello"));
        assertFalse(bus.post(1));
        assertFalse(bus.post(1.1));
        assertFalse(bus.post(true));
        assertFalse(bus.post(false));

        verify(connectionHandler, times(4)).onStartListen(any());
        verify(connectionHandler, times(4)).onEndListen(any());
    }

    @Test public void multiplyContextsTest() {
        Object context1 = new Object();
        Object context2 = new Object();
        Object context3 = new Object();
        AtomicReference stringRef1 = new AtomicReference();
        AtomicReference stringRef2 = new AtomicReference();
        AtomicReference stringRef3 = new AtomicReference();
        AtomicReference intRef1 = new AtomicReference();
        AtomicReference intRef2 = new AtomicReference();
        bus.connect(context1)
                .listen(String.class, Utils.getListenerWithReferenceHolder(stringRef1));
        verify(connectionHandler).onStartListen(String.class);
        bus.connect(context2)
                .listen(String.class, Utils.getListenerWithReferenceHolder(stringRef2))
                .listen(Integer.class, Utils.getListenerWithReferenceHolder(intRef2));
        verify(connectionHandler).onStartListen(Integer.class);
        bus.post("ev1");
        bus.post(42);
        assertEquals("ev1", stringRef1.get());
        assertEquals("ev1", stringRef2.get());
        assertEquals(42, intRef2.get());
        bus.connect(context1)
                .listen(Integer.class, Utils.getListenerWithReferenceHolder(intRef1));
        bus.post(100);
        assertEquals(100, intRef1.get());
        assertEquals(100, intRef2.get());

        bus.connect(context3)
                .listen(String.class, Utils.getListenerWithReferenceHolder(stringRef3));
        bus.post("ev2");
        assertEquals("ev2", stringRef1.get());
        assertEquals("ev2", stringRef2.get());
        assertEquals("ev2", stringRef3.get());

        bus.disconnect(context2);
        verify(connectionHandler, never()).onEndListen(any());

        bus.post(99);
        bus.post("ev3");
        assertEquals(99, intRef1.get());
        assertEquals(100, intRef2.get());
        assertEquals("ev3", stringRef1.get());
        assertEquals("ev2", stringRef2.get());

        bus.disconnect(context1);
        verify(connectionHandler).onEndListen(Integer.class);
        verify(connectionHandler, never()).onEndListen(String.class);

        assertFalse(bus.post(1));
        assertEquals(99, intRef1.get());
        assertTrue(bus.post("ev4"));
        assertEquals("ev4", stringRef3.get());
        assertEquals("ev3", stringRef1.get());

        bus.disconnect(context3);
        verify(connectionHandler, times(1)).onStartListen(String.class);
        verify(connectionHandler, times(1)).onStartListen(Integer.class);
        verify(connectionHandler, times(1)).onEndListen(String.class);
        verify(connectionHandler, times(1)).onEndListen(Integer.class);
    }

}
