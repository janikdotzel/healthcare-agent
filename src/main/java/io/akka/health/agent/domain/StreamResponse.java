package io.akka.health.agent.domain;

public record StreamResponse(String content, int inputTokens, int outputTokens, boolean finished) {

  public static StreamResponse partial(String content) {
    return new StreamResponse(content, 0, 0, false);
  }

  public static StreamResponse lastMessage(String content, int inputTokens, int outputTokens) {
    return new StreamResponse(content, inputTokens, outputTokens, true);
  }

  public static StreamResponse empty() {
    return new StreamResponse("", 0, 0, true);
  }
}
