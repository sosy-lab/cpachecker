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
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.collect.FluentIterable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class FailedCase {
  private final ReachedSet pReachedSet;

  public FailedCase(ReachedSet pPReachedSet) {
    this.pReachedSet = pPReachedSet;
  }
  /**
   * Gets all CFAEdges from generated counter Examples and consider this as failed paths.
   *
   * @return Detected all failed Paths.
   */
  public Set<List<CFAEdge>> getFailedPaths() {

    Set<List<CFAEdge>> failedPaths = new HashSet<>();
    for (CounterexampleInfo counterExample : getCounterExamples()) {
      failedPaths.add(counterExample.getTargetPath().getFullPath());
    }
    return failedPaths;
  }

  public FluentIterable<CounterexampleInfo> getCounterExamples() {

    return Optionals.presentInstances(
        from(pReachedSet)
            .filter(IS_TARGET_STATE)
            .filter(ARGState.class)
            .transform(ARGState::getCounterexampleInformation));
  }
  /**
   * Checks whether there is a false paths in the ARG or not.
   *
   * @return Returns <code>true</code> if the path exists otherwise returns <code>false</code>
   */
  public boolean existsErrorPath() {

    for (AbstractState state : pReachedSet) {
      if (AbstractStates.isTargetState(state)) {
        return true;
      }
    }

    return false;
  }
  /**
   * Checks whether the path is a failed path or not.
   *
   * @param path The chosen path.
   * @return <code>boolean</code>
   */
  public boolean isFailedPath(List<CFAEdge> path) {
    List<ARGState> targetStates = ARGUtils.getErrorStates(pReachedSet);

    for (AbstractState targetState : targetStates) {
      CFANode nodeOfTargetState = AbstractStates.extractLocation(targetState);
      if (path.get(path.size() - 1).getSuccessor().equals(nodeOfTargetState)) {
        return true;
      }
    }

    return false;
  }
  /**
   * Calculates how many total failed cases are in ARG.
   *
   * @return how many failed cases are found.
   */
  private int totalFailed() {

    return getCounterExamples().size();
  }
}
