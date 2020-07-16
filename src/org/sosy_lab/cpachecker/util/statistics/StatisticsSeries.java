// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import com.google.common.base.Joiner;
import com.google.common.primitives.Longs;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe container for a series of data elements with a time-stamp.
 *
 * <p>It collects all data internally; be aware of the memory usage!
 */
public class StatisticsSeries<T> {

  private final long startTime = System.currentTimeMillis();
  private final Collection<DataObject<T>> series = new ConcurrentLinkedQueue<>();

  public void add(T pData) {
    series.add(new DataObject<>(pData));
  }

  @Override
  public String toString() {
    return Joiner.on('\n').join(series);
  }

  private class DataObject<TT> implements Comparable<DataObject<TT>> {
    private final long time;
    private final TT data;

    DataObject(TT pData) {
      time = System.currentTimeMillis();
      data = pData;
    }

    @Override
    public String toString() {
      return (time - startTime) + ", " + data;
    }

    @Override
    public int compareTo(DataObject<TT> pOther) {
      return Longs.compare(time, pOther.time);
    }

    @Override
    public boolean equals(Object pOther) {
      return super.equals(pOther); // object identity
    }

    @Override
    public int hashCode() {
      return super.hashCode(); // object identity
    }
  }

  /** This stub-class can be used in benchmarking mode to avoid high memory consumption. */
  public static class NoopStatisticsSeries<T> extends StatisticsSeries<T> {
    @Override
    public void add(T pData) {
      // ignore
    }
  }
}
