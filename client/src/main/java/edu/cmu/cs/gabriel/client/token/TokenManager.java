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
    private boolean receivedWelcomeMessage;

    public TokenManager(int tokenLimit) {
        this.tokenCounter = new HashMap<>();
        this.running = false;
        this.tokenLimit = tokenLimit;
        this.receivedWelcomeMessage = false;
    }

    public synchronized void setTokensFromWelcomeMessage(WelcomeMessage welcomeMessage) {
        int numTokens = Math.min(welcomeMessage.getNumTokensPerFilter(), this.tokenLimit);

        this.tokenCounter.clear();
        for (String filterName : welcomeMessage.getFiltersConsumedList()) {
            this.tokenCounter.put(filterName, numTokens);
        }
        this.receivedWelcomeMessage = true;
        this.notify();

        Log.i(TAG, "numTokens update from welcome message");
    }

    /**
     * Check if the server has an engine that consumes frames that passed filterName.
     *
     * @param filterName
     * @return False if filter does not consume frames for filterName, or if we ran into an error
     */
    public synchronized boolean tokensForFilter(String filterName) {
        if (!this.waitForWelcomeMessage()) {
            return false;
        }
        return this.tokenCounter.containsKey(filterName);
    }

    private synchronized boolean waitForWelcomeMessage() {
        while (!this.receivedWelcomeMessage) {
            Log.i(TAG, "Waiting for welcome message.");
            try {
                this.wait();
            } catch(InterruptedException e){
                Log.e(TAG, "Error waiting for welcome message", e);
                return false;
            }
        }

        return true;
    }

    /** Take token if one is available. Otherwise return False. */
    public synchronized boolean getTokenNoWait(String filterName) {
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

    public synchronized void returnToken(String filterName) {
        assert this.tokenCounter.containsKey(filterName);

        int oldValue = this.tokenCounter.get(filterName);
        this.tokenCounter.put(filterName, oldValue + 1);
        this.notify();
    }

    /** Wait until there is a token available. Then take it. Return false if error. */
    public synchronized boolean getToken(String filterName) {
        if (!this.waitForWelcomeMessage()) {
            return false;
        }

        if (!this.tokenCounter.containsKey(filterName)) {
            Log.e(TAG, "No tokens for " + filterName);
            return false;
        }

        int oldValue = this.tokenCounter.get(filterName);
        while (oldValue < 1) {
            if (!this.running) {
                Log.i(TAG, "Not running. Will not wait for token");
                return false;
            }

            Log.d(TAG, "Too few tokens. Waiting for more.");
            try {
                this.wait();
            } catch(InterruptedException e) {
                Log.e(TAG, "Interrupted Exception while waiting for lock", e);
                return false;
            }

            oldValue = this.tokenCounter.get(filterName);
        }

        this.tokenCounter.put(filterName, oldValue - 1);
        return true;
    }

    public void start() {
        this.running = true;
    }

    public synchronized void stop() {
        this.running = false;
        // Clear all tokens so we have to get them from the server if we reconnect
        this.tokenCounter.clear();

        // This will allow a background handler to return if it is currently waiting on this
        this.notify();
    }

    public boolean isRunning() {
        return this.running;
    }
}
