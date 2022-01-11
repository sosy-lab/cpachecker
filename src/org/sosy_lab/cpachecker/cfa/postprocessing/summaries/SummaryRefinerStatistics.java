// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

public class SummaryRefinerStatistics implements Statistics {

  private Integer doubleRefinementsMade = 0;
  private Integer doubleRefinementsBecauseMaximumAmntFirstRefinements = 0;
  private Integer amountStrategiesRefinedAway = 0;
  private Integer distinctNodesWithStrategies = 0;
  private SummaryInformation summaryInformation;

  public SummaryRefinerStatistics(SummaryInformation pSummaryInformation) {
    this.summaryInformation = pSummaryInformation;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    pOut.println(
        "Total Number of Double Refinements:                                     "
            + doubleRefinementsMade);
    pOut.println(
        "Amount of Double refinements after limit of first refinements:          "
            + doubleRefinementsBecauseMaximumAmntFirstRefinements);
    pOut.println(
        "Amount of Strategies which where refined away:                          "
            + amountStrategiesRefinedAway);
    pOut.println(
        "Amount of distinct Strategies after refinements:                        "
            + distinctNodesWithStrategies);
  }

  public void increaseDoubleRefinements() {
    this.doubleRefinementsMade += 1;
  }

  public void increaseStrategiesRefinedAway() {
    this.amountStrategiesRefinedAway += 1;
  }

  public void recalculateDistinctStartegies() {
    Set<StrategiesEnum> ignoredStrategies = new HashSet<>();
    ignoredStrategies.add(StrategiesEnum.BASE);
    distinctNodesWithStrategies =
        summaryInformation
            .getDistinctNodesWithStrategiesWithoutDissallowed(ignoredStrategies)
            .size();
  }

  public void increaseDoubleRefinementsCausedByMaximumAmountFirstRefinements() {
    this.doubleRefinementsBecauseMaximumAmntFirstRefinements += 1;
  }

  @Override
  public String getName() {
    return "Summary refiner statistics";
  }
}
