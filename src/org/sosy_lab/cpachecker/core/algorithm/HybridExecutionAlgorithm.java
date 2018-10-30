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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This class implements a CPA algorithm based on the idea of concolic execution
 * It traversals the CFA via DFS and introduces new edges for the ARG on branching states in order to cover more branches
 */
public class HybridExecutionAlgorithm implements Algorithm {

  @Options(prefix = "hybridExecution")
  public static class HybridExecutionAlgorithmFactory implements AlgorithmFactory {

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
      return new HybridExecutionAlgorithm(algorithm, logger, dfsMaxDepth, useValueSets);
    }

  }

  private HybridExecutionAlgorithm(
    Algorithm algorithm, 
    LogManager logger, 
    int dfsMaxDepth,
    boolean useValueSets) {

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


    
    return null;
  }

}