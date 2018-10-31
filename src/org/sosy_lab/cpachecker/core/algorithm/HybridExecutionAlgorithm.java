/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
/**
 * The core algorithms of CPAchecker.
 */
package org.sosy_lab.cpachecker.core.algorithm;

import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ReachedSetUpdateListener;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ReachedSetUpdater;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This class implements a CPA algorithm based on the idea of concolic execution
 * It traversals the CFA via DFS and introduces new edges for the ARG on branching states in order to cover more branches
 */
public class HybridExecutionAlgorithm implements Algorithm, ReachedSetUpdater {

  @Options(prefix = "hybridExecution")
  public static class HybridExecutionAlgorithmFactory implements AlgorithmFactory {

    @Option(secure=true, name="unboundedDFS", description="Use dfs algorithm unbounded")
    private boolean unbounded = false;

    @Option(secure=true, name="dfsMaxDepth", description="The maximum depth for the dfs algorithm")
    private int dfsMaxDepth = 60;

    @Option(secure=true, name="useValueSets", description="Wether to use multiple values on a state")
    private boolean useValueSets = false;

    private final Algorithm algorithm;
    private final LogManager logger;

    public HybridExecutionAlgorithmFactory(
      Algorithm algorithm, 
      LogManager logger, 
      Configuration configuration) throws InvalidConfigurationException {
        configuration.inject(this);
        this.algorithm = algorithm;
        this.logger = logger;
      }

    @Override
    public Algorithm newInstance() {
      return new HybridExecutionAlgorithm(algorithm, logger, unbounded, dfsMaxDepth, useValueSets);
    }

  }

  private final Algorithm algorithm;
  private final LogManager logger;
  private final boolean unbounded;
  private int dfsMaxDepth;
  private boolean useValueSets;
  private final List<ReachedSetUpdateListener> reachedSetUpdateListeners;

  private HybridExecutionAlgorithm(
    Algorithm algorithm, 
    LogManager logger,
    boolean unbounded, 
    int dfsMaxDepth,
    boolean useValueSets) {

      this.algorithm = algorithm;
      this.logger = logger;
      this.unbounded = unbounded;
      this.dfsMaxDepth = dfsMaxDepth;
      this.useValueSets = useValueSets;
      this.reachedSetUpdateListeners = new LinkedList<>();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    // ( LeafExpression#of creates a new ExpressionTree out of a CExpression
    // ToFormulaVisitor defines a BooleanFormula out the the ExpressionTree
    // BooleanFormulaManagerView#and(BooleanFormula... args) builds a conjunction over all formulas )

    
    // ARGUtils#getOnePathTo(ARGState ..) to get the complete path to the defined ARGState
    // PathFormulaManager to build the BooleanFormula
    
    // Solver#isUnsat
    // Solver#getProverEnvironment
    // Prover#getModelAsAssignments

    // ReachedSet#add

    // ARGCPA is needed for hybrid execution

    // start with good status
    AlgorithmStatus currentStatus = AlgorithmStatus.SOUND_AND_PRECISE;

    boolean running = true;
    while(running) {


      // get the next state from the waitlist
      AbstractState currentState = pReachedSet.popFromWaitlist();

      // check for reached set updates and notify
      // if(>>updated<<) {
      //   notifyListeners(pReachedSet);
      // }

      // check for continuation
      running = checkContinue(pReachedSet, currentState);

      // update status on arbitrary conditions
    }
    
    return currentStatus;
  }

  @Override
  public void register(ReachedSetUpdateListener pReachedSetUpdateListener) {

    if(algorithm instanceof ReachedSetUpdater) {
      ((ReachedSetUpdater) algorithm).register(pReachedSetUpdateListener);
    }

    reachedSetUpdateListeners.add(pReachedSetUpdateListener);
  }

  @Override
  public void unregister(ReachedSetUpdateListener pReachedSetUpdateListener) {

    if(algorithm instanceof ReachedSetUpdater) {
      ((ReachedSetUpdater) algorithm).unregister(pReachedSetUpdateListener);
    }

    reachedSetUpdateListeners.remove(pReachedSetUpdateListener);
  }

  // notify listeners on update of the reached set
  private void notifyListeners(ReachedSet pReachedSet) {
    reachedSetUpdateListeners.forEach(listener -> listener.updated(pReachedSet));
  }

  // check if the algorithm should continue 
  private boolean checkContinue(ReachedSet pReachedSet, AbstractState currentState) {

    boolean check = true;
    // some checks for the state
    // update check

    check = check && pReachedSet.hasWaitingState();
    return check;
  }

}