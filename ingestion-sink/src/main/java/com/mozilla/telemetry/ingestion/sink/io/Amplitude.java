package com.mozilla.telemetry.ingestion.sink.io;

import com.google.common.annotations.VisibleForTesting;
import com.google.pubsub.v1.PubsubMessage;
import com.mozilla.telemetry.ingestion.core.Constant.Attribute;
import com.mozilla.telemetry.ingestion.sink.transform.PubsubMessageToTemplatedString;
import com.mozilla.telemetry.ingestion.sink.util.BatchWrite;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Amplitude {

  public static class Delete extends BatchWrite<PubsubMessage, String, String, Void> {

    // TODO this should be an amplitude service provider, so that tests can mock requests
    private final Object amplitude;

    /** Constructor. */
    public Delete(Object amplitude, long maxBytes, int maxMessages, Duration maxDelay,
        PubsubMessageToTemplatedString batchKeyTemplate) {
      super(maxBytes, maxMessages, maxDelay, batchKeyTemplate);
      this.amplitude = amplitude;
    }

    @Override
    protected String encodeInput(PubsubMessage input) {
      return input.getAttributesOrThrow(Attribute.CLIENT_ID);
    }

    @Override
    protected String getBatchKey(PubsubMessage input) {
      return batchKeyTemplate.apply(input);
    }

    @Override
    protected Batch getBatch(String destination) {
      return new Batch(destination);
    }

    @VisibleForTesting
    class Batch extends BatchWrite<PubsubMessage, String, String, Void>.Batch {

      private final String destination;
      private final List<String> deleteIds;

      private Batch(String batchKey) {
        super();
        this.destination = batchKey;
        this.deleteIds = new ArrayList<>();
      }

      @Override
      protected synchronized CompletableFuture<Void> close(Void ignore) {
        // TODO write to amplitude and surface any exceptions, if a single id in the request can
        // fail make sure to only to fail relevant messages, as seen in BigQuery.Write.
        throw new RuntimeException(
            "Amplitude deletes not implemented, could not request deletion of: " + deleteIds
                + " from " + destination);
      }

      @Override
      protected synchronized void write(String id) {
        deleteIds.add(id);
      }

      @Override
      protected long getByteSize(String id) {
        return id.length();
      }
    }
  }
}
