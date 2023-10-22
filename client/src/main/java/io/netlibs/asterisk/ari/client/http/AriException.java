package io.netlibs.asterisk.ari.client.http;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@SuppressWarnings("serial")
public final class AriException extends RuntimeException {

  private final HttpRequest req;
  private final HttpResponse<?> res;

  AriException(final Exception ex) {
    this(ex, null);
  }

  AriException(final Exception ex, final HttpRequest req) {
    super(ex);
    this.req = req;
    this.res = null;
  }

  AriException(final String message, final HttpRequest req, final HttpResponse<?> res) {
    super(message);
    this.req = req;
    this.res = res;
  }

}
