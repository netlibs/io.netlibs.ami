package io.netlibs.ami.client;

import java.util.function.Consumer;

import javax.net.ssl.SSLException;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.google.common.net.HostAndPort;

import io.netlbs.ami.AmiFrame;
import io.netlbs.ami.AmiMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.processors.BehaviorProcessor;

public class NettyAmiTransport {

  private final HostAndPort target;
  private Channel channel;
  private final BehaviorProcessor<AmiMessage> incoming = BehaviorProcessor.create();

  private NettyAmiTransport(EventLoopGroup group, HostAndPort target, SslContext sslctx) {

    Bootstrap b = new Bootstrap();

    b.group(group)
      .channel(NioSocketChannel.class)
      .option(ChannelOption.TCP_NODELAY, true)
      .handler(new AmiClientHandler(incoming, sslctx));

    final ChannelFuture f = b.connect(target.getHost(), target.getPort());

    f.addListener(_f -> {

      try {
        // get the error if there is one.
        f.get();
        this.channel = f.channel();
        channel.closeFuture().addListener(_cf -> incoming.onComplete());
      }
      catch (Exception t) {
        incoming.onError(t);
        return;
      }

    });

    this.target = target;

  }

  public static NettyAmiTransport create(EventLoopGroup group, HostAndPort target) {
    return new NettyAmiTransport(group, target, null);
  }

  public static NettyAmiTransport create(EventLoopGroup group, HostAndPort target, SslContext sslctx) {
    return new NettyAmiTransport(group, target, sslctx);

  }

  public @NonNull
      Disposable subscribe(@NonNull Consumer<? super @NonNull AmiMessage> onNext, @NonNull Consumer<? super Throwable> onError, Runnable onComplete) {
    return incoming.subscribe(onNext::accept, onError::accept, onComplete::run);
  }

  public void writeAndFlush(AmiFrame frame) {
    this.channel.writeAndFlush(frame);
  }

  public static NettyAmiTransport createTls(EventLoopGroup group, HostAndPort target) {
    final SslContext sslctx;
    try {
      sslctx = SslContextBuilder.forClient().build();
    }
    catch (SSLException e) {
      throw new RuntimeException(e);
    }
    return create(group, target, sslctx);
  }

}
