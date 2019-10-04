package edu.cmu.cs.gabrielclient;

import com.tinder.scarlet.Stream;
import com.tinder.scarlet.WebSocket.Event;
import com.tinder.scarlet.ws.Receive;
import com.tinder.scarlet.ws.Send;

import edu.cmu.cs.gabrielclient.Protos.FromClient;
import edu.cmu.cs.gabrielclient.Protos.ToClient;

interface GabrielSocket {
    @Send
    void Send(FromClient fromClient);

    @Receive
    Stream<ToClient> Receive();

    @Receive
    Stream<Event> observeWebSocketEvent();
}