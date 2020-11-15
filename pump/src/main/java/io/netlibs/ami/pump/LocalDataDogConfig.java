package io.netlibs.ami.pump;

import io.micrometer.datadog.DatadogConfig;

public class LocalDataDogConfig implements DatadogConfig {

  private String apiKey;

  public LocalDataDogConfig(String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
  public String prefix() {
    return "ami2kinesis";
  }

  @Override
  public String get(String key) {
    switch (key) {
      case "apiKey":
        return apiKey;
    }
    return null;
  }

}
