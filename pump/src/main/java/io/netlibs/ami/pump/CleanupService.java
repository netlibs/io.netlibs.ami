package io.netlibs.ami.pump;

import java.io.File;
import java.text.ParseException;
import java.time.Duration;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractScheduledService;

import net.openhft.chronicle.queue.TailerDirection;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueStore;

public class CleanupService extends AbstractScheduledService {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CleanupService.class);
  private ImmutableList<KinesisJournal> streams;

  public CleanupService(ImmutableList<KinesisJournal> streams) {
    this.streams = streams;
  }

  public void execute() {
    for (KinesisJournal stream : streams) {
      try {

        SingleChronicleQueue journal = stream.journal();

        long lowerIndex = stream.commitedIndex();

        if ((lowerIndex <= 0) || (lowerIndex == Long.MAX_VALUE)) {
          continue;
        }

        int lowerCycle = journal.rollCycle().toCycle(lowerIndex);

        int upperCycle = journal.lastCycle();

        if (upperCycle == Integer.MIN_VALUE) {
          continue;
        }

        int activeCycle = Math.min(lowerCycle, upperCycle);

        log.info("active cycle for {} is {}", stream.serviceName(), activeCycle);

        if (activeCycle <= 0) {
          continue;
        }

        cleanup(journal, activeCycle - 1);

      }
      catch (Exception ex) {
        log.error("exception caught while trying to clean up stream {}: {}", stream.toString(), ex.getMessage(), ex);
      }
    }
  }

  @Override
  protected void runOneIteration() throws Exception {
    execute();
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedRateSchedule(Duration.ofMinutes(1), Duration.ofSeconds(30));
  }

  /**
   * attempt to remove all journal segments up to (but not including) "untilCycle".
   * 
   * these cycles must not be in use. if they are, it may cause an error.
   * 
   * @throws ParseException
   * 
   */

  private void cleanup(SingleChronicleQueue journal, int untilCycle) throws ParseException {

    int currentCycle = journal.firstCycle();

    do {

      int nextCycle = journal.nextCycle(currentCycle, TailerDirection.FORWARD);

      // fetch the store. we don't really need (or want) to open it, but alas there is not a way
      // supported by the public API which allows us to get the file name for a cycle that i can
      // find.
      SingleChronicleQueueStore store = journal.storeForCycle(currentCycle, 0, false, null);

      // get filename.
      File file = store.file();

      // close the store.
      journal.closeStore(store);

      if (!file.exists()) {
        log.warn("couldn't remove missing cycle from store {}", file);
        return;
      }

      log.info("removing expired cycle {}", currentCycle, file);

      // remove it, or at least try!
      try {
        file.delete();
      }
      catch (Exception ex) {
        log.error("failed to delete cycle {}: {}", file.toString(), ex.getMessage());
      }

      journal.refreshDirectoryListing();

      // move to the next cycle, if there is one.
      currentCycle = nextCycle;

    }
    while ((currentCycle != -1) && (currentCycle < untilCycle));

  }

}
