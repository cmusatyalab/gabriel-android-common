package edu.cmu.cs.gabriel.client.socket;

import android.app.Application;
import android.os.SystemClock;
import android.util.LongSparseArray;

import com.tinder.scarlet.lifecycle.LifecycleRegistry;

import edu.cmu.cs.gabriel.client.observer.EventObserver;
import edu.cmu.cs.gabriel.client.observer.ResultObserver;
import edu.cmu.cs.gabriel.protocol.Protos.FromClient;

public class TimingSocketWrapper extends SocketWrapper {
    private LongSparseArray<Long> sentTimestamps;

    public TimingSocketWrapper(
            String serverURL, Application application, LifecycleRegistry lifecycleRegistry,
            ResultObserver resultObserver, EventObserver eventObserver) {
        super(serverURL, application, lifecycleRegistry, resultObserver, eventObserver);

        this.sentTimestamps = new LongSparseArray<>();
    }

    public void send(FromClient fromClient) {
        long timestamp = SystemClock.elapsedRealtime();
        this.sentTimestamps.put(fromClient.getFrameId(), timestamp);
        super.send(fromClient);
    }

    public LongSparseArray<Long> getSentTimestamps() {
        return sentTimestamps;
    }

    public void clearSentTimestamps() {
        sentTimestamps.clear();
    }
}
