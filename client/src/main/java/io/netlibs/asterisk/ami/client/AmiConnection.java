package io.netlibs.asterisk.ami.client;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import com.google.common.net.HostAndPort;
import com.google.common.primitives.Ints;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class AmiConnection {

  public static CompletableFuture<Channel> connect(NioEventLoopGroup eventLoop, HostAndPort target, Duration timeout) {

    CompletableFuture<Channel> chf = new CompletableFuture<>();

    Bootstrap b = new Bootstrap();

    b.group(eventLoop)
      .channel(NioSocketChannel.class)
      .option(ChannelOption.AUTO_READ, false)
      .option(ChannelOption.TCP_NODELAY, true)
      .option(ChannelOption.SO_KEEPALIVE, true)
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Ints.checkedCast(timeout.toMillis()))
      .handler(new AmiClientHandler(chf));

    final ChannelFuture f = b.connect(target.getHost(), target.getPort());

    f.addListener(_f -> {
      try {
        f.get(); // throws if an error.
      }
      catch (Throwable t) {
        // failed to connect. fail the future.
        chf.completeExceptionally(t);
      }
    });

    return chf;

  }

}
