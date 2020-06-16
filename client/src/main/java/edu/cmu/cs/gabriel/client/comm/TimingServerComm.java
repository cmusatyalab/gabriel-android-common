package edu.cmu.cs.gabriel.client.comm;

import android.app.Application;
import android.os.SystemClock;
import android.util.Log;
import android.util.LongSparseArray;

import edu.cmu.cs.gabriel.client.observer.ResultObserver;
import edu.cmu.cs.gabriel.client.socket.TimingSocketWrapper;
import edu.cmu.cs.gabriel.client.function.Consumer;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;

public class TimingServerComm extends ServerCommCore {
    private final static String TAG = "TimingServerComm";
    private final static int DEFAULT_OUTPUT_FREQ = 10;

    private LongSparseArray<Long> receivedTimestamps;
    private TimingSocketWrapper timingSocketWrapper;
    private long count;
    private long intervalCount;
    private long startTime;
    private long intervalStartTime;

    // TODO: Replace these constructors with a builder to allow setting tokenLimit without setting
    //       outputFreq
    public TimingServerComm(
            final Consumer<ResultWrapper> consumer, Consumer<String> onDisconnect, String serverURL,
            Application application, int tokenLimit, final int outputFreq) {
        super(onDisconnect, tokenLimit, application);

        this.receivedTimestamps = new LongSparseArray<>();

        Consumer<ResultWrapper> timingConsumer = new Consumer<ResultWrapper>() {
            @Override
            public void accept(ResultWrapper resultWrapper) {
                consumer.accept(resultWrapper);
                long timestamp = SystemClock.elapsedRealtime();
                TimingServerComm.this.receivedTimestamps.put(resultWrapper.getFrameId(), timestamp);
                TimingServerComm.this.count++;
                TimingServerComm.this.intervalCount++;

                if (outputFreq > 0 && (TimingServerComm.this.count % outputFreq == 0)) {
                    long startTime = TimingServerComm.this.startTime;
                    double overallFps = (double)TimingServerComm.this.count /
                            ((timestamp - startTime) / 1000.0);
                    Log.i(TAG, "Overall FPS: " + overallFps);

                    long intervalCount = TimingServerComm.this.intervalCount;
                    long intervalStartTime = TimingServerComm.this.intervalStartTime;
                    double intervalFps = (double)intervalCount /
                            ((timestamp - intervalStartTime) / 1000.0);
                    Log.i(TAG, "Interval FPS: " + intervalFps);

                    TimingServerComm.this.intervalCount = 0;
                    TimingServerComm.this.intervalStartTime = SystemClock.elapsedRealtime();
                }
            }
        };

        ResultObserver resultObserver = new ResultObserver(
                this.tokenManager, timingConsumer, this.onErrorResult);
        this.timingSocketWrapper = new TimingSocketWrapper(
                serverURL, application, this.lifecycleRegistry, resultObserver, this.eventObserver);
        this.socketWrapper = this.timingSocketWrapper;

        this.count = 0;
        this.intervalCount = 0;
        this.startTime = SystemClock.elapsedRealtime();
        this.intervalStartTime = this.startTime;
    }

    public TimingServerComm(
            Consumer<ResultWrapper> consumer, Consumer<String> onDisconnect, String serverURL,
            Application application, int tokenLimit) {
        this(consumer, onDisconnect, serverURL, application, tokenLimit,
                TimingServerComm.DEFAULT_OUTPUT_FREQ);
    }

    public TimingServerComm(Consumer<ResultWrapper> consumer, Consumer<String> onDisconnect,
                            String serverURL, Application application) {
        this(consumer, onDisconnect, serverURL, application, Integer.MAX_VALUE);
    }

    public void logAvgRtt() {
        long count = 0;
        long totalRtt = 0;

        LongSparseArray<Long> sentTimestamps = this.timingSocketWrapper.getSentTimestamps();
        for (int i = 0; i < sentTimestamps.size(); i++) {
             long frameId = sentTimestamps.keyAt(i);
             Long sentTimestamp = sentTimestamps.valueAt(i);

             Long receivedTimestamp = this.receivedTimestamps.get(frameId);
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
