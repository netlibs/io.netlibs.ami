package io.netlibs.ami.client;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import io.netlibs.ami.api.AmiChannelStatusMessage;
import io.netlibs.ami.api.AmiFrame;
import io.netlibs.ami.api.AmiMessage;
import io.netlibs.ami.netty.DefaultAmiFrame;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.processors.MulticastProcessor;

public class AmiConnectionHandler {

  private final NettyAmiTransport transport;
  private final Optional<AmiCredentials> credentials;
  private final @NonNull Disposable disposer;

  // for keeping counter.
  private final AtomicLong actionIdCounter = new AtomicLong(0);

  private final @NonNull MulticastProcessor<AmiMessage> events = MulticastProcessor.create();

  private AmiConnectionHandler(NettyAmiTransport ch, AmiCredentials credentials) {
    this.transport = ch;
    this.credentials = Optional.ofNullable(credentials);
    this.disposer = ch.subscribe(this::rx, this::rxerr, this::rxend);
    this.events.start();
  }

  public void rx(AmiMessage msg) {

    if (msg instanceof AmiChannelStatusMessage.Connected) {
      // send Login frame on connection.
      this.credentials.ifPresent(credentials -> {
        DefaultAmiFrame loginFrame = DefaultAmiFrame.newFrame();
        loginFrame.add("Action", "Login");
        loginFrame.add("ActionID", Long.toHexString(this.actionIdCounter.getAndIncrement()));
        loginFrame.add("Username", credentials.username());
        loginFrame.add("Secret", credentials.secret());
        this.transport.writeAndFlush(loginFrame);
      });
    }

    else if (msg instanceof AmiFrame) {

      rxframe(AmiFrame.class.cast(msg));

    }

  }

  private void rxframe(AmiFrame frame) {

    if (frame.contains("Response")) {

      CharSequence actionIdString = frame.get("ActionID");

      if (actionIdString != null) {
        long actionId = Long.parseUnsignedLong(actionIdString.toString(), 16);
        rxresponse(actionId, frame);
        return;
      }

    }
    events.onNext(frame);
  }

  /**
   * response from a previous action request.
   * 
   * @param actionId
   * @param frame
   */

  private void rxresponse(long actionId, AmiFrame frame) {

  }

  public void subscribe(Consumer<? super AmiMessage> onNext, Consumer<? super Throwable> onError, Runnable onComplete) {
    this.events.subscribe(onNext::accept, onError::accept, onComplete::run);
  }

  public void rxerr(Throwable t) {
    this.events.onError(t);
  }

  private void rxend() {
    this.events.onComplete();
  }

  public static AmiConnectionHandler create(NettyAmiTransport ch, AmiCredentials credentials) {
    return new AmiConnectionHandler(ch, credentials);
  }

  public void subscribeWith(BehaviorProcessor<AmiMessage> subscriber) {
    this.events.subscribeWith(subscriber);
  }

}
