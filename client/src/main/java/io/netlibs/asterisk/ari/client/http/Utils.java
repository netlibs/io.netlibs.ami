package io.netlibs.asterisk.ari.client.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;

class Utils {

  private static final Escaper queryEscaper = new PercentEscaper("_-!.~'()*,;:$?/[]@+<>{}", true);

  static URI makeUri(final String path, final Map<String, String> query) {
    try {

      final String queryString =
        query.entrySet().stream().map(e -> queryEscaper.escape(e.getKey()) + "=" + queryEscaper.escape(e.getValue())).collect(Collectors.joining("&"));
      // log.debug("query string: {}", queryString);
      return new URI(
        null,
        null,
        path,
        queryString,
        null);
    }
    catch (final URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  static URI makeUri(final String baseUri, final List<String> path) {
    return makeUri(baseUri, path, Map.of());
  }

  static URI makeUri(final URI baseUri, final List<String> path) {
    return makeUri(baseUri, path, Map.of());
  }

  static URI makeUri(final String baseUri, final List<String> path, final Map<String, String> args) {

    final StringBuilder sb = new StringBuilder();

    sb.append(baseUri);

    for (final String part : path) {
      sb.append("/");
      sb.append(part);
    }

    return makeUri(sb.toString(), args);

  }

  static URI makeUri(final URI baseUri, final List<String> path, final Map<String, String> args) {
    return makeUri(baseUri.toASCIIString(), path, args);
  }

}
