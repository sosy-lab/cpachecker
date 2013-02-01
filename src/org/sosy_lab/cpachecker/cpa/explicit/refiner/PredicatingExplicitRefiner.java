/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit.refiner;

import java.io.PrintStream;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;

// TODO: check whether this class is needed at all or if it can just be replaced by PredicateRefiner
class PredicatingExplicitRefiner extends PredicateRefiner {

  protected PredicatingExplicitRefiner(Configuration pConfig,
      LogManager pLogger, ConfigurableProgramAnalysis pCpa,
      InterpolationManager pInterpolationManager,
      final FormulaManagerView pFormulaManager,
      final AbstractionManager pAbstractionManager, PathFormulaManager pPathFormulaManager)
          throws CPAException, InvalidConfigurationException {
    super(pConfig, pLogger, pCpa, pInterpolationManager, pPathFormulaManager,
        new PredicatingExplicitRefinementStrategy(pConfig, pLogger, pFormulaManager, pAbstractionManager));
  }

  // overridden just for visibility
  @Override
  protected CounterexampleInfo performRefinement(ARGReachedSet pReached, ARGPath pPath) throws CPAException,
      InterruptedException {
    return super.performRefinement(pReached, pPath);
  }

  // overridden just for visibility
  @Override
  protected void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    super.printStatistics(pOut, pResult, pReached);
  }
}
