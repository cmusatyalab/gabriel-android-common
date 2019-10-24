package edu.cmu.cs.gabriel.client;

import android.util.Log;

public class TokenManager {
    private static String TAG = "TokenManager";

    private volatile int numTokens;
    private final Object tokenLock;
    private boolean running;

    public TokenManager() {
        this.tokenLock = new Object();
        this.numTokens = 0;
        this.running = false;
    }

    public void setNumTokens (int numTokens) {
        synchronized (this.tokenLock) {
            this.numTokens = numTokens;
            this.tokenLock.notify();
        }

        Log.i(TAG, "numTokens is now " + numTokens);
    }

    /** Take token if one is available. Otherwise return False. */
    public boolean getTokenNoWait() {
        synchronized (this.tokenLock) {
            if (numTokens == 0) {
                return false;
            }

            numTokens--;
            return true;
        }
    }

    public void returnToken() {
        synchronized (this.tokenLock) {
            this.numTokens++;
            this.tokenLock.notify();
        }
    }

    /** Wait until there is a token available. Then take it. Return false if error. */
    public boolean getToken() {
        synchronized (this.tokenLock) {
            try {
                while (this.numTokens < 1) {
                    if (!this.running) {
                        Log.i(TAG, "Not running. Will not wait for token");
                        return false;
                    }

                    Log.d(TAG, "Too few tokens. Waiting for more.");
                    this.tokenLock.wait();
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted Exception while waiting for lock", e);
                return false;
            }

            this.numTokens--;
            return true;
        }
    }

    public void start() {
        this.running = true;
    }

    public void stop() {
        this.running = false;
        synchronized (this.tokenLock) {
            // Zero out tokens so we have to get them from the server if we reconnect
            this.numTokens = 0;

            // This will allow a background handler to return if it is currently waiting on
            // this.tokenLock
            this.tokenLock.notify();
        }
    }
}
