package edu.cmu.cs.gabriel.client;

import android.app.Application;

import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;

public class ServerComm extends ServerCommCore{
    public ServerComm(Consumer<ResultWrapper> consumer, Runnable onDisconnect, String serverIP,
                      int port, Application application) {
        super(consumer, onDisconnect, serverIP, port, application);

        ResultObserver resultObserver = new ResultObserver(this.tokenManager, consumer);
        EventObserver eventObserver = new EventObserver(this.tokenManager, onDisconnect);
        this.socketWrapper = new SocketWrapper(serverIP, port, application, resultObserver,
                eventObserver);
    }
}
