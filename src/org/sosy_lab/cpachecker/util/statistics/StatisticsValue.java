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
 * A class to store and update exactly one statistic value.
 * Implements an Observable for e.g. resource limits
 */

public class StatisticsValue implements StatisticsValueObservable{

  private final Collection<StatisticsValueUpdateListener> statisticsValueUpdateListeners = new ArrayList<>();
  
  /**
   * Define a name for this statistics value.
   *
   * @return A String with a human-readable name or null.
   */
  public String getName() {return "";}
  
  public Object getValue() {return null;}
  
  /**
   * @param out the PrintStream to use for printing the statistics
   */
  public void printValue(PrintStream out) {}
  
  @Override
  public void register(StatisticsValueUpdateListener pStatisticsValueListener) {
    statisticsValueUpdateListeners.add(pStatisticsValueListener);
  }

  @Override
  public void unregister(StatisticsValueUpdateListener pStatisticsValueListener) {
    statisticsValueUpdateListeners.remove(pStatisticsValueListener);
  }
  
  public interface StatisticsValueUpdateListener {
    void updated(StatisticsValue pStatisticsValue);
  }

}
