// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A class to store and update exactly one statistic value. Implements an Observable for e.g.
 * resource limits
 */
public class StatisticsValue<T> {

  private String name;
  private T value;

  private final Collection<StatisticsValueUpdateListener<T>> statisticsValueUpdateListeners =
      new ArrayList<>();

  public StatisticsValue(String pname) {
    name = pname;
  }

  /**
   * Get the name for this statistics value.
   *
   * @return A String with a human-readable name.
   */
  public String getName() {
    return name;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T pValue) {
    value = pValue;
  }

  /** @param out the PrintStream to use for printing the statistics */
  public void printValue(PrintStream out) {}

  public void register(StatisticsValueUpdateListener<T> pStatisticsValueListener) {
    statisticsValueUpdateListeners.add(pStatisticsValueListener);
  }

  public void unregister(StatisticsValueUpdateListener<T> pStatisticsValueListener) {
    statisticsValueUpdateListeners.remove(pStatisticsValueListener);
  }

  public interface StatisticsValueUpdateListener<T> {
    void updated(StatisticsValue<T> pStatisticsValue) throws InterruptedException;
  }
  }