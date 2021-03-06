package com.mozilla.telemetry.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class TestWithDeterministicJson {

  /** Make serialization of Map deterministic for testing. */
  @BeforeClass
  public static void enableOrderMapEntriesByKeys() {
    Json.enableOrderMapEntriesByKeys();
  }

  /** Reset the ObjectMapper configuration changed for testing. */
  @AfterClass
  public static void disableOrderMapEntriesByKeys() {
    Json.disableOrderMapEntriesByKeys();
  }

  /**
   * Reserialize {@code data} to make json deterministic.
   */
  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  public static String sortJSON(String data) {
    try {
      return Json.asString(Json.asMap(Json.readObjectNode(data.getBytes(StandardCharsets.UTF_8))));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
