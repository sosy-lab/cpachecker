// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Preconditions;
import java.io.PrintStream;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;

public abstract class MultiStatistics implements Statistics {

  private final Collection<Statistics> subStats = new CopyOnWriteArrayList<>();
  protected final LogManager logger;

  protected MultiStatistics(LogManager pLogger) {
    logger = Preconditions.checkNotNull(pLogger);
  }

  /** returns a modifiable collection of statistics. */
  public Collection<Statistics> getSubStatistics() {
    return subStats;
  }

  /** removes all available statistics. */
  public void resetSubStatistics() {
    subStats.clear();
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    for (Statistics s : subStats) {
      StatisticsUtils.printStatistics(s, out, logger, result, reached);
    }
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    for (Statistics s : subStats) {
      StatisticsUtils.writeOutputFiles(s, logger, pResult, pReached);
    }
  }
}
