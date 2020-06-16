package edu.cmu.cs.gabriel.client.observer;

import android.util.Log;

import com.tinder.scarlet.Stream.Observer;
import com.tinder.scarlet.WebSocket.Event;

import edu.cmu.cs.gabriel.client.token.TokenManager;

public class EventObserver implements Observer<Event> {
    private static final String TAG = "EventObserver";

    private TokenManager tokenManager;
    private Runnable onConnectionProblem;

    public EventObserver(TokenManager tokenManager, Runnable onConnectionProblem) {
        this.tokenManager = tokenManager;
        this.onConnectionProblem = onConnectionProblem;
    }

    @Override
    public void onNext(Event receivedUpdate) {
        if (!(receivedUpdate instanceof Event.OnMessageReceived)) {
            Log.i(TAG, receivedUpdate.toString());

            if (receivedUpdate instanceof Event.OnConnectionOpened) {
                this.tokenManager.start();
            } else if (receivedUpdate instanceof Event.OnConnectionFailed) {
                this.onConnectionProblem.run();
                this.tokenManager.stop();
            }

            // We do not check for Event.OnConnectionClosed because this is what gets sent when the
            // User presses the home button. Scarlet will automatically reconnect when a user
            // returns to the app.
        }
    }

    @Override
    public void onError(Throwable throwable) {
        Log.e(TAG, "Event onError", throwable);

    }

    @Override
    public void onComplete() {
        Log.i(TAG, "Event onComplete");
    }
}
