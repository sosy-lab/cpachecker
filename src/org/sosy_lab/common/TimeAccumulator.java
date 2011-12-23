/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
