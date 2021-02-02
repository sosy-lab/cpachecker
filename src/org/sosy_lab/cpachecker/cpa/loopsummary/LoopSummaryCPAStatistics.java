// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import java.io.PrintStream;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

@Options(prefix = "cpa.loopsummary")
class LoopSummaryCPAStatistics implements Statistics {

  @Option(name = "test", secure = true, description = "test")
  private boolean test = false;

  //private final LogManager logger;
  //private final AbstractLoopSummaryCPA cpa;

  public LoopSummaryCPAStatistics(Configuration pConfig, LogManager pLogger, AbstractLoopSummaryCPA pCpa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    // logger = pLogger;
    // cpa = pCpa;
    if (test) {
      test = true;
    }
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
