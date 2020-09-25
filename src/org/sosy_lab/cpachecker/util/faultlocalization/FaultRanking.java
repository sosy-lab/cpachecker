// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

@FunctionalInterface
public interface FaultRanking {

  /**
   * Rank the input set for visual representation in the ReportManager.
   *
   * If more than just one parameter is needed (here: result) a class that
   * implements this interface can be created.
   * For more details and an example see MaximalLineDistanceRanking.
   * To concatenate multiple heuristics FaultRankingUtils.concatHeuristics() can be used.
   * @param result The result of any fault localization algorithm
   * @return a ranked list of all contained faults.
   * @see FaultRankingUtils#concatHeuristics(Function, FaultRanking...)
   * @see FaultRankingUtils#concatHeuristicsDefaultFinalScoring(FaultRanking...)
   * @see org.sosy_lab.cpachecker.util.faultlocalization.ranking.MaximalLineDistanceRanking
   */
  List<Fault> rank(Set<Fault> result);
}
