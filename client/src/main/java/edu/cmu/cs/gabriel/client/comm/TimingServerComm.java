package edu.cmu.cs.gabriel.client.comm;

import android.app.Application;
import android.util.Log;
import android.util.LongSparseArray;

import edu.cmu.cs.gabriel.client.observer.EventObserver;
import edu.cmu.cs.gabriel.client.observer.ResultObserver;
import edu.cmu.cs.gabriel.client.socket.TimingSocketWrapper;
import edu.cmu.cs.gabriel.client.function.Consumer;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;

public class TimingServerComm extends ServerComm {
    private final static String TAG = "TimingServerComm";
    private final static int DEFAULT_OUTPUT_FREQ = 10;

    private LongSparseArray<Long> receivedTimestamps;
    private TimingSocketWrapper timingSocketWrapper;
    private long count;
    private long intervalCount;
    private long startTime;
    private long intervalStartTime;

    // TODO: Replace these constructors with a builder to allow setting token_limit without setting
    //       output_freq
    public TimingServerComm(final Consumer<ResultWrapper> consumer, Runnable onDisconnect,
                            String serverIP, int port, Application application,
                            int tokenLimit, final int outputFreq) {
        super(consumer, onDisconnect, serverIP, port, application, tokenLimit);

        this.receivedTimestamps = new LongSparseArray<>();

        Consumer<ResultWrapper> timingConsumer = new Consumer<ResultWrapper>() {
            @Override
            public void accept(ResultWrapper resultWrapper) {
                consumer.accept(resultWrapper);

                // TODO: Change to java.time.Instant once we can stop supporting Google Glass
                //       Explorer Edition
                long timestamp = System.currentTimeMillis();

                TimingServerComm.this.receivedTimestamps.put(resultWrapper.getFrameId(), timestamp);
                TimingServerComm.this.count++;
                TimingServerComm.this.intervalCount++;

                if (TimingServerComm.this.count % outputFreq == 0) {
                    long start_time = TimingServerComm.this.startTime;
                    double overall_fps = (double) TimingServerComm.this.count /
                            ((timestamp - start_time) / 1000.0);
                    Log.i(TAG, "Overall FPS: " + overall_fps);

                    long interval_count = TimingServerComm.this.intervalCount;
                    long interval_start_time = TimingServerComm.this.intervalStartTime;
                    double interval_fps = (double) interval_count /
                            ((timestamp - interval_start_time) / 1000.0);
                    Log.i(TAG, "Interval FPS: " + interval_fps);

                    TimingServerComm.this.intervalCount = 0;

                    // TODO: change to java.time.Instant once we can stop supporting Google Glass
                    //       Explorer Edition
                    TimingServerComm.this.intervalStartTime = System.currentTimeMillis();
                }
            }
        };

        ResultObserver resultObserver = new ResultObserver(this.tokenManager, timingConsumer);
        EventObserver eventObserver = new EventObserver(this.tokenManager, onDisconnect);
        this.timingSocketWrapper = new TimingSocketWrapper(
                serverIP, port, application, resultObserver, eventObserver);
        this.socketWrapper = this.timingSocketWrapper;

        this.count = 0;
        this.intervalCount = 0;

        // TODO: change to java.time.Instant once we can stop supporting Google Glass Explorer
        //       Edition
        this.startTime = System.currentTimeMillis();

        this.intervalStartTime = this.startTime;
    }

    public TimingServerComm(final Consumer<ResultWrapper> consumer, Runnable onDisconnect,
                            String serverIP, int port, Application application,
                            int tokenLimit) {
        this(consumer, onDisconnect, serverIP, port, application, tokenLimit,
                TimingServerComm.DEFAULT_OUTPUT_FREQ);
    }

    public TimingServerComm(final Consumer<ResultWrapper> consumer, Runnable onDisconnect,
                            String serverIP, int port, Application application) {
        this(consumer, onDisconnect, serverIP, port, application, Integer.MAX_VALUE);
    }

    public void logAvgRtt() {
        long count = 0;
        long totalRtt = 0;

        LongSparseArray<Long> sentTimestamps = this.timingSocketWrapper.getSentTimestamps();
        for (int i = 0; i < sentTimestamps.size(); i++) {
             long frameId = sentTimestamps.keyAt(i);
             Long sentTimestamp = sentTimestamps.valueAt(i);

             Long receivedTimestamp = receivedTimestamps.get(frameId);
             if (receivedTimestamp == null) {
                Log.e(TAG, "Frame with ID " + frameId + " never received");
             } else {
                 count++;
                 totalRtt += (receivedTimestamp - sentTimestamp);
             }
        }

        Log.i(TAG, "Average RTT: " + ((double)totalRtt / count) + " ms");
    }

    public void clearTimestamps() {
        this.timingSocketWrapper.clearSentTimestamps();
        this.receivedTimestamps.clear();
    }
}
