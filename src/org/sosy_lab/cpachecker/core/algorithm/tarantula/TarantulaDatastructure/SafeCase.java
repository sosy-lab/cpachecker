/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaDatastructure;

import static com.google.common.collect.FluentIterable.from;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaUtils;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class SafeCase {
  private final ReachedSet pReachedSet;
  private int totalSafeCases = 0;

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
        .filter(
            e -> {
              assert e != null;
              return !e.isCovered() && !e.isTarget();
            })
        .filter(
            s -> {
              assert s != null;
              return s.getChildren().isEmpty();
            })
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
      if (existsSafePath()) {
        allSafePathsTogether.addAll(TarantulaUtils.getAllPaths(pReachedSet, safeState));
      }
    }
    return allSafePathsTogether;
  }

  /**
   * Checks whether there is a safe path in the ARG or not.
   *
   * @return Returns <code>true</code> if the path exists otherwise returns <code>false</code>
   */
  public boolean existsSafePath() {
    if (!getSafeStates().isEmpty()) {
      return true;
    }

    return false;
  }
  /**
   * Gets root state from reachedSet.
   *
   * @return ARG root state.
   */
  private ARGState rootState() {
    return AbstractStates.extractStateByType(pReachedSet.getFirstState(), ARGState.class);
  }
  /**
   * Gets the total passed cases.
   *
   * @return Number of total passed cases.
   */
  public int getTotalSafeCases() {

    return totalSafeCases == 0 ? totalSafeCases = getSafePaths().size() : totalSafeCases;
  }
}
