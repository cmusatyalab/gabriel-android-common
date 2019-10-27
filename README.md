# gabriel-android-common

Android Library for communicating with Gabriel Server

## Usage

Add `implementation 'edu.cmu.cs.gabriel:client:0.1.5'` to your app's dependencies. 
Your project must include either the `jcenter()` or `mavenCentral()` repository. 

Extend the `edu.cmu.cs.gabriel.client.comm.ServerComm` class.
Send messages to the server using either the `sendNoWait`, `sendBlocking`, or 
`sendSupplier` methods.

## Publishing Changes to Maven Central

1. Update the VERSION_NAME parameter in `gradle.properties`. You can add
   `-SNAPSHOT` to the end of the version number to get a snapshot published
   instead.
2. Open the `Gradle` tab in the top right of Android studio.
3. Open `protocol/Tasks/upload`.
4. Run uploadArchives
5. Snapshots will be published to
   https://oss.sonatype.org/content/repositories/snapshots/edu/cmu/cs/gabriel/client/.
6. Relases should show up at https://oss.sonatype.org/#stagingRepositories.
   1. Publish a release by first clicking the `Close` button. Then click the
      `Release` button when it becomes available.
   2. Releases will be published to https://repo1.maven.org/maven2/edu/cmu/cs/gabriel/client/.
