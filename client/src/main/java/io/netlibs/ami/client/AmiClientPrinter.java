package io.netlibs.ami.client;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.common.net.HostAndPort;

import io.netlibs.ami.netty.DefaultAmiFrame;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class AmiClientPrinter {

  public static void main(String[] args) throws InterruptedException, ExecutionException {

    String target =
      args.length > 0 ? args[0]
                      : "localhost:5030";

    CompletableFuture<Channel> connector = AmiConnection.connect(HostAndPort.fromString(target), Duration.ofSeconds(5));

    Channel ch = connector.get();

    System.err.println("connected");

    DefaultAmiFrame loginFrame = DefaultAmiFrame.newFrame();
    loginFrame.add("Action", "Login");
    loginFrame.add("ActionID", Long.toHexString(1));
    loginFrame.add("Username", "test");
    loginFrame.add("Secret", "test");
    ch.writeAndFlush(loginFrame);

    ch.pipeline().addLast(new SimpleChannelInboundHandler<Object>(true) {
      @Override
      protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.err.println(msg);
      }
    });

    while (ch.isOpen()) {
      ch.read();
    }

  }

}
