package edu.cmu.cs.gabriel.client.socket;

import android.app.Application;
import android.util.LongSparseArray;

import edu.cmu.cs.gabriel.client.observer.EventObserver;
import edu.cmu.cs.gabriel.client.observer.ResultObserver;
import edu.cmu.cs.gabriel.protocol.Protos.FromClient;

public class TimingSocketWrapper extends SocketWrapper {
    private LongSparseArray<Long> sentTimestamps;

    public TimingSocketWrapper(String serverIP, int port, Application application,
                               ResultObserver resultObserver, EventObserver eventObserver) {
        super(serverIP, port, application, resultObserver, eventObserver);

        this.sentTimestamps = new LongSparseArray<>();
    }

    public void send(FromClient fromClient) {
        // TODO: change to java.time.Instant once we can stop supporting Google Glass Explorer
        //       Edition
        long timestamp = System.currentTimeMillis();

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
