// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import org.sosy_lab.cpachecker.util.statistics.StatisticsValue.StatisticsValueUpdateListener;

public class StatisticsLimit<T extends Comparable<T>> implements StatisticsValueUpdateListener<T> {

  private String name;
  private T limit;

  public StatisticsLimit(String pname, T pLimit) {
    name = pname;
    limit = pLimit;
  }

  /**
   * Return a human-readable representation of this limit that can be presented to the user.
   *
   * @return A non-null string.
   */
  public String getName() {
    return name;
  }

  public T getLimit() {
    return limit;
  }

  public void setLimit(T pLimit) {
    limit = pLimit;
  }

  /**
   * Check whether a given StatisticsValue means that the limit has been exceeded and we should
   * stop.
   *
   * <p>Usually, this method checks whether the current value is greater or equal than some stored
   * value that specifies the limit.
   *
   * @param pStatisticsValue A Statistics Value
   * @return True if the limit has been exceeded.
   */
  public boolean isExceeded(StatisticsValue<T> pStatisticsValue) {
    if (pStatisticsValue.getValue().compareTo(limit) >= 0) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void updated(StatisticsValue<T> pStatisticsValue) throws InterruptedException {
    if (this.isExceeded(pStatisticsValue)) {
      String reason = String.format("The %s of %s has elapsed.", name, limit.toString());
      throw new InterruptedException(reason);
    }
  }
}