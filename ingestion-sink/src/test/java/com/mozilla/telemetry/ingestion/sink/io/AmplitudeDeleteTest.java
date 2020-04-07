package com.mozilla.telemetry.ingestion.sink.io;

import com.google.pubsub.v1.PubsubMessage;
import com.mozilla.telemetry.ingestion.sink.transform.PubsubMessageToTemplatedString;
import java.time.Duration;
import org.junit.Test;

public class AmplitudeDeleteTest {

  private static final Object amplitudeMock = null;
  private static final Amplitude.Delete OUTPUT = new Amplitude.Delete(amplitudeMock, 0, 0,
      Duration.ZERO, PubsubMessageToTemplatedString.of("${document_namespace}"));

  @Test
  public void canReturnSuccess() {
    PubsubMessage input = PubsubMessage.newBuilder().putAttributes("client_id", "x")
        .putAttributes("document_namespace", "telemetry").build();
    OUTPUT.apply(input).join();
    // TODO check that amplitudeMock received deletion request for "x";
  }
}
