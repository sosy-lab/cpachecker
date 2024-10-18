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
import java.util.Objects;
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
  /**
   * A mapping of ARGState objects to their corresponding classification strings.
   */
  private Map<ARGState,String> stateClassification;
  /**
   * Represents an algorithm to be executed on a set of abstract states.
   */
  private final Algorithm algorithm;
  /**
   * Represents a LogManager instance used for logging messages within the TubeAlgorithm class.
   */
  private final LogManager logger;
  /**
   * Represents a Configurable Program Analysis that provides various components for static program analysis.
   *
   * The ConfigurableProgramAnalysis interface contains methods to retrieve the abstract domain,
   * transfer relation, merge operator, stop operator, as well as initial state and precision adjustment operators.
   */
  private final ConfigurableProgramAnalysis cpa;
  /**
   * Initializes a TubeAlgorithm with the given Algorithm, ConfigurableProgramAnalysis, and LogManager.
   *
   * @param pAlgorithm The Algorithm to run before performing additional operations
   * @param pCPA The ConfigurableProgramAnalysis on which to operate
   * @param pLogger The LogManager for logging messages
   */
  public TubeAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA, LogManager pLogger){
    algorithm = pAlgorithm;
    cpa = pCPA;
    this.logger = pLogger;
    this.stateClassification = new HashMap<>();
  }

  /**
   * Check if a given boolean formula is unsatisfiable using the PredicateCPA solver.
   *
   * @param f The boolean formula to check for unsatisfiability
   * @return True if the formula is unsatisfiable, false otherwise
   * @throws SolverException If an error occurs in the solver
   * @throws InterruptedException If the operation is interrupted
   */
  public boolean test(BooleanFormula f) throws SolverException, InterruptedException {
    return CPAs.retrieveCPA(cpa, PredicateCPA.class).getSolver().isUnsat(f);
  }

  /**
   * Runs the algorithm on the given reached set and classifies the states within it as negated or not negated,
   * based on specific criteria. Then, determines if the classification is sound and/or under approximation
   * and logs the result accordingly.
   *
   * @param reachedSet The set of states to run the algorithm on
   * @return The status of the algorithm execution, indicating the soundness and precision of the results
   * @throws CPAException If an error occurs during the analysis
   * @throws InterruptedException If the analysis is interrupted
   */
  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = algorithm.run(reachedSet);
    Set<TubeState> negatedStates = new HashSet<>();
    Set<TubeState> notNegatedStates = new HashSet<>();
    for (AbstractState abstractState : reachedSet) {
      if (((ARGState) abstractState).isTarget() || ((ARGState) abstractState).getChildren().isEmpty() && Objects.requireNonNull(
          AbstractStates.extractLocation(abstractState)).getNumLeavingEdges() == 0) {
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
    boolean isUnderApprox = notNegatedStates.stream().allMatch(s -> s.getErrorCounter() > 0);


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
