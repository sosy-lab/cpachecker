// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.StrategyInterface;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;

@Options(prefix = "cpa.loopsummary")
class LoopSummaryCPAStatistics implements Statistics {

  @Option(
      name = "loopsummarystats",
      secure = true,
      description = "Should Loopsummary Statistics be recorded")
  private boolean loopsummarystats = true;

  @SuppressWarnings("unused")
  private final LogManager logger;

  @SuppressWarnings("unused")
  private final AbstractLoopSummaryCPA cpa;

  private final Map<String, StatCounter> strategiesUsed = new LinkedHashMap<>();

  public LoopSummaryCPAStatistics(Configuration pConfig, LogManager pLogger, AbstractLoopSummaryCPA pCpa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    cpa = pCpa;
    for (StrategyInterface s : pCpa.getStrategies()) {
      strategiesUsed.put(s.getName(), new StatCounter(s.getName()));
    }
  }

  @Override
  public String getName() {
    return "LoopSummaryCPA";
  }

  public void incrementStrategyUsageCount(String summaryName) {
    if (strategiesUsed.containsKey(summaryName)) {
      strategiesUsed.get(summaryName).inc();
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    if (loopsummarystats) {
      out.println("Strategy Statistics:");
      for (Entry<String, StatCounter> e : strategiesUsed.entrySet()) {
        int padding = maxlen - e.getKey().length() + 1;
        put(out, String.format("%s usage count", e.getKey()), e.getValue());
      }
    }
  }
}
