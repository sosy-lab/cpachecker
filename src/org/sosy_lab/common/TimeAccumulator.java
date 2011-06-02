package org.sosy_lab.common;

import com.google.common.base.Preconditions;

public class TimeAccumulator {

  private long[] mMilliseconds;
  private long mStartTime;

  private boolean mActive;

  public TimeAccumulator(int pNumberOfSlots) {
    Preconditions.checkArgument(pNumberOfSlots > 0);

    mMilliseconds = new long[pNumberOfSlots];

    for (int lIndex = 0; lIndex < pNumberOfSlots; lIndex++) {
      mMilliseconds[lIndex] = 0;
    }

    mActive = false;
  }

  public TimeAccumulator() {
    this(1);
  }

  public void proceed() {
    Preconditions.checkState(!mActive);

    mActive = true;

    mStartTime = System.currentTimeMillis();
  }

  public void pause(int pSlot) {
    Preconditions.checkArgument(pSlot >= 0);
    Preconditions.checkArgument(pSlot < mMilliseconds.length);
    Preconditions.checkState(mActive);

    mMilliseconds[pSlot] += (System.currentTimeMillis() - mStartTime);

    mActive = false;
  }

  public void pause() {
    pause(0);
  }

  public double getSeconds(int pSlot) {
    Preconditions.checkArgument(pSlot >= 0);
    Preconditions.checkArgument(pSlot < mMilliseconds.length);
    Preconditions.checkState(!mActive);

    return mMilliseconds[pSlot]/1000.0;
  }

  public double getSeconds() {
    return getSeconds(0);
  }

}
