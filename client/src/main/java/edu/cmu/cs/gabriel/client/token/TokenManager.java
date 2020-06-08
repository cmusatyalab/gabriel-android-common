package edu.cmu.cs.gabriel.client.token;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class TokenManager {
    private static String TAG = "TokenManager";

    private HashMap<String, Integer> tokenCounter;
    private boolean running;
    private int tokenLimit;

    public TokenManager(int tokenLimit) {
        this.tokenCounter = new HashMap<>();
        this.running = false;
        this.tokenLimit = tokenLimit;
    }

    public void setNumTokens (int numTokens, String filterName) {
        numTokens = Math.min(numTokens, this.tokenLimit);

        synchronized (this.tokenCounter) {
            this.tokenCounter.put(filterName, numTokens);
            this.tokenCounter.notify();
        }

        Log.i(TAG, "numTokens is now " + numTokens + " for filter " + filterName);
    }

    /** Take token if one is available. Otherwise return False. */
    public boolean getTokenNoWait(String filterName) {
        synchronized (this.tokenCounter) {
            int oldValue = this.tokenCounter.get(filterName);
            if (oldValue == 0) {
                return false;
            }

            this.tokenCounter.put(filterName, oldValue - 1);
            return true;
        }
    }

    public void returnToken(String filterName) {
        synchronized (this.tokenCounter) {
            int oldValue = this.tokenCounter.get(filterName);
            this.tokenCounter.put(filterName, oldValue + 1);
            this.tokenCounter.notify();
        }
    }

    /** Wait until there is a token available. Then take it. Return false if error. */
    public boolean getToken(String filterName) {
        synchronized (this.tokenCounter) {
            int oldValue = this.tokenCounter.get(filterName);
            while (oldValue < 1) {
                try {
                    if (!this.running) {
                        Log.i(TAG, "Not running. Will not wait for token");
                        return false;
                    }

                    Log.d(TAG, "Too few tokens. Waiting for more.");
                    this.tokenCounter.wait();

                    oldValue = this.tokenCounter.get(filterName);
                } catch(InterruptedException e){
                    Log.e(TAG, "Interrupted Exception while waiting for lock", e);
                    return false;
                }
            }

            this.tokenCounter.put(filterName, oldValue - 1);
            return true;
        }
    }

    public void start() {
        this.running = true;
    }

    public void stop() {
        this.running = false;
        synchronized (this.tokenCounter) {
            // Clear all tokens so we have to get them from the server if we reconnect
            this.tokenCounter.clear();

            // This will allow a background handler to return if it is currently waiting on
            // this.tokenCounter
            this.tokenCounter.notify();
        }
    }

    public boolean isRunning() {
        return this.running;
    }
}
