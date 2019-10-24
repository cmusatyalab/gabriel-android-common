package edu.cmu.cs.gabriel.client;

import android.app.Application;
import android.util.Log;

import com.tinder.scarlet.Lifecycle;
import com.tinder.scarlet.ShutdownReason;
import com.tinder.scarlet.lifecycle.LifecycleRegistry;

import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;
import edu.cmu.cs.gabriel.protocol.Protos.FromClient;

public abstract class ServerComm {
    private static String TAG = "ServerComm";

    private GabrielSocket webSocketInterface;
    private LifecycleRegistry lifecycleRegistry;
    private long frameID;
    private boolean connected;

    protected abstract void handleResults(ResultWrapper resultWrapper);
    protected abstract void handleDisconnect();

    public ServerComm(String serverIP, int port, Application application) {
        this(SocketWrapper.generateStandard(serverIP, port, application));
    }

    public ServerComm(SocketWrapper socketWrapper) {
        this.webSocketInterface = socketWrapper.getWebSocketInterface();
        this.lifecycleRegistry = socketWrapper.getLifecycleRegistry();

        this.frameID = 0;
        this.connected = false;

        webSocketInterface.Receive().start(new ResultObserver() {
            @Override
            protected void handleResults(ResultWrapper resultWrapper) {
                ServerComm.this.handleResults(resultWrapper);
            }


            @Override
            protected void returnToken() {
                ServerComm.this.returnToken();
            }
        });

        webSocketInterface.observeWebSocketEvent().start(new EventObserver() {
            @Override
            protected void onConnect() {
                ServerComm.this.connected = true;
            }

            @Override
            protected void onDisconnect() {
                ServerComm.this.handleDisconnect();

                // Must be called after ServerComm.this.handleDisconnect() because this might check
                // the connected status.
                ServerComm.this.connected = false;

                synchronized (ServerComm.this.tokenLock) {
                    // Zero out tokens so we have to get them from the server if we reconnect
                    ServerComm.this.numTokens = 0;

                    // This will allow a background handler to return if it is currently waiting on
                    // ServerComm.this.tokenLock
                    ServerComm.this.tokenLock.notify();
                }
            }
        });
    }

    private void sendHelper(FromClient.Builder fromClientBuilder) {
        fromClientBuilder.setFrameId(this.frameID);
        FromClient fromClient = fromClientBuilder.build();
        this.webSocketInterface.Send(fromClient);
        this.frameID++;
        Log.d(TAG, "numTokens is now " + this.numTokens);
    }







    /** Send if there is at least one token available. Returns false if there were no tokens. */
    private boolean sendNoWait(FromClient.Builder fromClientBuilder) {
        boolean gotToken = getTokenNoWait();

        if (gotToken) {
            this.sendHelper(fromClientBuilder);
        }
        return gotToken;
    }

    /**
     * Wait until there is a token available.
     *
     * Then add the current frame ID to fromClientBuilder. Then build
     * fromClientBuilder and send the resulting FromClient.
     *
     * @param fromClientBuilder Item to send
     * @return False if we ran into an error.
     */
    public boolean sendBlocking(FromClient.Builder fromClientBuilder) {
        boolean gotToken = getToken();

        if (gotToken) {
            this.sendHelper(fromClientBuilder);
        }
        return gotToken;
    }

    /** Wait until there is a token available. Then call @param supplier to get the partially built
     * fromClientBuilder to send. fromClientBuilder is modified and sent according to the
     * description of {@link #sendBlocking}
     * Return false if we ran into an error. */
    public boolean sendSupplier(Supplier<FromClient.Builder> supplier) {
        boolean gotToken = getToken();

        if (gotToken) {
            FromClient.Builder fromClientBuilder = supplier.get();

            if (fromClientBuilder != null) {
                this.sendHelper(fromClientBuilder);
                return true;
            } else {
                this.returnToken();
            }
        }
        return false;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void stop() {
        lifecycleRegistry.onNext(new Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL));
    }
}
