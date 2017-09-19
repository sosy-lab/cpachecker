/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.statistics;

import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/** Thread-safe implementation of numerical statistics. */
public class StatInt extends AbstractStatValue {

  private LongAccumulator maxValue = new LongAccumulator(Math::max, Integer.MIN_VALUE);
  private LongAccumulator minValue = new LongAccumulator(Math::min, Integer.MAX_VALUE);
  private LongAdder valueCount = new LongAdder();
  private LongAdder valueSum = new LongAdder();

  public StatInt(StatKind pMainStatisticKind, String pTitle) {
    super(pMainStatisticKind, pTitle);
  }

  public void setNextValue(int newValue) {
    valueSum.add(newValue);
    valueCount.increment();
    maxValue.accumulate(newValue);
    minValue.accumulate(newValue);
  }

  public long getMaxValue() {
    return valueCount.sum() == 0 ? 0 : maxValue.intValue();
  }

  public long getMinValue() {
    return valueCount.sum() == 0 ? 0 : minValue.intValue();
  }

  public long getValueCount() {
    return valueCount.sum();
  }

  public long getValueSum() {
    return valueSum.sum();
  }

  public float getAverage() {
    long count = valueCount.sum();
    if (count > 0) {
      return (float) valueSum.sum() / count;
    } else {
      return 0;
    }
  }

  public long getMax() {
    return maxValue.get();
  }

  public long getMin() {
    return minValue.get();
  }

  @Override
  public int getUpdateCount() {
    return valueCount.intValue();
  }

  @Override
  public String toString() {
    return String.format(
        "%8d (count: %d, min: %d, max: %d, avg: %.2f)",
        getValueSum(), getValueCount(), getMinValue(), getMaxValue(), getAverage());
  }

}