package io.netlibs.ami.pump;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager.Listener;

public class AmiServiceManagerListener extends Listener {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AmiServiceManagerListener.class);

  @Override
  public void healthy() {
    log.info("service manager is healthy");
  }

  @Override
  public void stopped() {
    log.info("service manager is stopped");
  }

  @Override
  public void failure(Service service) {

    // this should always result in immediate failure.
    log.error("service {} has failed", service);

  }

}
