/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
