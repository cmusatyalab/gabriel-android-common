# Gabriel Android Library

Android library for communicating with a [Gabriel Server](https://github.com/cmusatyalab/gabriel-server-common).

## Usage

Add the lines `implementation 'edu.cmu.cs.gabriel:client:0.1.9'` and `implementation 'edu.cmu.cs.gabriel:protocol:0.1.15'` to your app's build.gradle file.
Your project must include either the `jcenter()`repository  or the `mavenCentral()` repository. 

Your app must allow cleartext traffic. If your app does not have an Android Network Security 
Config, you must sepcify `android:usesCleartextTraffic="true"` in the [application element](https://developer.android.com/guide/topics/manifest/application-element) of your Manifest file. 
If your app has an Android Network Security Config, you must allow cleartext traffic using this
config. See [here](https://developer.android.com/guide/topics/manifest/application-element#usesCleartextTraffic) for more details.

Create a `edu.cmu.cs.gabriel.client.comm.ServerComm` instance.
Send messages to the server using either the `sendNoWait`, `sendBlocking`, or 
`sendSupplier` methods. See [here](https://github.com/cmusatyalab/openrtist/blob/68958fe4393599de567bd4f1917480610e644bed/android-client/app/src/main/java/edu/cmu/cs/gabriel/network/BaseComm.java#L128) for an example.

## Publishing Changes to Maven Central

1. Update the VERSION_NAME parameter in `gradle.properties`. You can add
   `-SNAPSHOT` to the end of the version number to get a snapshot published
   instead.
2. Open the `Gradle` tab in the top right of Android studio.
3. Open `gabriel-android-common/client/Tasks/upload`.
4. Run uploadArchives
5. Snapshots will be published to
   https://oss.sonatype.org/content/repositories/snapshots/edu/cmu/cs/gabriel/client/.
6. Relases should show up at https://oss.sonatype.org/#stagingRepositories.
   1. Publish a release by first clicking the `Close` button. Then click the
      `Release` button when it becomes available.
   2. Releases will be published to https://repo1.maven.org/maven2/edu/cmu/cs/gabriel/client/.
