// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.faultlocalizationrankingmetrics;

import static com.google.common.collect.FluentIterable.from;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.AbstractStates;
/** Class represents the safe case for algorithm which works with ranking metric */
public class SafeCase {
  private final ReachedSet pReachedSet;

  public SafeCase(ReachedSet pPReachedSet) {
    this.pReachedSet = pPReachedSet;
  }
  /**
   * Gets safe states (safe leaves) from ARG.
   *
   * @return Detected safe states.
   */
  private List<ARGState> getSafeStates() {
    return from(rootState().getSubgraph())
        .filter(e -> !e.isCovered() && !e.isTarget() && e.getChildren().isEmpty())
        .toList();
  }

  /**
   * Gets all possible safe paths.
   *
   * @return Detected safe edges.
   */
  public Set<ARGPath> getSafePaths() {
    Set<ARGPath> allSafePathsTogether = new HashSet<>();

    for (ARGState safeState : getSafeStates()) {
      allSafePathsTogether.addAll(FaultLocalizationUtils.getAllPaths(pReachedSet, safeState));
    }
    return allSafePathsTogether;
  }

  /**
   * Checks whether there is a safe path in the ARG or not.
   *
   * @return Returns <code>true</code> if the path exists otherwise returns <code>false</code>
   */
  public boolean existsSafePath() {
    return !getSafeStates().isEmpty();
  }
  /**
   * Gets root state from reachedSet.
   *
   * @return ARG root state.
   */
  private ARGState rootState() {
    return AbstractStates.extractStateByType(pReachedSet.getFirstState(), ARGState.class);
  }
}
