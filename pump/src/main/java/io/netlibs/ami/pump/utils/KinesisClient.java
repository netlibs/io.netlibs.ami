package io.netlibs.ami.pump.utils;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import org.immutables.value.Value;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.reactivex.rxjava3.processors.BehaviorProcessor;

public class KinesisClient {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KinesisClient.class);

  private KinesisProducer producer;
  private BehaviorProcessor<Runnable> queue = BehaviorProcessor.create();
  private AWSCredentialsProvider credentialsProvider;
  private String streamName;

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableKinesisAggregationConfig.Builder.class)
  interface KinesisAggregationConfig {

    Optional<Boolean> enabled();

    OptionalInt maxCount();

    OptionalInt maxSize();

  }

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableKinesisConfig.Builder.class)
  interface KinesisConfig {

    String region();

    String streamName();

    Optional<KinesisAggregationConfig> aggregation();

    OptionalInt rateLimit();

    OptionalInt maxConnections();

    Optional<Duration> recordTtl();

    OptionalInt threadPoolSize();

    Optional<Duration> recordMaxBufferedTime();

    Optional<Duration> requestTimeout();

  }

  public KinesisClient(AWSCredentialsProvider credentialsProvider, String region, String streamName) {
    this.credentialsProvider = credentialsProvider;
    applyConfig(ImmutableKinesisConfig.builder().region(region).streamName(streamName).build());
  }

  private void applyConfig(KinesisConfig settings) {

    this.streamName = settings.streamName();

    log.info("Config: {}", settings);

    // this.streamArn = ArnParser.parse(settings.streamArn(), KinesisStreamArn.class);

    log.info("Kinesis ARN is {}", streamName);

    final KinesisProducerConfiguration config = new KinesisProducerConfiguration();

    settings.aggregation().ifPresentOrElse(aggr -> {
      config.setAggregationEnabled(aggr.enabled().orElse(true));
      config.setAggregationMaxCount(aggr.maxCount().orElse(1024));
      config.setAggregationMaxSize(aggr.maxSize().orElse(1024 * 16));
    }, () -> {
      config.setAggregationEnabled(true);
      // config.setAggregationMaxCount(1024);
      // config.setAggregationMaxSize(10224 * 16);
    });

    config.setRateLimit(settings.rateLimit().orElse(150));
    config.setMaxConnections(settings.maxConnections().orElse(24));

    // config.setRecordTtl(settings.recordTtl().orElse(Duration.ofSeconds(30)).toMillis());

    config.setThreadPoolSize(settings.threadPoolSize().orElse(0));
    config.setThreadingModel(KinesisProducerConfiguration.ThreadingModel.POOLED);
    config.setRecordMaxBufferedTime(settings.recordMaxBufferedTime().orElse(Duration.ofMillis(10)).toMillis());
    config.setRequestTimeout(settings.requestTimeout().orElse(Duration.ofSeconds(6)).toMillis());

    // region based on the stream ARN.
    config.setRegion(settings.region());

    // todo: use AssumeRole if different account?
    config.setCredentialsProvider(this.credentialsProvider);
    //
    this.producer = new KinesisProducer(config);

    // now we can run
    this.queue.subscribe(e -> e.run());

  }

  public void add(String partitionKey, ByteBuffer data) {
    queue.onNext(() -> this.producer.addUserRecord(this.streamName, partitionKey, data));
  }

  public void flushSync() {
    this.producer.flushSync();
  }

  public void close() {
    this.queue.onComplete();
    this.producer.destroy();
  }

}