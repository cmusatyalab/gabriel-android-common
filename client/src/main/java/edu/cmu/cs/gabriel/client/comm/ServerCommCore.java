package edu.cmu.cs.gabriel.client.comm;

import android.app.Application;

import edu.cmu.cs.gabriel.client.socket.SocketWrapper;
import edu.cmu.cs.gabriel.client.token.TokenManager;
import edu.cmu.cs.gabriel.client.function.Consumer;
import edu.cmu.cs.gabriel.client.function.Supplier;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;
import edu.cmu.cs.gabriel.protocol.Protos.FromClient;

public abstract class ServerCommCore {
    private static String TAG = "ServerCommCore";

    TokenManager tokenManager;
    SocketWrapper socketWrapper;
    private long frameID;

    public ServerCommCore(Consumer<ResultWrapper> consumer, Runnable onDisconnect, String serverIP,
                          int port, Application application) {
        this.tokenManager = new TokenManager();
        this.frameID = 0;
    }

    long getFrameID() {
        return this.frameID;
    }

    void sendHelper(FromClient.Builder fromClientBuilder) {
        fromClientBuilder.setFrameId(this.frameID);
        FromClient fromClient = fromClientBuilder.build();
        this.socketWrapper.send(fromClient);
        this.frameID++;
    }

    /** Send if there is at least one token available. Returns false if there were no tokens. */
    public boolean sendNoWait(FromClient.Builder fromClientBuilder) {
        boolean gotToken = tokenManager.getTokenNoWait();
        if (!gotToken) {
            return false;
        }

        this.sendHelper(fromClientBuilder);
        return true;
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
        boolean gotToken = this.tokenManager.getToken();
        if (!gotToken) {
            return false;
        }

        this.sendHelper(fromClientBuilder);
        return true;
    }

    /** Wait until there is a token available. Then call @param supplier to get the partially built
     * fromClientBuilder to send. fromClientBuilder is modified and sent according to the
     * description of {@link #sendBlocking}
     * Return false if we ran into an error. */
    public boolean sendSupplier(Supplier<FromClient.Builder> supplier) {
        boolean gotToken = this.tokenManager.getToken();
        if (!gotToken) {
            return false;
        }

        FromClient.Builder fromClientBuilder = supplier.get();
        if (fromClientBuilder == null) {
            this.tokenManager.returnToken();
            return false;
        }

        this.sendHelper(fromClientBuilder);
        return true;
    }

    public void stop() {
        this.tokenManager.stop();
        this.socketWrapper.stop();
    }

    public boolean isRunning() {
        return this.tokenManager.isRunning();
    }
}
