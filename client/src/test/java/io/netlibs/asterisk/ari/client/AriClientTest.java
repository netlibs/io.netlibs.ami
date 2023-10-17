package io.netlibs.asterisk.ari.client;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.netlibs.asterisk.ari.events.Event;

class AriClientTest {

  String IN =
    """
        {"type":"StasisStart","timestamp":"2023-10-16T12:14:48.762+0000","args":["dwedewd"],"channel":{"id":"1697458488.14","name":"PJSIP/outbound-0000000e","state":"Ring","protocol_id":"120009ODViODEzZTBhYzQ5OTJiYWE4MGUzNWIzNWExZDE1NWU","caller":{"name":"Theo","number":"100-theo"},"connected":{"name":"","number":""},"accountcode":"","dialplan":{"context":"outbound","exten":"dwedewd","priority":1,"app_name":"Stasis","app_data":"ob.us-west-2.rtfs.theo.fluentdev.app,dwedewd"},"creationtime":"2023-10-16T12:14:48.762+0000","language":"en"},"asterisk_id":"0a:58:a9:fe:ac:02","application":"ob.us-west-2.rtfs.theo.fluentdev.app"}
        {"cause":127,"type":"ChannelHangupRequest","timestamp":"2023-10-16T12:26:24.703+0000","channel":{"id":"1697459182.15","name":"PJSIP/outbound-0000000f","state":"Ring","protocol_id":"120009ZTAxZTE4YjgwODhiYzA2YTRkYzNlMTI5MjA1MjVlNGY","caller":{"name":"Theo","number":"100-theo"},"connected":{"name":"","number":""},"accountcode":"","dialplan":{"context":"outbound","exten":"deded","priority":1,"app_name":"Stasis","app_data":"ob.us-west-2.rtfs.theo.fluentdev.app,deded"},"creationtime":"2023-10-16T12:26:22.973+0000","language":"en"},"asterisk_id":"0a:58:a9:fe:ac:02","application":"ob.us-west-2.rtfs.theo.fluentdev.app"}
        {"type":"StasisEnd","timestamp":"2023-10-16T12:26:24.703+0000","channel":{"id":"1697459182.15","name":"PJSIP/outbound-0000000f","state":"Ring","protocol_id":"120009ZTAxZTE4YjgwODhiYzA2YTRkYzNlMTI5MjA1MjVlNGY","caller":{"name":"Theo","number":"100-theo"},"connected":{"name":"","number":""},"accountcode":"","dialplan":{"context":"outbound","exten":"deded","priority":1,"app_name":"Stasis","app_data":"ob.us-west-2.rtfs.theo.fluentdev.app,deded"},"creationtime":"2023-10-16T12:26:22.973+0000","language":"en"},"asterisk_id":"0a:58:a9:fe:ac:02","application":"ob.us-west-2.rtfs.theo.fluentdev.app"}
        """;

  @Test
  void test() throws JsonMappingException, JsonProcessingException {

    final ObjectMapper mapper =
      new JsonMapper()
        .registerModule(new JavaTimeModule())
        .registerModule(new Jdk8Module());

    for (final String line : this.IN.split("\n")) {

      mapper.readValue(line, Event.class);

    }

  }

}
