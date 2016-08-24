/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;


/**
 * Abstract implementation of Statistics that
 * provides some functions for more convenience and cleaner code
 * when dealing with CPAchecker statistics.
 */
public abstract class AbstractStatistics implements Statistics {

  protected int outputNameColWidth = 50;

  private final Map<String, Object> keyValueStats = new TreeMap<>();

  /**
   * Print a statistics line in a "pretty" fashion.
   *
   * @param target      Write to this stream
   * @param indentLevel Indentation level (0 = no indentation)
   * @param name        Left hand side (name/description)
   * @param value       Right hand side (value)
   */
  protected void put(PrintStream target, int indentLevel, String name, Object value) {
    StatisticsUtils.write(target, indentLevel, outputNameColWidth, name, value);
  }

  protected void put(PrintStream target, int indentLevel, AbstractStatValue stat) {
    StatisticsUtils.write(target, indentLevel, outputNameColWidth, stat);
  }

  protected void put(PrintStream pTarget, String pName, Object pValue) {
    put(pTarget, 0, pName, pValue);
  }

  public void addKeyValueStatistic(final String pName, final Object pValue) {
    keyValueStats.put(pName, pValue);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    for (Map.Entry<String, Object> item : keyValueStats.entrySet()) {
      put(pOut, item.getKey(), item.getValue());
    }
  }

  /**
   * {@inheritDoc}
   *
   * This implementation of getName() computes the name of the statistic from the class name.
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
