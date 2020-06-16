package edu.cmu.cs.gabriel.client.comm;

import android.app.Application;

import com.tinder.scarlet.Lifecycle;
import com.tinder.scarlet.ShutdownReason;
import com.tinder.scarlet.lifecycle.LifecycleRegistry;

import edu.cmu.cs.gabriel.client.R;
import edu.cmu.cs.gabriel.client.function.Consumer;
import edu.cmu.cs.gabriel.client.function.Supplier;
import edu.cmu.cs.gabriel.client.observer.EventObserver;
import edu.cmu.cs.gabriel.client.socket.SocketWrapper;
import edu.cmu.cs.gabriel.client.token.TokenManager;
import edu.cmu.cs.gabriel.protocol.Protos;
import edu.cmu.cs.gabriel.protocol.Protos.FromClient;

public abstract class ServerCommCore {
    private static final String TAG = "ServerCommCore";

    TokenManager tokenManager;
    SocketWrapper socketWrapper;
    private long frameID;
    final LifecycleRegistry lifecycleRegistry;
    Consumer<Protos.ResultWrapper.Status> onErrorResult;
    EventObserver eventObserver;

    public ServerCommCore(
            final Consumer<String> onDisconnect, int tokenLimit, final Application application) {
        this.tokenManager = new TokenManager(tokenLimit);
        this.frameID = 0;

        this.lifecycleRegistry = new LifecycleRegistry(0L);
        this.onErrorResult = new Consumer<Protos.ResultWrapper.Status>() {
            @Override
            public void accept(Protos.ResultWrapper.Status status) {
                lifecycleRegistry.onNext(
                        new Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL));
                String messagePrefix = application.getResources().getString(
                        R.string.server_error_prefix);
                onDisconnect.accept(messagePrefix + status.name());
                ServerCommCore.this.tokenManager.stop();
            }
        };

        Runnable onConnectionProblem = new Runnable() {
            @Override
            public void run() {
                String message = ServerCommCore.this.tokenManager.isRunning()
                        ? application.getResources().getString(R.string.server_disconnected)
                        : application.getResources().getString(R.string.could_not_connect);
                onDisconnect.accept(message);
            }
        };
        this.eventObserver = new EventObserver(this.tokenManager, onConnectionProblem);
    }

    private void sendHelper(FromClient.Builder fromClientBuilder) {
        fromClientBuilder.setFrameId(this.frameID);
        FromClient fromClient = fromClientBuilder.build();
        this.socketWrapper.send(fromClient);
        this.frameID++;
    }

    /**
     * Check if the server has a cognitive engine that input that has passed filterName.
     *
     * @param filterName
     * @return True if server accepts input for filterName. False if not, or if we ran into an
     * error.
     */
    public boolean acceptsInputForFilter(String filterName) {
        return this.tokenManager.tokensForFilter(filterName);
    }

    /** Send if there is at least one token available. Returns false if there were no tokens. */
    public boolean sendNoWait(FromClient.Builder fromClientBuilder) {
        String filterName = fromClientBuilder.getFilterPassed();

        boolean gotToken = tokenManager.getTokenNoWait(filterName);
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
     * @return True if send succeeded.
     */
    public boolean sendBlocking(FromClient.Builder fromClientBuilder) {
        String filterName = fromClientBuilder.getFilterPassed();

        boolean gotToken = this.tokenManager.getToken(filterName);
        if (!gotToken) {
            return false;
        }

        this.sendHelper(fromClientBuilder);
        return true;
    }

    /** Wait until there is a token available. Then call @param supplier to get the partially built
     * fromClientBuilder to send. fromClientBuilder is modified and sent according to the
     * description of {@link #sendBlocking} */
    public SendSupplierResult sendSupplier(
            Supplier<FromClient.Builder> supplier, String filterName) {
        boolean gotToken = this.tokenManager.getToken(filterName);
        if (!gotToken) {
            return SendSupplierResult.ERROR_GETTING_TOKEN;
        }

        FromClient.Builder fromClientBuilder = supplier.get();
        if (fromClientBuilder == null) {
            this.tokenManager.returnToken(filterName);
            return SendSupplierResult.NULL_FROM_SUPPLIER;
        }

        this.sendHelper(fromClientBuilder);
        return SendSupplierResult.SUCCESS;
    }

    public void stop() {
        this.tokenManager.stop();
        this.socketWrapper.stop();
    }

    public boolean isRunning() {
        return this.tokenManager.isRunning();
    }
}
