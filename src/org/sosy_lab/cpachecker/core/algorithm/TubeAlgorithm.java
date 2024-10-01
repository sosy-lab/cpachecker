// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.tube.TubeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.logging.Level;

/**
 * This class represents a TubeAlgorithm that performs additional operations on the reached set after running another algorithm.
 */
public class TubeAlgorithm implements Algorithm{
  /**
   *
   */
  private final Algorithm algorithm;
  private final LogManager logger;

  /**
   * This class represents a TubeAlgorithm that performs additional operations on the reached set after running another algorithm.
   */
  public TubeAlgorithm(Algorithm pAlgorithm, LogManager pLogger){
    algorithm = pAlgorithm;

    this.logger = pLogger;
  }
  /**
   * Runs the algorithm on the given reached set and performs additional operations.
   *
   * @param reachedSet The reached set on which the algorithm will be run.
   * @return The status of the algorithm after execution.
   * @throws CPAException If an error occurs during the execution of the algorithm.
   * @throws InterruptedException If the execution of the algorithm is interrupted.
   */
  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = algorithm.run(reachedSet);
    for (AbstractState abstractState : reachedSet) {
      ARGState state = (ARGState) abstractState;
      if (state.getChildren().isEmpty()) {
        TubeState tubeState = AbstractStates.extractStateByType(state, TubeState.class);

        if (tubeState != null) {
          if (isTubeOverApprox(tubeState) && isTubeUnderApprox(tubeState)){
            logger.log(Level.INFO,"precise");
          }else if (isTubeUnderApprox(tubeState)){
            logger.log(Level.INFO,"under");
          }else if (isTubeOverApprox(tubeState)){
            logger.log(Level.INFO,"over");
          }
        }
      }
    }
    return status;
  }

  /**
   * Checks if the given TubeState represents a tube that is under approximation.
   *
   * @param tubeState The TubeState to check.
   * @return True if the tube is under approximation, false otherwise.
   */
  private boolean isTubeUnderApprox(TubeState tubeState) {
    // first rule: if there is no negation on the TubeState,
    // and all leafs have an error count greater then 0,
    // then the tube is under approx.
    return !tubeState.isNegated() && tubeState.getErrorCounter() >= 1;
  }

  /**
   * Checks if the given TubeState represents a tube that is over approximation.
   *
   * @param tubeState The TubeState to check.
   * @return True if the tube is over approximation, false otherwise.
   */
  private boolean isTubeOverApprox(TubeState tubeState) {
    // second rule: if there is at least one negation in the TubeState
    // and all leafs have error count of 0, then the tube is over approx.
    return tubeState.isNegated() && tubeState.getErrorCounter() == 0;
  }
}
