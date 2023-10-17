package io.netlibs.asterisk.ari.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AriTransport {

  private static final Logger LOG = LoggerFactory.getLogger(AriTransport.class);

  private final String baseUri;
  private final HttpClient httpClient;

  public AriTransport(final HttpClient httpClient, final String baseUri) {
    this.httpClient = httpClient;
    this.baseUri = baseUri;
  }

  public <T> T get(final List<String> path, final Class<T> bodyType) throws InterruptedException {

    final HttpRequest request =
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, path, Map.of()))
        .GET()
        .build();

    try {

      final HttpResponse<T> res = this.httpClient.send(request, HttpUtils.asJSONHandler(HttpUtils.jsonMapper(), bodyType));

      if ((res.statusCode() / 100) != 2) {
        throw new IllegalStateException(String.format("Unexpected HTTP status code %d", res.statusCode()));
      }

      return res.body();

    }
    catch (final IOException ex) {
      throw new UncheckedIOException(ex);
    }

  }

  public void delete(final List<String> path) throws InterruptedException {
    this.delete(path, Map.of());
  }

  public void delete(final List<String> path, final Map<String, String> args) throws InterruptedException {

    final HttpRequest request =
      HttpRequest.newBuilder()
        .uri(Utils.makeUri(this.baseUri, path, args))
        .DELETE()
        .build();

    HttpResponse<Void> res;

    try {
      LOG.info("<<< {} ? {}", path, args);
      res = this.httpClient.send(request, BodyHandlers.discarding());
    }
    catch (final IOException ex) {
      throw new UncheckedIOException(ex);
    }

    if ((res.statusCode() / 100) != 2) {
      throw new IllegalStateException(String.format("Unecpected HTTP status code %d", res.statusCode()));
    }

  }

  public void post(final List<String> path) throws InterruptedException {
    this.post(Utils.makeUri(this.baseUri, path, Map.of()));
  }

  public void post(final URI uri) throws InterruptedException {

    final HttpRequest request =
      HttpRequest.newBuilder()
        .uri(uri)
        .POST(BodyPublishers.noBody())
        .build();

    HttpResponse<Void> res;

    try {
      LOG.info("<<< {}", uri);
      res = this.httpClient.send(request, BodyHandlers.discarding());
    }
    catch (final IOException ex) {
      throw new UncheckedIOException(ex);
    }

    if ((res.statusCode() / 100) != 2) {
      throw new IllegalStateException(String.format("Unecpected HTTP status code %d", res.statusCode()));
    }

  }

}
