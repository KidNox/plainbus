package plainbus;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public interface Bus {

    Connection connect(Object context);

    void disconnect(Object context);

    boolean post(Object event);

    boolean post(Object type, Object event);


    class Builder {
        private ConnectionHandler connectionHandler;
        private boolean weak;

        public Builder withConnectionHandler(ConnectionHandler connectionHandler) {
            this.connectionHandler = connectionHandler;
            return this;
        }

        public Builder withWeakConnections() {
            weak = true;
            return this;
        }

        public Bus build() {
            if (connectionHandler == null) {
                connectionHandler = new ConnectionHandler.Stub();
            }
            Connection.ConnectionFactory connectionFactory = weak ? Connection.WeakConnection::new : Connection::new;
            Map<Object, Connection> connectionsMap = weak ? new WeakHashMap<>() : new HashMap<>();
            return new BusImpl(connectionHandler, connectionFactory, connectionsMap);
        }
    }
}
