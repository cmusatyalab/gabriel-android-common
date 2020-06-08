package edu.cmu.cs.gabriel.client.observer;

import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.tinder.scarlet.Stream.Observer;

import edu.cmu.cs.gabriel.client.function.Consumer;
import edu.cmu.cs.gabriel.client.token.TokenManager;
import edu.cmu.cs.gabriel.protocol.Protos.ToClient;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;

public class ResultObserver implements Observer<byte[]>  {
    private String TAG = "ResultObserver";

    private TokenManager tokenManager;
    private Consumer<ResultWrapper> consumer;

    public ResultObserver(TokenManager tokenManager, Consumer<ResultWrapper> consumer) {
        this.tokenManager = tokenManager;
        this.consumer = consumer;
    }

    @Override
    public void onNext(byte[] rawToClient) {
        ToClient toClient;
        try {
            toClient = ToClient.parseFrom(rawToClient);
        } catch (InvalidProtocolBufferException e) {
            Log.e(TAG, "Failed to parse ToClient", e);
            return;
        }

        if (toClient.hasWelcomeMessage()) {
            int numTokens = toClient.getWelcomeMessage().getNumTokensPerFilter();
            for (String filterName : toClient.getWelcomeMessage().getFiltersConsumedList()) {
                this.tokenManager.setNumTokens(numTokens, filterName);
            }

            assert !toClient.getReturnToken();
        } else if (toClient.hasResultWrapper()) {
            ResultWrapper resultWrapper = toClient.getResultWrapper();
            if (toClient.getReturnToken()) {
                this.tokenManager.returnToken(resultWrapper.getFilterPassed());
            }

            if (resultWrapper.getStatus() != ResultWrapper.Status.SUCCESS) {
                Log.e(TAG, "Output status was: " + resultWrapper.getStatus().name());
                return;
            }

            try {
                consumer.accept(resultWrapper);
            } catch (Exception e) {
                Log.e(TAG, "Consumer threw exception.", e);
            }
        } else {
            throw new RuntimeException("Server sent empty message");
        }
    }

    @Override
    public void onError(Throwable throwable) {
        Log.e(TAG, "onError", throwable);
    }

    @Override
    public void onComplete() {
        Log.i(TAG, "onComplete");
    }
}
