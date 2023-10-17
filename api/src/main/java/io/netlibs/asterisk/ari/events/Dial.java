package io.netlibs.asterisk.ari.events;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Dial(

    @JsonProperty("asterisk_id") String asteriskId,

    String type,

    /** application: string - Name of the application receiving the event. */
    String application,

    /** timestamp: Date - Time at which this event was created. */
    String timestamp,

    /** caller: Channel (optional) - The calling channel. */
    Optional<Channel> caller,

    /** dialstatus: string - Current status of the dialing attempt to the peer. */
    String dialstatus,

    // dialstring: string (optional) - The dial string for calling the peer channel.
    Optional<String> dialstring,

    // forward: string (optional) - Forwarding target requested by the original dialed channel.
    Optional<String> forward,

    // forwarded: Channel (optional) - Channel that the caller has been forwarded to.
    Optional<String> forwarded,

    // peer: Channel - The dialed channel.
    Channel peer

) implements Event {

}
