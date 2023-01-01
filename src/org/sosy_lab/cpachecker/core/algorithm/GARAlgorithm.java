// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public interface GARAlgorithm extends Algorithm {

  /**
   * Run the algorithm on the given set of abstract states and the given waitlist.
   *
   * @param reachedSet Input.
   * @return information about how reliable the result is
   * @throws CPAException may be thrown by implementors
   * @throws InterruptedException may be thrown by implementors
   */
  @Override
  default AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    // TODO: Determine the algorithm status from facts about the spaceExploration, projection,
    // refiner and refiner condition functions.
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    while (true) {

      spaceExploration(reachedSet);
      projection(reachedSet);
      if (refinementNecessary(reachedSet)) {
        refine(reachedSet);
      } else {
        return status;
      }
    }
  }

  /**
   * Explore the state space using the given reached set and waitlist.
   *
   * @param reachedSet Input.
   */
  void spaceExploration(ReachedSet reachedSet);

  /**
   * Projects the reached set which has been proven on some abstraction into a reasonable abstraction of the
   * original program.
   *
   * @param reachedSet Input.
   */
  void projection(ReachedSet reachedSet);

  /**
   * Checks whether a refinement is necessary for the current abstraction.
   *
   * @param reachedSet the current abstraction.
   * @return whether the current abstraction needs to be refined.
   */
  boolean refinementNecessary(ReachedSet reachedSet);

  /**
   * Refine the current abstraction.
   *
   * @param reachedSet the current abstraction to be refined.
   */
  void refine(ReachedSet reachedSet);
}
