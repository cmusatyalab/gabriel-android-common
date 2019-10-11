package edu.cmu.cs.gabriel.client;

import android.app.Application;
import android.util.Log;

import com.tinder.scarlet.Lifecycle;
import com.tinder.scarlet.Scarlet;
import com.tinder.scarlet.ShutdownReason;
import com.tinder.scarlet.lifecycle.LifecycleRegistry;
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle;
import com.tinder.scarlet.messageadapter.protobuf.ProtobufMessageAdapter;
import com.tinder.scarlet.websocket.okhttp.OkHttpClientUtils;

import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;
import edu.cmu.cs.gabriel.protocol.Protos.FromClient;
import okhttp3.OkHttpClient;

public abstract class ServerComm {
    private static String TAG = "ServerComm";

    private GabrielSocket webSocketInterface;
    private LifecycleRegistry lifecycleRegistry;
    private long frameID;
    private boolean connected;
    private Object tokenLock;
    private int numTokens;

    protected abstract void handleResults(ResultWrapper resultWrapper);
    protected abstract void handleDisconnect();

    public ServerComm(String serverIP, int port, Application application) {
        String url = "ws://" + serverIP + ":" + port;
        frameID = 0;
        this.connected = false;

        OkHttpClient okClient = new OkHttpClient();

        Lifecycle androidLifecycle = AndroidLifecycle.ofApplicationForeground(application);
        this.lifecycleRegistry = new LifecycleRegistry(0L);
        this.lifecycleRegistry.onNext(Lifecycle.State.Started.INSTANCE);

        webSocketInterface = new Scarlet.Builder()
                .webSocketFactory(OkHttpClientUtils.newWebSocketFactory(okClient, url))
                .addMessageAdapterFactory(new ProtobufMessageAdapter.Factory())
                .lifecycle(androidLifecycle.combineWith(this.lifecycleRegistry))
                .build().create(GabrielSocket.class);

        this.tokenLock = new Object();
        this.numTokens = 0;

        webSocketInterface.Receive().start(new ResultObserver() {
            @Override
            protected void handleResults(ResultWrapper resultWrapper) {
                ServerComm.this.handleResults(resultWrapper);
            }

            @Override
            protected void updateNumTokens(int numTokens) {
                synchronized (ServerComm.this.tokenLock) {
                    ServerComm.this.numTokens = numTokens;
                    ServerComm.this.tokenLock.notify();
                }

                Log.i(TAG, "numTokens is now " + numTokens);
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

    /** Take token if one is available. Otherwise return False. */
    private boolean getTokenNoWait() {
        boolean gotToken;
        synchronized (this.tokenLock) {
            gotToken = numTokens > 0;
            if (gotToken) {
                numTokens--;
            }
        }

        return gotToken;
    }

    /** Wait until there is a token available. Then take it. */
    private boolean getToken() {
        boolean tokenAvailable;
        synchronized (this.tokenLock) {
            try {
                while (this.numTokens < 1) {
                    if (!this.connected) {
                        Log.i(TAG, "Not connected. Will not wait for token");
                        return false;
                    }

                    Log.d(TAG, "Too few tokens. Waiting for more.");
                    this.tokenLock.wait();
                }
                tokenAvailable = true;
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted Exception while waiting for lock", e);
                tokenAvailable = false;
            }

            if (tokenAvailable) {
                this.numTokens--;
            }
        }
        return tokenAvailable;
    }

    private void returnToken() {
        synchronized (ServerComm.this.tokenLock) {
            ServerComm.this.numTokens++;
            ServerComm.this.tokenLock.notify();
        }
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
     * @param fromClientBuilder
     * @return Return false if we ran into an error.
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
