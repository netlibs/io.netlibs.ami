package io.netlibs.ami.pump;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import io.micrometer.core.instrument.MeterRegistry;
import io.netlibs.ami.api.AmiFrame;
import io.netlibs.ami.netty.DefaultAmiFrame;
import io.netlibs.ami.pump.event.ImmutableKinesisEvent;
import io.netlibs.ami.pump.event.KinesisEvent;
import io.netlibs.ami.pump.model.ImmutablePumpId;
import io.netlibs.ami.pump.model.ImmutableSourceInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class AmiChannelHandler extends SimpleChannelInboundHandler<Object> {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AmiChannelHandler.class);

  private boolean closed = false;
  private final AtomicLong nextActionId = new AtomicLong();

  private final ObjectMapper mapper;
  private final MeterRegistry meterRegistry;
  private final ImmutablePumpId pumpId;
  private final ImmutableSet<String> ignoreEvents;
  private final ImmutableList<KinesisJournal> streams;

  private ObjectNode pumpNode;

  private AtomicLong seqno;

  public AmiChannelHandler(
      ObjectMapper mapper,
      MeterRegistry meterRegistry,
      ImmutablePumpId pumpId,
      ImmutableSet<String> ignoreEvents,
      ImmutableList<KinesisJournal> streams) {
    this.mapper = mapper;
    this.meterRegistry = meterRegistry;
    this.pumpId = pumpId;
    this.ignoreEvents = ignoreEvents;
    this.streams = streams;

    this.pumpNode = JsonNodeFactory.instance.objectNode();
    pumpNode.put("id", pumpId.id());
    pumpNode.put("epoch", pumpId.epoch());

    //
    this.seqno = new AtomicLong();

  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent e = (IdleStateEvent) evt;
      log.info("idle event {}", e);
      if (e.state() == IdleState.READER_IDLE) {
        log.info("reader was idle for too long ({}), closing connection");
        this.closed = true;
        ctx.close();
      }
      else if (e.state() == IdleState.ALL_IDLE) {
        // no read or writes for the interval, so send ping.
        log.info("no read or write for {}, sending ping");
        sendPing(ctx);
      }
    }
  }

  private void sendPing(ChannelHandlerContext ctx) {
    log.info("sending ping");
    DefaultAmiFrame pingFrame = DefaultAmiFrame.newFrame();
    pingFrame.add("Action", "Ping");
    pingFrame.add("ActionID", Long.toHexString(nextActionId.incrementAndGet()));
    ctx.writeAndFlush(pingFrame);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

    if (msg instanceof AmiFrame) {

      AmiFrame frame = (AmiFrame) msg;

      CharSequence res = frame.get("Response");

      String friendlyEvent;

      if (res != null) {

        if (res.equals("Error")) {
          log.error("got aim frame error: {}", frame);
          ctx.channel().close();
          closed = true;
          return;
        }

        friendlyEvent = "Response";

        // note: we include the Ping response in the stream. this is useful for keepalive
        // purposes.

      }
      else {

        String eventType = frame.getOrDefault("Event", "").toString();

        if (eventType.isEmpty()) {
          log.error("invalid Event frame (missing Event): {}", frame);
          return;
        }

        if (ignoreEvents.contains(eventType.toLowerCase())) {
          log.debug("ignoring event: {}", eventType);
          return;
        }

        if (eventType.equalsIgnoreCase("UserEvent")) {
          friendlyEvent = frame.getOrDefault("UserEvent", "").toString().toLowerCase();
        }
        else {
          friendlyEvent = eventType.toLowerCase();
        }

        try {

          this.meterRegistry
            .counter("events.count", "pump", this.pumpId.id(), "type", friendlyEvent)
            .increment();

        }
        catch (Exception ex) {
          log.warn("error recording event", ex.getMessage());
        }

      }

      KinesisEvent evt = convert(pumpId.id(), seqno, frame);
      ObjectNode e = convert(evt, pumpNode);
      String output = mapper.writeValueAsString(e) + "\n";

      streams.forEach(stream -> stream.append(friendlyEvent, output));

    }

  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    log.error("channel received exception: {}", cause.getMessage(), cause);
    if (ctx.channel().isOpen() && !closed) {
      log.info("closing channel");
      closed = true;
      ctx.channel().close();
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    if (!closed && ctx.channel().isActive()) {
      // always read more, as long as we are not closed.
      ctx.read();
    }
  }

  public long nextActionId() {

    return nextActionId.incrementAndGet();

  }

  private static KinesisEvent convert(String sourceId, AtomicLong seqno, AmiFrame event) {

    ObjectNode jevt = JsonNodeFactory.instance.objectNode();
    event.forEach((k, v) -> jevt.put(k.toString(), v.toString()));

    // do not pass this header on.
    jevt.remove("Privilege");

    // remove SystemName if it matches the source id.
    String source = event.getOrDefault("SystemName", sourceId);

    if (event.scalarContains("SystemName", source)) {
      jevt.remove("SystemName");
    }

    return ImmutableKinesisEvent.builder()
      .source(ImmutableSourceInfo.of(sourceId, seqno.getAndIncrement()))
      .payload(jevt)
      .build();

  }

  private static ObjectNode convert(KinesisEvent e, ObjectNode pump) {
    ObjectNode vals = JsonNodeFactory.instance.objectNode();
    vals.put("v", 2);
    vals.set("pump", pump.deepCopy());
    vals.put("typ", "event");
    vals.put("at", Instant.now().toString());
    ObjectNode sourceNode = vals.putObject("src");
    sourceNode.put("id", e.source().sourceId());
    sourceNode.put("seq", e.source().sourceSequence());
    vals.set("event", e.payload());
    return vals;
  }

}
