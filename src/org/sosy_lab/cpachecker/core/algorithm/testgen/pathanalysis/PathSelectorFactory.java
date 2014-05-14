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
package org.sosy_lab.cpachecker.core.algorithm.testgen.pathanalysis;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.testgen.TestGenAlgorithm.AnalysisStrategySelector;
import org.sosy_lab.cpachecker.core.algorithm.testgen.TestGenStatistics;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;


public class PathSelectorFactory {

  private StartupConfig startupConfig;



  public PathSelectorFactory(StartupConfig pStartupConfig) {
    startupConfig = pStartupConfig;
  }

  public PathValidator createValidator(AnalysisStrategySelector selector, CFA pCfa) throws InvalidConfigurationException{
    PathChecker pathChecker = createPathChecker(pCfa);
    PathValidator validator;
    switch (selector) {
    case CUTE_PATH_SELECTOR:
      validator = new CUTEPathValidator(pathChecker, startupConfig);
      break;
    case CFA_TRACKING:
      validator = new CFATrackingPathValidator(pathChecker, startupConfig);
      break;
    case CUTE_LIKE:
    case LOCATION_AND_VALUE_STATE_TRACKING:
      throw new IllegalArgumentException("Currently not supported strategy with a validator");
    default:
      throw new IllegalStateException("Not all analysisStrategySelector cases matched");
    }
    return validator;
  }

  public PathSelector createPathSelector(PathValidator pPathValidator, TestGenStatistics stats){
    return new BasicPathSelector(pPathValidator,startupConfig, stats);
  }

  @SuppressWarnings("deprecation") //suppresses deprecated CUTEBasicPathSelector
  public PathSelector createPathSelector(AnalysisStrategySelector selector, CFA pCfa, TestGenStatistics stats) throws InvalidConfigurationException{
    PathSelector analysisStrategy;
    switch (selector) {
    case LOCATION_AND_VALUE_STATE_TRACKING:
      analysisStrategy = new LocationAndValueStateTrackingPathAnalysisStrategy(createPathChecker(pCfa), startupConfig, stats);
      break;
    case CUTE_LIKE:
      analysisStrategy = new CUTEBasicPathSelector(createPathChecker(pCfa), startupConfig, stats);
      break;
    case CUTE_PATH_SELECTOR:
    case CFA_TRACKING:
      analysisStrategy = createPathSelector(createValidator(selector, pCfa), stats);
      break;
    default:
      throw new IllegalStateException("Not all analysisStrategySelector cases matched");
    }
    return analysisStrategy;
  }

  private PathChecker createPathChecker(CFA pCfa) throws InvalidConfigurationException
  {
    FormulaManagerFactory formulaManagerFactory =
        new FormulaManagerFactory(startupConfig.getConfig(), startupConfig.getLog(),
            ShutdownNotifier.createWithParent(startupConfig.getShutdownNotifier()));
    FormulaManagerView formulaManager =
        new FormulaManagerView(formulaManagerFactory.getFormulaManager(), startupConfig.getConfig(), startupConfig.getLog());
    PathFormulaManager pfMgr = new PathFormulaManagerImpl(formulaManager, startupConfig.getConfig(), startupConfig.getLog(), startupConfig.getShutdownNotifier(), pCfa);
    Solver solver = new Solver(formulaManager, formulaManagerFactory);
    PathChecker pathChecker = new PathChecker(startupConfig.getLog(), pfMgr, solver, pCfa.getMachineModel());
    return pathChecker;
  }
}
