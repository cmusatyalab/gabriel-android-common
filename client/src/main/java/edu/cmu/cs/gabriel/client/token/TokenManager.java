package edu.cmu.cs.gabriel.client.token;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import edu.cmu.cs.gabriel.protocol.Protos.ToClient.WelcomeMessage;

public class TokenManager {
    private static String TAG = "TokenManager";

    private Map<String, Integer> tokenCounter;
    private boolean running;
    private int tokenLimit;

    public TokenManager(int tokenLimit) {
        this.tokenCounter = new HashMap<>();
        this.running = false;
        this.tokenLimit = tokenLimit;
    }

    public void setTokensFromWelcomeMessage(WelcomeMessage welcomeMessage) {
        int numTokens = Math.min(welcomeMessage.getNumTokensPerFilter(), this.tokenLimit);

        synchronized (this.tokenCounter) {
            this.tokenCounter.clear();
            for (String filterName : welcomeMessage.getFiltersConsumedList()) {
                this.tokenCounter.put(filterName, numTokens);
            }
            this.tokenCounter.notify();
        }

        Log.i(TAG, "numTokens update from welcome message");
    }

    /** Take token if one is available. Otherwise return False. */
    public boolean getTokenNoWait(String filterName) {
        synchronized (this.tokenCounter) {
            if (!this.tokenCounter.containsKey(filterName)) {
                return false;
            }

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
            assert this.tokenCounter.containsKey(filterName);

            int oldValue = this.tokenCounter.get(filterName);
            this.tokenCounter.put(filterName, oldValue + 1);
            this.tokenCounter.notify();
        }
    }

    /** Wait until there is a token available. Then take it. Return false if error. */
    public boolean getToken(String filterName) {
        synchronized (this.tokenCounter) {
            while (!this.tokenCounter.containsKey(filterName)) {
                Log.d(TAG, "No tokens for " + filterName + ". Waiting for server to give " +
                        "some.");
                try {
                    this.tokenCounter.wait();
                } catch(InterruptedException e){
                    Log.e(TAG, "Interrupted Exception while waiting for lock", e);
                    return false;
                }
            }

            int oldValue = this.tokenCounter.get(filterName);
            while (oldValue < 1) {
                if (!this.running) {
                    Log.i(TAG, "Not running. Will not wait for token");
                    return false;
                }

                Log.d(TAG, "Too few tokens. Waiting for more.");
                try {
                    this.tokenCounter.wait();
                } catch(InterruptedException e){
                    Log.e(TAG, "Interrupted Exception while waiting for lock", e);
                    return false;
                }

                oldValue = this.tokenCounter.get(filterName);
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
