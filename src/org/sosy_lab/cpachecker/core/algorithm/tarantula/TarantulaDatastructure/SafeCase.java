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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaUtils;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class SafeCase {
  private final ReachedSet pReachedSet;
  private final ARGState root;

  public SafeCase(ReachedSet pPReachedSet, ARGState pRoot) {
    this.pReachedSet = pPReachedSet;
    this.root = pRoot;
  }
  /**
   * Gets safe states (safe leaves) from ARG.
   *
   * @param pRoot root at ARG.
   * @return Detected safe states.
   */
  private List<ARGState> getSafeStates() {
    return from(root.getSubgraph())
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
   * Gets safe states in case of loop (safe leaves) from ARG.
   *
   * @return Detected safe loop states.
   */
  private List<ARGState> getSafeLoopStates() {

    return from(root.getSubgraph())
        .filter(
            e -> {
              assert e != null;
              return e.isCovered() && !e.isTarget();
            })
        .filter(
            s -> {
              assert s != null;
              return s.getChildren().isEmpty();
            })
        .toList();
  }
  /**
   * Gets two dimensional CFAEdge list of the safe paths.
   *
   * @return Detected safe edges.
   */
  public Set<List<CFAEdge>> getEdgesOfSafePaths() {

    Set<List<CFAEdge>> allSafePathsTogether = new HashSet<>();

    for (ARGState safePath : mergeAllSafeStates(getSafeStates(), getSafeLoopStates())) {

      if (existsSafePath()) {
        allSafePathsTogether.addAll(TarantulaUtils.getAllPaths(pReachedSet, safePath));
      }
    }
    return allSafePathsTogether;
  }
  /**
   * Merges safe states and safe states in case of loop from ARG together.
   *
   * @return Merged all safe states.
   */
  public static List<ARGState> mergeAllSafeStates(
      List<ARGState> safeStates, List<ARGState> safeLoopStates) {

    List<ARGState> mergedAllSafeStates = new ArrayList<>(safeStates);

    List<ARGState> getSafeStatesOfLoop = safeLoopStates;
    if (getSafeStatesOfLoop != null) {
      mergedAllSafeStates.addAll(getSafeStatesOfLoop);
    }
    return mergedAllSafeStates;
  }
  /**
   * Checks whether there is a safe paths in the ARG or not.
   *
   * @return Returns <code>true</code> if the path exists otherwise returns <code>false</code>
   */
  public boolean existsSafePath() {

    for (AbstractState state : pReachedSet) {
      if (getSafeStates().contains(state)) {
        return true;
      }
    }

    return false;
  }
}
