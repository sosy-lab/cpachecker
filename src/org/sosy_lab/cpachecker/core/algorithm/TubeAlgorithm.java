// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
  private Map<ARGState,String> stateClassification;
  private final Algorithm algorithm;

  private final LogManager logger;
  /**
   * This class represents a TubeAlgorithm that performs additional operations on the reached set after running another algorithm.
   */
  public TubeAlgorithm(Algorithm pAlgorithm, LogManager pLogger){
    algorithm = pAlgorithm;

    this.logger = pLogger;
    this.stateClassification = new HashMap<>();
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

      if (state.getChildren().isEmpty()) { // This state is a leaf-node
        TubeState tubeState = AbstractStates.extractStateByType(state, TubeState.class);

        if (tubeState != null) {
          String classification;
          if (isTubeOverApprox(tubeState) && isTubeUnderApprox(tubeState)){
            classification = "precise";
          } else if (isTubeUnderApprox(tubeState)){
            classification = "under";
          } else if (isTubeOverApprox(tubeState)) {
            classification = "over";
          } else {
            classification = "unclassified";
          }
          stateClassification.put(state, classification); // Save the classification
        }
      }
    }
    Set<String> uniqueValues = new HashSet<>(stateClassification.values());

    if (uniqueValues.size() == 1 && uniqueValues.contains("over")) {
      logger.log(Level.INFO, "over");
    } else if (uniqueValues.size() == 1 && uniqueValues.contains("under")) {
      logger.log(Level.INFO, "under");
    } else if (uniqueValues.contains("over") && uniqueValues.contains("under")) {
      logger.log(Level.INFO, "precise");
    } else {
      logger.log(Level.INFO, "unclassified");
    }
    return status;
  }

  private boolean isTubeUnderApprox(TubeState tubeState) {
    // first rule: if there is no negation on the TubeState,
    // and all leafs have an error count greater than 0,
    // then the tube is under approx.
    return !tubeState.isNegated() && tubeState.getErrorCounter() >= 1;
  }

  private boolean isTubeOverApprox(TubeState tubeState) {
    // second rule: if there is at least one negation in the TubeState
    // and all leafs have error count of 0, then the tube is over approx.
    return tubeState.isNegated() && tubeState.getErrorCounter() == 0;
  }
}
