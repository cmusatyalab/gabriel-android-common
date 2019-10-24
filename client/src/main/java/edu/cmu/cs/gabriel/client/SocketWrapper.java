package edu.cmu.cs.gabriel.client;

import android.app.Application;

import com.tinder.scarlet.Lifecycle;
import com.tinder.scarlet.Scarlet;
import com.tinder.scarlet.ShutdownReason;
import com.tinder.scarlet.lifecycle.LifecycleRegistry;
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle;
import com.tinder.scarlet.websocket.okhttp.OkHttpClientUtils;

import edu.cmu.cs.gabriel.protocol.Protos.FromClient;
import okhttp3.OkHttpClient;

public class SocketWrapper {
    private LifecycleRegistry lifecycleRegistry;
    private GabrielSocket webSocketInterface;

    public SocketWrapper(
            String serverIP, int port, Application application, ResultObserver resultObserver,
            EventObserver eventObserver) {
        this.lifecycleRegistry = new LifecycleRegistry(0L);
        this.lifecycleRegistry.onNext(Lifecycle.State.Started.INSTANCE);

        Lifecycle androidLifecycle = AndroidLifecycle.ofApplicationForeground(application);

        String url = "ws://" + serverIP + ":" + port;
        OkHttpClient okClient = new OkHttpClient();

        this.webSocketInterface = (new Scarlet.Builder())
                .webSocketFactory(OkHttpClientUtils.newWebSocketFactory(okClient, url))
                .lifecycle(androidLifecycle.combineWith(lifecycleRegistry))
                .build().create(GabrielSocket.class);
        this.webSocketInterface.receive().start(resultObserver);
        this.webSocketInterface.observeWebSocketEvent().start(eventObserver);
    }

    public void send(FromClient fromClient) {
        this.webSocketInterface.send(fromClient.toByteArray());
    }

    public void stop() {
        lifecycleRegistry.onNext(new Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL));
    }
}
