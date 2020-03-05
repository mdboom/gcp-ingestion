package com.mozilla.telemetry.ingestion.sink.io;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class Pipe {

  public static class Read<ResultT> {

    final Function<String, CompletableFuture<ResultT>> output;

    /** Constructor. */
    private Read(Function<String, CompletableFuture<ResultT>> output) {
      this.output = output;
    }

    public static <T> Read<T> of(Function<String, CompletableFuture<T>> output) {
      return new Read<>(output);
    }

    /** Read stdin until end of stream. */
    public void run() {
      BufferedReader in = new BufferedReader(
          new InputStreamReader(System.in, StandardCharsets.UTF_8));
      in.lines().map(output).forEach(CompletableFuture::join);
    }
  }

  public static class Write implements Function<String, CompletableFuture<Void>> {
    private final PrintStream pipe;

    private Write(PrintStream pipe) {
      this.pipe = pipe;
    }

    public static Write of(PrintStream pipe) {
      return new Write(pipe);
    }

    @Override
    public CompletableFuture<Void> apply(String message) {
      return CompletableFuture.completedFuture(message).thenAcceptAsync(pipe::println);
    }
  }
}
