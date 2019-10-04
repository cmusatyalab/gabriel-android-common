package edu.cmu.cs.gabrielclient;

import android.util.Log;

import com.tinder.scarlet.Stream.Observer;
import edu.cmu.cs.gabrielclient.Protos.ToClient;
import edu.cmu.cs.gabrielclient.Protos.ResultWrapper;

public abstract class ResultObserver implements Observer<ToClient>  {
    private String TAG = "ResultObserver";

    protected abstract void handleResults(ResultWrapper resultWrapper);
    protected abstract void updateNumTokens(int numTokens);
    protected abstract void returnToken();

    @Override
    public void onNext(ToClient toClient) {
        if (toClient.hasResultWrapper()) {
            Protos.ResultWrapper resultWrapper = toClient.getResultWrapper();
            if (resultWrapper.getStatus() == Protos.ResultWrapper.Status.SUCCESS) {
                this.handleResults(resultWrapper);
            } else {
                Log.e(TAG, "Output status was: " + resultWrapper.getStatus().name());
            }

            this.returnToken();
        } else {
            this.updateNumTokens(toClient.getNumTokens());
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
