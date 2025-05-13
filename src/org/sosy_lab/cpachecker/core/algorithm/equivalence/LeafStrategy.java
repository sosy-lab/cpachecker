// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.equivalence;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.Set;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.EquivalenceRunner.SafeAndUnsafeConstraints;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;

public interface LeafStrategy {

  static FluentIterable<ARGState> filterStatesWithNoChildren(ReachedSet pReachedSet) {
    return FluentIterable.from(pReachedSet)
        .filter(ARGState.class)
        .filter(state -> state.getChildren().isEmpty());
  }

  static ImmutableList<Integer> findTouchedLines(ReachedSet pReachedSet) {
    Set<CFANode> nodes =
        FluentIterable.from(pReachedSet).transform(AbstractStates::extractLocation).toSet();
    ImmutableList.Builder<Integer> touchedLines = ImmutableList.builder();
    for (CFANode node : nodes) {
      for (CFAEdge leaving : CFAUtils.allLeavingEdges(node)) {
        if (nodes.contains(leaving.getSuccessor())) {
          touchedLines.add(leaving.getLineNumber());
        }
      }
    }
    return ImmutableList.sortedCopyOf(touchedLines.build());
  }

  SafeAndUnsafeConstraints export(ReachedSet pReachedSet, CFA pCfa, AlgorithmStatus pStatus)
      throws CPAException, InterruptedException, InvalidConfigurationException;
}
