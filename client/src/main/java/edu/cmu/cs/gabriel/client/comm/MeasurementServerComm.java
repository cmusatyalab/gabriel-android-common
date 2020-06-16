package edu.cmu.cs.gabriel.client.comm;

import android.app.Application;
import android.os.SystemClock;
import android.util.Log;
import android.util.LongSparseArray;

import edu.cmu.cs.gabriel.client.observer.ResultObserver;
import edu.cmu.cs.gabriel.client.socket.MeasurementSocketWrapper;
import edu.cmu.cs.gabriel.client.function.Consumer;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;

public class MeasurementServerComm extends ServerCommCore {
    private static final String TAG = "MeasurementServerComm";
    private final static int DEFAULT_OUTPUT_FREQ = 10;

    private LongSparseArray<Long> receivedTimestamps;
    private MeasurementSocketWrapper measurementSocketWrapper;
    private int count;  // LongSparseArray#size returns an int
    private long startTime;
    private long intervalStartTime;

    // TODO: Replace these constructors with a builder to allow setting tokenLimit without setting
    //       outputFreq
    public MeasurementServerComm(
            final Consumer<ResultWrapper> consumer, Consumer<String> onDisconnect, String serverURL,
            Application application, final Consumer<RttFps> intervalReporter, int tokenLimit,
            final int outputFreq) {
        super(onDisconnect, tokenLimit, application);

        this.receivedTimestamps = new LongSparseArray<>();

        Consumer<ResultWrapper> timingConsumer = new Consumer<ResultWrapper>() {
            @Override
            public void accept(ResultWrapper resultWrapper) {
                long currentTime = SystemClock.elapsedRealtime();
                consumer.accept(resultWrapper);
                long frameId = resultWrapper.getFrameId();
                MeasurementServerComm.this.receivedTimestamps.put(frameId, currentTime);
                MeasurementServerComm.this.count++;

                if (outputFreq > 0 && (MeasurementServerComm.this.count % outputFreq == 0)) {
                    double intervalRtt = MeasurementServerComm.this.computeRtt(
                            MeasurementServerComm.this.count - outputFreq);
                    double intervalFps = MeasurementServerComm.this.computeFps(
                            outputFreq, currentTime, MeasurementServerComm.this.intervalStartTime);
                    intervalReporter.accept(new RttFps(intervalRtt, intervalFps));

                    MeasurementServerComm.this.intervalStartTime = SystemClock.elapsedRealtime();
                }
            }
        };

        ResultObserver resultObserver = new ResultObserver(
                this.tokenManager, timingConsumer, this.onErrorResult);
        this.measurementSocketWrapper = new MeasurementSocketWrapper(
                serverURL, application, this.lifecycleRegistry, resultObserver, this.eventObserver);
        this.socketWrapper = this.measurementSocketWrapper;

        this.zeroCountTimesNow();
    }

    public MeasurementServerComm(
            Consumer<ResultWrapper> consumer, Consumer<String> onDisconnect, String serverURL,
            Application application, Consumer<RttFps> intervalReporter, int tokenLimit) {
        this(consumer, onDisconnect, serverURL, application, intervalReporter, tokenLimit,
                MeasurementServerComm.DEFAULT_OUTPUT_FREQ);
    }

    public MeasurementServerComm(
            Consumer<ResultWrapper> consumer, Consumer<String> onDisconnect, String serverURL,
            Application application, Consumer<RttFps> intervalReporter) {
        this(consumer, onDisconnect, serverURL, application, intervalReporter, Integer.MAX_VALUE);
    }

    private void zeroCountTimesNow() {
        this.count = 0;
        this.startTime = SystemClock.elapsedRealtime();
        this.intervalStartTime = this.startTime;
    }

    private double computeFps(long count, long currentTime, long startTime) {
        return (double)count / ((currentTime - startTime) / 1000.0);
    }

    private double computeRtt(int startingFrame) {
        long count = 0;
        long totalRtt = 0;

        LongSparseArray<Long> sentTimestamps = this.measurementSocketWrapper.getSentTimestamps();
        for (int i = startingFrame; i < sentTimestamps.size(); i++) {
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
        return ((double)totalRtt) / count;
    }

    public double getOverallAvgRtt() {
        return this.computeRtt(0);
    }

    public double getOverallFps() {
        return computeFps(MeasurementServerComm.this.count, SystemClock.elapsedRealtime(),
                MeasurementServerComm.this.startTime);
    }

    public void clearMeasurements() {
        this.measurementSocketWrapper.clearSentTimestamps();
        this.receivedTimestamps.clear();
        this.zeroCountTimesNow();
    }
}
