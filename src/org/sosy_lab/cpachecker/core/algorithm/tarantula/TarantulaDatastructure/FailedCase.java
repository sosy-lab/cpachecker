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

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.tarantula.TarantulaUtils;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

public class FailedCase {
  private final ReachedSet pReachedSet;
  int totalErrorCases;

  public FailedCase(ReachedSet pPReachedSet) {
    this.pReachedSet = pPReachedSet;
  }
  /**
   * Gets all CFAEdges from generated counter Examples and consider this as failed paths.
   *
   * @return Detected all failed Paths.
   */
  public Set<ARGPath> getErrorPaths() {
    Set<ARGPath> allErrorPathsTogether = new HashSet<>();

    for (ARGState safeState : ARGUtils.getErrorStates(pReachedSet)) {
      allErrorPathsTogether.addAll(TarantulaUtils.getAllPaths(pReachedSet, safeState));
    }
    return allErrorPathsTogether;
  }

  /**
   * Checks whether there is a false paths in the ARG or not.
   *
   * @return Returns <code>true</code> if the path exists otherwise returns <code>false</code>
   */
  public boolean existsErrorPath() {

    if (!getErrorPaths().isEmpty()) {
      return true;
    }

    return false;
  }
  /**
   * Checks whether the path is a failed path or not.
   *
   * @param path The chosen path.
   * @return <code>boolean</code>
   */
  public boolean isFailedPath(ARGPath path) {

    if (path.getLastState().isTarget()) {
      return true;
    }
    return false;
  }
  /**
   * Gets the total failed cases.
   *
   * @return Number of total failed cases.
   */
  public int getTotalErrorCases() {

    return totalErrorCases == 0 ? totalErrorCases = getErrorPaths().size() : totalErrorCases;
  }
}
