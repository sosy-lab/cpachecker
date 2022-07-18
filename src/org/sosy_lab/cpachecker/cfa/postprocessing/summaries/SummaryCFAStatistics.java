// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

public class SummaryCFAStatistics implements Statistics {

  private Map<StrategiesEnum, Integer> usedStrategies = new HashMap<>();

  @SuppressWarnings("unused")
  private SummaryInformation summaryInformation;

  public SummaryCFAStatistics(
      SummaryInformation pSummaryInformation, Set<StrategiesEnum> pStrategies) {
    summaryInformation = pSummaryInformation;
    for (StrategiesEnum s : pStrategies) {
      usedStrategies.put(s, 0);
    }
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    pOut.println("CFA summaries statistics:");
    pOut.println("    Used Strategies on the CFA:");
    for (Entry<StrategiesEnum, Integer> e : this.usedStrategies.entrySet()) {
      pOut.printf("        %-30s: %d%n", e.getKey(), e.getValue());
    }

    int distinctNodesWithStrategies =
        -100; // This must be refactored, since the information for the CFA is no longer in the
              // SummaryInformation

    pOut.printf("    %-30s: %d%n", "Distinct Nodes with Strategies", distinctNodesWithStrategies);

    pOut.printf(
        "    %-30s: %d%n",
        "Total strategies used",
        this.usedStrategies.values().stream().mapToInt(Integer::intValue).sum());
  }

  public void addStrategy(StrategiesEnum pStrategy) {
    if (usedStrategies.containsKey(pStrategy)) {
      usedStrategies.put(pStrategy, usedStrategies.get(pStrategy) + 1);
    } else {
      usedStrategies.put(pStrategy, 1);
    }
  }

  @Override
  public String getName() {
    return null;
  }
}
