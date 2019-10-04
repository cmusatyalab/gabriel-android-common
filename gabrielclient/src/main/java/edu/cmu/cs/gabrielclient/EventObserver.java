package edu.cmu.cs.gabrielclient;

import android.util.Log;

import com.tinder.scarlet.Stream.Observer;
import com.tinder.scarlet.WebSocket.Event;

public abstract class EventObserver implements Observer<Event> {
    private static String TAG = "EventObserver";

    protected abstract void onConnect();
    protected abstract void onDisconnect();

    @Override
    public void onNext(Event receivedUpdate) {
        if (!(receivedUpdate instanceof Event.OnMessageReceived)) {
            Log.i(TAG, receivedUpdate.toString());

            if (receivedUpdate instanceof Event.OnConnectionOpened) {
                this.onConnect();
            } else if (receivedUpdate instanceof Event.OnConnectionFailed) {
                this.onDisconnect();
            }

            // We do not check for Event.OnConnectionClosed because this is what gets sent when the
            // User presses the home button. Scarlet will automatically reconnect when a user
            // returns to the app.
        }
    }

    @Override
    public void onError(Throwable throwable) {
        Log.e(TAG, "event onError", throwable);

    }

    @Override
    public void onComplete() {
        Log.i(TAG, "event onComplete");

    }
}
