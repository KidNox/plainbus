package plainbus;


public interface Bus {

    Connection connect(Object context);

    void disconnect(Object context);

    boolean post(Object event);

    boolean post(Object type, Object event);


    class Builder {
        private ConnectionHandler connectionHandler;

        public Builder withConnectionHandler(ConnectionHandler connectionHandler) {
            this.connectionHandler = connectionHandler;
            return this;
        }

        public Bus build() {
            if (connectionHandler == null) {
                connectionHandler = new ConnectionHandler.Stub();
            }
            return new BusImpl(connectionHandler, Connection::new);
        }
    }
}
