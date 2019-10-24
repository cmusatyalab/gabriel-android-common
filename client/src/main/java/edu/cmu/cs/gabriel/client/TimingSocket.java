package edu.cmu.cs.gabriel.client;

import android.app.Application;
import android.util.LongSparseArray;

import com.tinder.scarlet.Lifecycle;
import com.tinder.scarlet.Stream;
import com.tinder.scarlet.WebSocket;

import edu.cmu.cs.gabriel.protocol.Protos.FromClient;
import edu.cmu.cs.gabriel.protocol.Protos.ToClient;

public class TimingSocket implements GabrielSocket {
    private GabrielSocket gabrielSocket;
    private LongSparseArray<Long> sendTimestamps;

    public TimingSocket(String serverIP, int port, Lifecycle lifecycle) {
        this.gabrielSocket = SocketWrapper.createSocket(serverIP, port, lifecycle);
        this.sendTimestamps = new LongSparseArray<>();
    }

    @Override
    public void Send(byte[] rawFromClient) {
        // TODO: change to java.time.Instant once we can stop supporting Google Glass Explorer
        //       Edition
        long timestamp = System.currentTimeMillis();

        this.gabrielSocket.Send(fromClient);
        this.sendTimestamps.put(fromClient.getFrameId(), timestamp);
    }

    @Override
    public Stream<ToClient> Receive() {
        return this.gabrielSocket.Receive();
    }

    @Override
    public Stream<WebSocket.Event> observeWebSocketEvent() {
        return this.gabrielSocket.observeWebSocketEvent();
    }

    public LongSparseArray<Long> getSendTimestamps() {
        return sendTimestamps;
    }

    public void clearSendTimestamps() {
        sendTimestamps.clear();
    }
}
