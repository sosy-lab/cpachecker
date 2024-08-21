// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.refiner;

import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public interface UnsoundRefiner extends Refiner {

  /**
   * Any unsound refiner, like, e.g., the {@link ValueAnalysisImpactRefiner} whose refinement
   * procedure leaves the coverage relation in an inconsistent state, must ensure that a complete
   * re-exploration of the state-space must be performed before finishing the analysis.
   *
   * <p>To this end, all states except the root state must be removed from the reached set, and a
   * valid precision must be put in place, e.g. by calling the respective {@link
   * ARGReachedSet#removeSubtree(ARGState)} method.
   */
  void forceRestart(ReachedSet reached) throws InterruptedException;
}
