// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.residualprogram;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.residualprogram.TestGoalToConditionConverterAlgorithm.LeafStates;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/** A strategy to determine all (un)covered nodes we eventually wil generate our condition from. */
public interface IGoalFindingStrategy {
  /**
   * Finds all goals that are interesting for our analysis.
   *
   * @param pWaitlist A list of all exit nodes of the program.
   * @return A map of all intersting goals, partitioned into covered/ not covered.
   */
  Map<LeafStates, List<CFANode>> findGoals(
      Deque<ARGState> pWaitlist, final Set<String> coveredGoals)
      throws CPAException, InterruptedException;
}
