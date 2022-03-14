// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Alternative to {@link Refiner} for refiners that are based on using an ARG. The refiner is
 * supplied with the error path through the ARG on refinement.
 *
 * <p>Use {@link AbstractARGBasedRefiner#forARGBasedRefiner(ARGBasedRefiner,
 * org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis)} to create a {@link Refiner}
 * instance from an instance of this interface.
 */
public interface ARGBasedRefiner {

  /**
   * Perform refinement.
   *
   * @param pReached the reached set
   * @param pPath the potential error path
   * @return Information about the counterexample.
   */
  CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException;
}
