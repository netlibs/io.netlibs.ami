package io.netlibs.ami.pump;

import java.io.File;
import java.util.function.LongSupplier;

import net.openhft.chronicle.queue.impl.StoreFileListener;

public class StoreListener implements StoreFileListener {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StoreListener.class);

  private String streamName;
  private LongSupplier currentCycleProvider;

  public StoreListener(String streamName, LongSupplier currentCycleProvider) {
    this.streamName = streamName;
    this.currentCycleProvider = currentCycleProvider;
  }

  @Override
  public void onReleased(int cycle, File file) {

    long currentCycle = this.currentCycleProvider.getAsLong();

    log.info("streamName {}, journal cycle {} released with head at {}, file {}", streamName, currentCycle, cycle, file.toString());

    if (cycle < currentCycle) {
      try {
        log.info("deleting {}", file.toString());
        file.delete();
      }
      catch (Exception ex) {
        log.warn("failed to delete {}: {}", file.toString(), ex.getMessage());
      }
    }

  }

}
