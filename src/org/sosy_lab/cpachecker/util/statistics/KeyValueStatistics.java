// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.statistics;

import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

/** Simple Key-Value statistics implementation. */
public class KeyValueStatistics implements Statistics {

  private final Map<String, Object> keyValueStats = new TreeMap<>();

  public void addKeyValueStatistic(final String pName, final Object pValue) {
    keyValueStats.put(pName, pValue);
  }

  @Override
  public final void printStatistics(
      PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    for (Map.Entry<String, Object> item : keyValueStats.entrySet()) {
      put(pOut, item.getKey(), item.getValue());
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation of getName() computes the name of the statistic from the class name.
   */
  @Override
  public String getName() {
    String result = getClass().getSimpleName();
    int relevantUntil = result.lastIndexOf(Statistics.class.getSimpleName());
    if (relevantUntil == -1) {
      return result;
    } else {
      return result.substring(0, relevantUntil);
    }
  }
}
