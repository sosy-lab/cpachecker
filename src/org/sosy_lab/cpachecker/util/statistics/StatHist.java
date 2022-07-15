// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thread-safe implementation of numerical statistics. This class tracks how often a value is used
 * in a series of values. Use case might be sampling of values during an analysis.
 */
public class StatHist extends AbstractStatValue {

  protected final Multiset<Long> hist = HashMultiset.create();

  public StatHist(String pTitle) {
    super(StatKind.AVG, pTitle);
  }

  public int getTimesWithValue(Long value) {
    synchronized (hist) {
      return hist.count(value);
    }
  }

  public void insertValue(long pNewValue) {
    synchronized (hist) {
      hist.add(pNewValue);
    }
  }

  public void insertValue(long pNewValue, int occurrences) {
    synchronized (hist) {
      hist.add(pNewValue, occurrences);
    }
  }

  @Override
  public String toString() {
    synchronized (hist) {
      return String.format(
          "%s (cnt=%d, avg=%.2f, dev=%.2f)", hist, hist.size(), getAvg(), getStdDeviation());
    }
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
      ImmutableList<Long> values = ImmutableList.sortedCopyOf(hist.elementSet());
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

  /** returns the maximum value, or Long.MIN_VALUE if no value is available. */
  public long getMax() {
    synchronized (hist) {
      return hist.isEmpty() ? Long.MIN_VALUE : Collections.max(hist.elementSet());
    }
  }

  /** returns the minimum value, or Long.MAX_VALUE if no value is available. */
  public long getMin() {
    synchronized (hist) {
      return hist.isEmpty() ? Long.MAX_VALUE : Collections.min(hist.elementSet());
    }
  }

  /** returns the sum of all values, or 0 if no value is available. */
  public double getSum() {
    synchronized (hist) {
      return hist.entrySet().stream()
          .mapToDouble(e -> ((double) e.getElement()) * e.getCount())
          .sum();
    }
  }

  @Override
  public int getUpdateCount() {
    synchronized (hist) {
      return hist.size();
    }
  }

  public void mergeWith(StatHist other) {
    // copy data to avoid a possible deadlock from locking hist and other hist.
    Map<Long, Integer> countMap = new LinkedHashMap<>();
    synchronized (other.hist) {
      for (Long e : other.hist.elementSet()) {
        countMap.put(e, other.hist.count(e));
      }
    }
    synchronized (hist) {
      for (java.util.Map.Entry<Long, Integer> e : countMap.entrySet()) {
        hist.add(e.getKey(), e.getValue());
      }
    }
  }
}
