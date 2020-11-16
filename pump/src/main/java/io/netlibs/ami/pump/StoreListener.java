package io.netlibs.ami.pump;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

import net.openhft.chronicle.queue.impl.StoreFileListener;

public class StoreListener implements StoreFileListener {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StoreListener.class);

  private String streamName;
  private LongSupplier currentCycleProvider;

  private Map<Integer, CycleState> state = new HashMap<>();

  private class CycleState {

    private int cycle;
    private volatile int count = 0;

    public CycleState(int cycle) {
      this.cycle = cycle;
    }

    public void increment() {
      this.count++;
    }

    public boolean decrement(long currentCycle, File file) {
      this.count--;
      if (this.count == 0) {
        return (currentCycle > cycle);
      }
      return false;
    }

  }

  public StoreListener(String streamName, LongSupplier currentCycleProvider) {
    this.streamName = streamName;
    this.currentCycleProvider = currentCycleProvider;
  }

  @Override
  public void onAcquired(int cycle, File file) {
    synchronized (this.state) {
      this.state.computeIfAbsent(cycle, c -> new CycleState(c)).increment();
    }
  }

  @Override
  public void onReleased(int cycle, File file) {

    long currentCycle = this.currentCycleProvider.getAsLong();

    log.info("streamName {}, journal cycle {} released with head at {}, file {}", streamName, currentCycle, cycle, file.toString());

    synchronized (this.state) {
      if (this.state.get(cycle).decrement(currentCycle, file)) {
        this.state.remove(cycle);
        try {
          file.delete();
        }
        catch (Exception e) {
          log.warn("failed to delete cycle {} file {}: {}", cycle, file, e.getMessage());
        }
      }
    }

  }

}
