package com.mozilla.telemetry.ingestion.sink.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.mozilla.telemetry.ingestion.core.util.Json;
import java.io.IOException;
import java.io.UncheckedIOException;

public class StringToPubsubMessage {

  public static PubsubMessage apply(String line) {
    try {
      ObjectNode node = Json.readObjectNode(line);
      PubsubMessage.Builder messageBuilder = PubsubMessage.newBuilder();
      JsonNode dataNode = node.path("payload");
      if (!dataNode.isNull()) {
        final String data;
        if (dataNode.isTextual()) {
          data = dataNode.asText();
        } else {
          data = Json.asString(dataNode);
        }
        messageBuilder.setData(ByteString.copyFromUtf8(data));
      }
      JsonNode attributeNode = node.path("attributeMap");
      if (attributeNode.isObject()) {
        attributeNode.fields().forEachRemaining(entry -> messageBuilder.putAttributes(entry.getKey(), entry.getValue().asText()));
      }
      return messageBuilder.build();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
