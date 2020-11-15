// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.AbstractStates;

/** Class represents the safe case for algorithm which works with ranking metric . */
public class SafeCase {
  private final ReachedSet reachedSet;

  public SafeCase(ReachedSet pReachedSet) {
    reachedSet = pReachedSet;
  }

  /**
   * Gets safe states (safe leaves) from ARG.
   *
   * @return Detected safe states.
   */
  private ImmutableList<ARGState> getSafeStates() {
    return getRootState()
        .getSubgraph()
        .filter(e -> !e.isCovered() && !e.isTarget() && e.getChildren().isEmpty())
        .toList();
  }

  /**
   * Gets all possible safe paths.
   *
   * @return Detected safe edges.
   */
  public ImmutableSet<ARGPath> getSafePaths() {
    ImmutableSet.Builder<ARGPath> allSafePathsTogether = ImmutableSet.builder();

    for (ARGState safeState : getSafeStates()) {
      allSafePathsTogether.addAll(ARGUtils.getAllPaths(reachedSet, safeState));
    }
    return allSafePathsTogether.build();
  }

  /**
   * Checks whether there is a safe path in the ARG or not.
   *
   * @return Returns <code>true</code> if the path exists otherwise returns <code>false</code>.
   */
  public boolean existsSafePath() {
    return !getSafeStates().isEmpty();
  }
  /**
   * Gets root state from reachedSet.
   *
   * @return ARG root state.
   */
  private ARGState getRootState() {
    return AbstractStates.extractStateByType(reachedSet.getFirstState(), ARGState.class);
  }
}
