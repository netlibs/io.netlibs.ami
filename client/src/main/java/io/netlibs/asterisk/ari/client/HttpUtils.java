package io.netlibs.asterisk.ari.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class HttpUtils {

  public static HttpClient defaultHttpClient() {
    return HttpClient.newBuilder()
      .proxy(ProxySelector.getDefault())
      .followRedirects(HttpClient.Redirect.NORMAL)
      .connectTimeout(Duration.ofSeconds(15))
      .build();
  }

  public static HttpClient defaultHttpClient(final Authenticator authenticator) {
    return HttpClient.newBuilder()
      .authenticator(authenticator)
      .proxy(ProxySelector.getDefault())
      .followRedirects(HttpClient.Redirect.NORMAL)
      .connectTimeout(Duration.ofSeconds(15))
      .build();
  }

  public static Authenticator passwordAuthenticator(final String username, final String password) {
    return new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password.toCharArray());
      }
    };
  }

  public static Optional<Authenticator> authenticatorForURI(final URI uri) {

    final String userinfo = uri.getUserInfo();

    if ((userinfo == null) || userinfo.isEmpty()) {
      return Optional.empty();
    }

    final String[] parts = userinfo.split(":", 2);

    if (parts.length == 1) {
      return Optional.of(passwordAuthenticator(parts[0], ""));
    }

    return Optional.of(passwordAuthenticator(parts[0], parts[1]));

  }

  public static HttpClient defaultHttpClient(final URI baseURI) {
    return authenticatorForURI(baseURI)
      .map(HttpUtils::defaultHttpClient)
      .orElseGet(HttpUtils::defaultHttpClient);
  }

  public static <W, R> BodyHandler<R> asJSONHandler(final ObjectMapper objectMapper, final Class<W> targetType, final Function<W, R> extractor) {
    return res -> BodySubscribers.mapping(
      BodySubscribers.ofString(StandardCharsets.UTF_8),
      body -> {
        try {
          if (res.statusCode() >= 300) {
            throw new RuntimeException(body);
          }
          return extractor.apply(objectMapper.readValue(body, targetType));
        }
        catch (final JsonProcessingException e) {
          // TODO Auto-generated catch block
          throw new RuntimeException(e);
        }
      });
  }

  public static <W> BodyHandler<W> asJSONHandler(final ObjectMapper objectMapper, final Class<W> targetType) {
    return res -> BodySubscribers.mapping(
      BodySubscribers.ofString(StandardCharsets.UTF_8),
      body -> {
        try {
          if (res.statusCode() >= 300) {
            throw new RuntimeException(body);
          }
          return objectMapper.readValue(body, targetType);
        }
        catch (final JsonProcessingException e) {
          // TODO Auto-generated catch block
          throw new RuntimeException(e);
        }
      });
  }

  public static <W> BodySubscriber<Supplier<W>> asJSON(final ObjectMapper objectMapper, final Class<W> targetType) {
    final BodySubscriber<InputStream> upstream = BodySubscribers.ofInputStream();
    final BodySubscriber<Supplier<W>> downstream =
      BodySubscribers.mapping(
        upstream,
        (final InputStream is) -> () -> {
          try (InputStream stream = is) {
            return objectMapper.readValue(stream, targetType);
          }
          catch (final IOException e) {
            throw new UncheckedIOException(e);
          }
        });
    return downstream;
  }

  public static ObjectMapper jsonMapper() {
    return mapper;
  }

  private static final ObjectMapper mapper = new ObjectMapper().registerModules(new Jdk8Module(), new JavaTimeModule());

}
