// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.bam.AbstractBAMCPA;
import java.io.PrintStream;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;

class LoopSummaryCPAStatistics implements Statistics {

  //private final LogManager logger;
  //private final AbstractLoopSummaryCPA cpa;

  public LoopSummaryCPAStatistics(Configuration pConfig, LogManager pLogger, AbstractLoopSummaryCPA pCpa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    //logger = pLogger;
    //cpa = pCpa;
  }

  @Override
  public String getName() {
    return "LoopSummaryCPA";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {

    put(out, "Test", 1);
  }
}
