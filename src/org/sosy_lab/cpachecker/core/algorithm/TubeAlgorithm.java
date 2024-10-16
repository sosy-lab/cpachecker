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
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.tube.TubeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.logging.Level;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * This class represents a TubeAlgorithm that performs additional operations on the reached set after running another algorithm.
 */


public class TubeAlgorithm implements Algorithm{
  private Map<ARGState,String> stateClassification;
  private final Algorithm algorithm;

  private final LogManager logger;
  private final ConfigurableProgramAnalysis cpa;
  /**
   * This class represents a TubeAlgorithm that performs additional operations on the reached set after running another algorithm.
   */
  public TubeAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA, LogManager pLogger){
    algorithm = pAlgorithm;
    cpa = pCPA;
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

  public boolean test(BooleanFormula f) throws SolverException, InterruptedException {
    return CPAs.retrieveCPA(cpa, PredicateCPA.class).getSolver().isUnsat(f);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = algorithm.run(reachedSet);
    Set<TubeState> negatedStates = new HashSet<>();
    Set<TubeState> notNegatedStates = new HashSet<>();
    for (AbstractState abstractState : reachedSet) {
      if (((ARGState) abstractState).isTarget() || ((ARGState) abstractState).getChildren().isEmpty() && AbstractStates.extractLocation(abstractState).getNumLeavingEdges() == 0) {
        TubeState tubeState = AbstractStates.extractStateByType(abstractState, TubeState.class);
        assert tubeState != null;
        if (tubeState.getIsNegated()) {
          negatedStates.add(tubeState);
        } else {
          notNegatedStates.add(tubeState);
        }
      }
    }

    boolean isSound = negatedStates.stream().allMatch(s -> s.getErrorCounter() == 0);

    //boolean isSound2 = negatedStates.stream().anyMatch(s -> s.getErrorCounter() == 0);

    boolean isUnderApprox = notNegatedStates.stream().allMatch(s -> s.getErrorCounter() > 0);
    // boolean isUnderApprox2 = notNegatedStates.stream().anyMatch(s -> s.getErrorCounter() > 0);

    if(isSound && isUnderApprox){
      logger.log(Level.INFO, "precise");
    }else if(isSound){
      logger.log(Level.INFO, "over");
    }else if(isUnderApprox){
      logger.log(Level.INFO, "under");
    }else{
      logger.log(Level.INFO, "unclassified");
    }

    return status;
  }


}
