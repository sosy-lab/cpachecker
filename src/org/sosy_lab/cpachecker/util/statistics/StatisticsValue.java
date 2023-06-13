// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

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

  public StatisticsValue(String pName, T pValue) {
    name = pName;
    value = pValue;
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

  /**
   * @param pValue The new value to set
   * @throws InterruptedException In the limit implementation, we stop the verification run by
   *     interrupting
   */
  public void setValue(T pValue) throws InterruptedException {
    value = pValue;
    for (StatisticsValueUpdateListener<T> li : statisticsValueUpdateListeners) {
      li.updated(this);
    }
  }

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