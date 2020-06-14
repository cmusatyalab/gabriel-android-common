package edu.cmu.cs.gabriel.client.observer;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.tinder.scarlet.Stream.Observer;

import org.jetbrains.annotations.NotNull;

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

    @SuppressLint("Assert")
    @Override
    public void onNext(byte[] rawToClient) {
        ToClient toClient;
        try {
            toClient = ToClient.parseFrom(rawToClient);
        } catch (InvalidProtocolBufferException e) {
            Log.e(TAG, "Failed to parse ToClient", e);
            return;
        }

        switch (toClient.getWelcomeOrResultCase()) {
            case WELCOME_MESSAGE:
                this.tokenManager.setTokensFromWelcomeMessage(toClient.getWelcomeMessage());

                assert !toClient.getReturnToken();
                return;
            case RESULT_WRAPPER:
                ResultWrapper resultWrapper = toClient.getResultWrapper();
                if (toClient.getReturnToken()) {
                    this.tokenManager.returnToken(resultWrapper.getFilterPassed());
                }

                if (resultWrapper.getStatus() == 
                    ResultWrapper.Status.NO_ENGINE_FOR_FILTER_PASSED) {
                    throw new RuntimeException("No engine for Filter Passed");
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
                return;
            case WELCOMEORRESULT_NOT_SET:
                throw new RuntimeException("Server sent empty message");
            default:
                throw new IllegalStateException("Unexpected value: " + toClient.getWelcomeOrResultCase());
        }
    }

    @Override
    public void onError(@NotNull Throwable throwable) {
        Log.e(TAG, "onError", throwable);
    }

    @Override
    public void onComplete() {
        Log.i(TAG, "onComplete");
    }
}
