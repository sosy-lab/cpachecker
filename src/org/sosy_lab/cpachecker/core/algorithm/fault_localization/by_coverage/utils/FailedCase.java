// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_coverage.utils;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.AbstractStates;

/** Class represents the error case for algorithm which works with ranking metric. */
public class FailedCase {
  private final ReachedSet reachedSet;

  public FailedCase(ReachedSet pReachedSet) {
    reachedSet = pReachedSet;
  }

  /**
   * Gets all possible error paths.
   *
   * @return all error paths.
   */
  public Set<ARGPath> getErrorPaths() {
    Set<ARGPath> allErrorPathsTogether = new HashSet<>();

    Iterable<ARGState> targetStates =
        AbstractStates.projectToType(AbstractStates.getTargetStates(reachedSet), ARGState.class);
    for (ARGState errorState : targetStates) {
      allErrorPathsTogether.addAll(ARGUtils.getAllPaths(reachedSet, errorState));
    }
    return allErrorPathsTogether;
  }

  /**
   * Checks whether there is a false paths in the ARG or not.
   *
   * @return Returns <code>true</code> if the path exists otherwise returns <code>false</code>
   */
  public boolean existsErrorPath() {
    return !getErrorPaths().isEmpty();
  }
  /**
   * Checks whether the path is a failed path or not.
   *
   * @param path The chosen path.
   * @return <code>boolean</code>
   */
  public boolean isFailedPath(ARGPath path) {
    return path.getLastState().isTarget();
  }
}
