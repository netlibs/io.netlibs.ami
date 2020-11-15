package io.netlibs.ami.pump;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import picocli.CommandLine.Option;

public class UpstreamConfig {

  @Option(names = { "--journal" }, description = "base journal path")
  Optional<String> journalPath;

  @Option(names = { "--stream" }, description = "AWS Kinesis stream name to write to", defaultValue = "ami-events")
  String streamName;

  @Option(names = { "--filter" },
      description = "comma seperated event filter to apply. use '-' before a filter to exclude. wildcards may be used.",
      defaultValue = "*")
  List<String> filters = new ArrayList<>();

  @Option(names = { "--key" }, description = "AWS Kinesis partition to write to (default uses SystemName in frames)")
  Optional<String> partitionKey = Optional.empty();

  @Option(names = { "--assume-role" }, description = "AWS Kinesis partition to write to (default uses SystemName in frames)")
  Optional<String> assumeRole = Optional.empty();

}
