package edu.cmu.cs.gabriel.client;

import android.app.Application;

import com.tinder.scarlet.Lifecycle;
import com.tinder.scarlet.Scarlet;
import com.tinder.scarlet.lifecycle.LifecycleRegistry;
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle;
import com.tinder.scarlet.messageadapter.protobuf.ProtobufMessageAdapter;
import com.tinder.scarlet.websocket.okhttp.OkHttpClientUtils;

import okhttp3.OkHttpClient;

public class SocketWrapper {
    private LifecycleRegistry lifecycleRegistry;
    private GabrielSocket webSocketInterface;

    public static GabrielSocket createSocket(
            String serverIP, int port, Application application, Lifecycle lifecycle) {
        String url = "ws://" + serverIP + ":" + port;
        OkHttpClient okClient = new OkHttpClient();

        return new Scarlet.Builder()
                .webSocketFactory(OkHttpClientUtils.newWebSocketFactory(okClient, url))
                .addMessageAdapterFactory(new ProtobufMessageAdapter.Factory())
                .lifecycle(lifecycle)
                .build().create(GabrielSocket.class);
    }

    public static LifecycleRegistry createLifecycleRegistry() {
        LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(0L);
        lifecycleRegistry.onNext(Lifecycle.State.Started.INSTANCE);
        return lifecycleRegistry;
    }

    public static SocketWrapper generateStandard(
            String serverIP, int port, Application application) {
        LifecycleRegistry lifecycleRegistry = SocketWrapper.createLifecycleRegistry();

        Lifecycle androidLifecycle = AndroidLifecycle.ofApplicationForeground(application);
        Lifecycle socketLifecycle = androidLifecycle.combineWith(lifecycleRegistry);
        GabrielSocket webSocketInterface = SocketWrapper.createSocket(
                serverIP, port, application, socketLifecycle);

        return new SocketWrapper(lifecycleRegistry, webSocketInterface);
    }

    public SocketWrapper(LifecycleRegistry lifecycleRegistry, GabrielSocket webSocketInterface) {
        this.lifecycleRegistry = lifecycleRegistry;
        this.webSocketInterface = webSocketInterface;
    }

    public GabrielSocket getWebSocketInterface() {
        return webSocketInterface;
    }

    public LifecycleRegistry getLifecycleRegistry() {
        return lifecycleRegistry;
    }
}
