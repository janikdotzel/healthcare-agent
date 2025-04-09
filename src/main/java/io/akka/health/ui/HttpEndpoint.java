package io.akka.health.ui;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.http.HttpResponses;

/**
 * This Http endpoint return the static UI page located under src/main/resources/static-resources/
 */
@akka.javasdk.annotations.http.HttpEndpoint
@Acl(allow = @Acl.Matcher(principal = Acl.Principal.ALL))
public class HttpEndpoint {
  @Get("/")
  public HttpResponse index() {
    return HttpResponses.staticResource("index.html");
  }
}
