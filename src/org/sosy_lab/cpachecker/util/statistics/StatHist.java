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

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Thread-safe implementation of numerical statistics.
 * This class tracks how often a value is used in a series of values.
 * Use case might be sampling of values during an analysis.
 */
public class StatHist extends AbstractStatValue {

  protected final Multiset<Long> hist = ConcurrentHashMultiset.create();

  public StatHist(String pTitle) {
    super(StatKind.AVG, pTitle);
  }

  public int getTimesWithValue(Long value) {
    return hist.count(value);
  }

  public void insertValue(long pNewValue) {
    hist.add(pNewValue);
  }

  @Override
  public String toString() {
    return String.format(
        "%s (cnt=%d, avg=%.2f, dev=%.2f)", hist, hist.size(), getAvg(), getStdDeviation());
  }

  public double getStdDeviation() {
    synchronized (hist) {
      final double avg = getAvg();
      double sum = 0;
      for (Entry<Long> e : hist.entrySet()) {
        double deviation = avg - e.getElement();
        sum += (deviation * deviation * e.getCount());
      }
      return Math.sqrt(sum / hist.size());
    }
  }

  public double getAvg() {
    synchronized (hist) {
      return getSum() / hist.size();
    }
  }

  /** returns the element at position floor(size/2). */
  public long getMean() {
    synchronized (hist) {
      List<Long> values = new ArrayList<>(hist.elementSet());
      Collections.sort(values);
      int i = 0;
      int middle = (hist.size() + 1) / 2;
      for (long value : values) { // sorted
        int count = hist.count(value);
        if (i < middle && middle <= i + count) {
          return value;
        }
        i += count;
      }
      return 0;
    }
  }

  /** returns the maximum value, or MIN_INT if no value is available. */
  public long getMax() {
    return reduce(Math::max, Long.MIN_VALUE);
  }

  /** returns the minimum value, or MAX_INT if no value is available. */
  public long getMin() {
    return reduce(Math::min, Long.MAX_VALUE);
  }

  /** returns the sum of all values, or 0 if no value is available. */
  public double getSum() {
    return reduce((res, e) -> (res + e * hist.count(e)), 0.0);
  }

  private <T> T reduce(BiFunction<T, Long, T> f, T neutral) {
    synchronized (hist) {
      T result = neutral;
      for (Entry<Long> e : hist.entrySet()) {
        result = f.apply(result, e.getElement());
      }
      return result;
    }
  }

  @Override
  public int getUpdateCount() {
    return hist.size();
  }

  public void mergeWith(StatHist other) {
    hist.addAll(other.hist);
  }
}
