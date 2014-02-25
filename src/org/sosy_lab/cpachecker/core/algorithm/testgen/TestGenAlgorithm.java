/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.testgen;

import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.dummygen.ARGStateDummyCreator;
import org.sosy_lab.cpachecker.core.algorithm.testgen.dummygen.ExplicitTestcaseGenerator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;

@Options(prefix="testgen")
public class TestGenAlgorithm implements Algorithm {

  private Algorithm algorithm;
  private LogManager logger;
  private CFA cfa;
  private ExplicitTestcaseGenerator explicitAlg;
  private ARGStateDummyCreator dummyCreator;



  public TestGenAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa,
                             ShutdownNotifier pShutdownNotifier, CFA pCfa, String filename,
                             Configuration config, LogManager pLogger) throws InvalidConfigurationException, CPAException {
    cfa = pCfa;
    config.inject(this);
    this.algorithm = pAlgorithm;
    this.logger = pLogger;
    algorithm = pAlgorithm;

    dummyCreator = new ARGStateDummyCreator(pCpa, logger);
    /*TODO change the config file, so we can configure 'dfs'*/
//    Configuration testCaseConfig = Configuration.copyWithNewPrefix(config, "testgen.");
    explicitAlg = new ExplicitTestcaseGenerator(config, logger, pShutdownNotifier, cfa, filename);
  }


  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      PredicatedAnalysisPropertyViolationException {
    boolean pathToExplore = true;
    boolean success = true;
    /* run the given alg ones using the "config/specification/onepathloopautomaton.spc" and DFS */
    success &= algorithm.run(pReachedSet);
    do {
      /**/
      ARGState currentRootState;
      Set<ARGState> errorPathStates;
//      dummyCreator.computeOtherSuccessor(pState, pNotToChildState);
      /**/
//      pReachedSet.getFirstState()
//      ARGUtils.getPathFromBranchingInformation(root, arg, branchingInformation)
//      explicitAlg.analysePath(currentRootState, null, errorPathStates);
    }while(pathToExplore & success);
    return false;
  }

}
