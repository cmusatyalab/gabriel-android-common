# gabriel-android-common

Android Library for communicating with Gabriel Server

## Usage

Add `implementation 'edu.cmu.cs:gabrielclient:0.0.1'` to your app's dependencies. 
Your project must include either the `jcenter()` or `mavenCentral()` repository. 

Extend the `edu.cmu.cs.gabrielclient.ServerComm` class. 
Send messages to the server using either the `sendNoWait`, `sendBlocking`, or 
`sendSupplier` methods.
