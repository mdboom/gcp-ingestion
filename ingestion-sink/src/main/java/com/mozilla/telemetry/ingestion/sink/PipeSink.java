package com.mozilla.telemetry.ingestion.sink;

import com.google.common.collect.ImmutableList;
import com.google.pubsub.v1.PubsubMessage;
import com.mozilla.telemetry.ingestion.core.util.Json;
import com.mozilla.telemetry.ingestion.sink.config.SinkConfig;
import com.mozilla.telemetry.ingestion.sink.io.Pipe;
import com.mozilla.telemetry.ingestion.sink.transform.StringToPubsubMessage;
import com.mozilla.telemetry.ingestion.sink.transform.PubsubMessageToObjectNode;
import com.mozilla.telemetry.ingestion.sink.util.Env;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

public class PipeSink {

  private PipeSink() {
  }

  /**
   * Run a sink from stdin to stdout.
   */
  public static void main(String[] args) {
    Pipe.Write output = Pipe.Write.of(System.out);
    PubsubMessageToObjectNode format = SinkConfig.getFormat(new Env(ImmutableList.of("OUTPUT_FORMAT", "STRICT_SCHEMA_DOCTYPES", "SCHEMAS_LOCATION")));
    Function<String, PubsubMessage> decoder = StringToPubsubMessage::apply;
    Pipe.Read.of(line -> output.apply(Json.asString(format.apply(decoder.apply(line))))).run();
  }
}
