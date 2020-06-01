package edu.cmu.cs.gabriel.client.comm;


import android.app.Application;
import android.util.Log;
import android.util.LongSparseArray;

import edu.cmu.cs.gabriel.client.observer.EventObserver;
import edu.cmu.cs.gabriel.client.observer.ResultObserver;
import edu.cmu.cs.gabriel.client.socket.TimingSocketWrapper;
import edu.cmu.cs.gabriel.client.function.Consumer;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;

// Additions for measurement collection
import android.annotation.SuppressLint;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import androidx.annotation.RequiresApi;
import edu.cmu.cs.cliostore.InfluxDBHelper;

@RequiresApi(api = Build.VERSION_CODES.O)
public class TimingServerComm extends ServerCommCore {
    private final static String TAG = "TimingServerComm";
    private final static int DEFAULT_OUTPUT_FREQ = 10;

    private LongSparseArray<Long> receivedTimestamps;
    private TimingSocketWrapper timingSocketWrapper;
    private long count;
    private long intervalCount;
    private long startTime;
    private long intervalStartTime;

    // Measurement collection helpers
    private static LocationHelper locHelper = new LocationHelper();
    private static InfluxDBHelper influxhelper = new InfluxDBHelper();

    private static String MSGTAG = "";
    private static final String dl = "|";

    // TODO: Replace these constructors with a builder to allow setting tokenLimit without setting
    //       outputFreq
    @SuppressLint("MissingPermission")
    public TimingServerComm(final Consumer<ResultWrapper> consumer, Runnable onDisconnect,
                            final String serverURL, final ConnectivityManager conMan, final LocationManager locMan, Application application,
                            int tokenLimit, final int outputFreq) {
        super(tokenLimit);

        this.receivedTimestamps = new LongSparseArray<>();
//        this.influxhelper.listDB();
//        this.influxhelper.dropMeasurementRecords("location");
//        this.influxhelper.dropMeasurementRecords("framerate");
//        this.influxhelper.dropMeasurementRecords("roundtriptime"); // Clear it out
        NetworkInfo active_network = conMan.getActiveNetworkInfo();
        String connect_type = active_network.getTypeName();

        // Create tag showing device, server and network data
        MSGTAG = String.format("|%s|%s|%s|%s|%s|",TAG,Build.MANUFACTURER,Build.MODEL,serverURL,connect_type);

        locMan.requestLocationUpdates("gps",1000, (float) 1, locHelper);

        Consumer<ResultWrapper> timingConsumer = new Consumer<ResultWrapper>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void accept(ResultWrapper resultWrapper) {
                consumer.accept(resultWrapper);

                // TODO: Change to java.time.Instant once we can stop supporting Google Glass
                //       Explorer Edition
                long timestamp = System.currentTimeMillis();

                TimingServerComm.this.receivedTimestamps.put(resultWrapper.getFrameId(), timestamp);
                TimingServerComm.this.count++;
                TimingServerComm.this.intervalCount++;

                if (outputFreq > 0 && (TimingServerComm.this.count % outputFreq == 0)) {
                    // Log GPS Data
                    String msg;
                    Location locationGPS = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (locationGPS != null) {
                        msg = String.format("Current GPS LONG/LAT: POINT(%f %f)",locationGPS.getLongitude(),locationGPS.getLatitude());
                        influxhelper.addPoint("location",Build.MANUFACTURER,Build.MODEL,serverURL, connect_type, locationGPS.getLongitude(),locationGPS.getLatitude());
                    } else {
                        msg = "Current GPS LAT/LNG: UNAVAILABLE";
                    }
                    Log.i(TimingServerComm.this.MSGTAG , msg);

                    // Log FPS data
                    long startTime = TimingServerComm.this.startTime;
                    double overallFps = TimingServerComm.this.round2((double)TimingServerComm.this.count /
                            ((timestamp - startTime) / 1000.0));

                    long intervalCount = TimingServerComm.this.intervalCount;
                    long intervalStartTime = TimingServerComm.this.intervalStartTime;
                    double intervalFps = TimingServerComm.this.round2((double)intervalCount /
                            ((timestamp - intervalStartTime) / 1000.0));
                    Log.i(TimingServerComm.this.MSGTAG,"IntervalFPS: "  + intervalFps + " Overall FPS: " + overallFps) ;
                    influxhelper.addPoint("framerate",Build.MANUFACTURER,Build.MODEL,serverURL, connect_type, overallFps,intervalFps);

                    // Log Round Trip Time (RTT) Data
                    double[] artt = TimingServerComm.this.logAvgRtt();
                    Log.i(TimingServerComm.this.MSGTAG , "OverallRTT(ms): " +  artt[0] + " IntervalRTT(ms): "  + artt[1]);
                    influxhelper.addPoint("roundtriptime",Build.MANUFACTURER,Build.MODEL,serverURL, connect_type, artt[0],artt[1]);

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
                serverURL, application, resultObserver, eventObserver);
        this.socketWrapper = this.timingSocketWrapper;

        this.count = 0;
        this.intervalCount = 0;

        // TODO: Change to java.time.Instant once we can stop supporting Google Glass Explorer
        //       Edition
        this.startTime = System.currentTimeMillis();

        this.intervalStartTime = this.startTime;
    }

    public TimingServerComm(final Consumer<ResultWrapper> consumer, Runnable onDisconnect,
                            String serverURL, ConnectivityManager conMan, LocationManager locMan, Application application,
                            int tokenLimit) {
        this(consumer, onDisconnect, serverURL, conMan, locMan, application, tokenLimit,
                TimingServerComm.DEFAULT_OUTPUT_FREQ);
    }

    public TimingServerComm(final Consumer<ResultWrapper> consumer, Runnable onDisconnect,
                            String serverURL, ConnectivityManager conMan, LocationManager locMan, Application application) {
        this(consumer, onDisconnect, serverURL, conMan, locMan, application, Integer.MAX_VALUE);
    }

    public double[] logAvgRtt() {
        long count = 0;
        long totalRtt = 0;

        long intervalRtt = 0;
        // Average since start
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
        // Just do the last inteval's frames
        for (int i = (sentTimestamps.size() - DEFAULT_OUTPUT_FREQ); i < sentTimestamps.size(); i++) {
            long frameId = sentTimestamps.keyAt(i);
            Long sentTimestamp = sentTimestamps.valueAt(i);

            Long receivedTimestamp = receivedTimestamps.get(frameId);
            if (receivedTimestamp == null) {
                Log.e(TAG, "Frame with ID " + frameId + " never received");
            } else {
                intervalRtt += (receivedTimestamp - sentTimestamp);
            }
        }
        double avgRTT = this.round2((double)totalRtt / count);
        double intRTT = this.round2((double)intervalRtt / DEFAULT_OUTPUT_FREQ) ;
        double[] retmat = new double[2];retmat[0] = avgRTT;retmat[1] = intRTT;
        return retmat;
    }

    private double round2(double inval) {
        return(Math.round(inval*100)/100);
    }
    public void clearTimestamps() {
        this.timingSocketWrapper.clearSentTimestamps();
        this.receivedTimestamps.clear();
    }
}
