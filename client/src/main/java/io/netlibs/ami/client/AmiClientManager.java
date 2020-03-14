package io.netlibs.ami.client;

import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractService;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class AmiClientManager extends AbstractService {

  private EventLoopGroup group = new NioEventLoopGroup();

  public AmiConnectionHandler connectTls(HostAndPort target, AmiCredentials credentials) {
    NettyAmiTransport ch = NettyAmiTransport.createTls(group, target);
    return AmiConnectionHandler.create(ch, credentials);
  }

  @Override
  protected void doStart() {
    super.notifyStarted();
  }

  @Override
  protected void doStop() {
    group.shutdownGracefully().addListener(f -> super.notifyStopped());
  }

}
