// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward.BackwardAnalysisCoreStatistics;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward.BackwardAnalysisFullStatistics;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.forward.ForwardAnalysisStatistics;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;

public class ConcurrentStatisticsCollector implements StatisticsProvider {
  public interface TaskStatistics { 
    void accept(final ConcurrentStatisticsCollector collector);
  }
  
  public void visit(final BackwardAnalysisFullStatistics pStatistics) {
    
  }

  public void visit(final BackwardAnalysisCoreStatistics pStatistics) {

  }
  
  public void visit(final ForwardAnalysisStatistics pStatistics) {

  }
  
  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
  }
}
