package de.upb.agw.cpachecker.cpa.abm.util;

import org.sosy_lab.common.Timer;

/**
 * Extended <code>Timer</code> that can be started multiple times and stops only if <code>stop</code> is called as many times.
 * @author dwonisch
 *
 */
public class CountedTimer extends Timer {
  
  private int counter = 0;
  
  @Override
  public void start() {
    if(counter++ == 0) {
      super.start();
    }    
  }

  
  @Override
  public long stop() {
    if(--counter == 0) {
      return super.stop();
    }
    return 0;
  }
}
