// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import com.google.common.base.Joiner;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe container for a series of data elements with a time-stamp.
 *
 * <p>It collects all data internally; be aware of the memory usage!
 */
public class StatisticsSeries<T> {

  private final long startTime = System.currentTimeMillis();

  /**
   * The fastest minimal working solution.
   *
   * <p>Note that we do not guarantee a strict temporal ordering of the entries. It is possible to
   * get an unsorted list.
   */
  private final Collection<DataObject<T>> series = new ConcurrentLinkedQueue<>();

  public void add(T pData) {
    series.add(new DataObject<>(pData));
  }

  Collection<DataObject<T>> getSeries() {
    List<DataObject<T>> result = new ArrayList<>(series);
    result.sort(null);
    return result;
  }

  long getStartTime() {
    return startTime;
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

  /** Sub-class with additional methods for statistics. */
  public static class StatisticsSeriesWithNumbers extends StatisticsSeries<Integer> {

    /**
     * Get plain numerical statistics without any hint on behavior over time.
     *
     * <p>Example: For a time series <code>[10:1,11:2,15:3,40:4]</code> we return <code>
     * StatInt([1,2,3,4])</code>.
     */
    public StatInt getStatsWithoutTime() {
      StatInt stats = new StatInt(StatKind.AVG, null);
      for (StatisticsSeries<Integer>.DataObject<Integer> data : getSeries()) {
        stats.setNextValue(data.data);
      }
      return stats;
    }

    /**
     * Get statistics over all time steps.
     *
     * <p>Example: For a time series <code>[10:1,11:2,15:3,40:4]</code> we return <code>
     * StatHist([10x1,1x2,4x3,25x4])</code>.
     */
    public StatHist getStatsOverTime() {
      StatHist stats = new StatHist(null);
      long currentTime = getStartTime();
      for (StatisticsSeries<Integer>.DataObject<Integer> data : getSeries()) {
        long occurences = data.time - currentTime;
        stats.insertValue(data.data, Ints.checkedCast(occurences));
        currentTime = data.time;
      }
      return stats;
    }
  }
}
