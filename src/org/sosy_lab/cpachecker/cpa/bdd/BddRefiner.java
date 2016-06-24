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
package org.sosy_lab.cpachecker.cpa.bdd;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisPathInterpolator;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStrongestPostOperator;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

/**
 * Refiner implementation that delegates to {@link ValueAnalysisPathInterpolator}.
 */
public abstract class BddRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis cpa)
      throws InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(BddRefiner.class.getSimpleName() + " could not find the BDDCPA");
    }

    WrapperCPA wrapperCpa = ((WrapperCPA)cpa);

    BDDCPA bddCpa = wrapperCpa.retrieveWrappedCpa(BDDCPA.class);
    if (bddCpa == null) {
      throw new InvalidConfigurationException(BddRefiner.class.getSimpleName() + " needs a BDDCPA");
    }

    Configuration config = bddCpa.getConfiguration();
    LogManager logger = bddCpa.getLogger();
    CFA cfa = bddCpa.getCFA();
    ShutdownNotifier shutdownNotifier = bddCpa.getShutdownNotifier();

    bddCpa.injectRefinablePrecision();

    final StrongestPostOperator<ValueAnalysisState> strongestPostOperator =
        new ValueAnalysisStrongestPostOperator(logger, Configuration.builder().build(), cfa);

    final FeasibilityChecker<ValueAnalysisState> feasibilityChecker =
        new ValueAnalysisFeasibilityChecker(strongestPostOperator, logger, cfa, config);

    final ValueAnalysisPathInterpolator pathInterpolator =
        new ValueAnalysisPathInterpolator(
            feasibilityChecker,
            strongestPostOperator,
            new ValueAnalysisPrefixProvider(logger, cfa, config),
            config,
            logger,
            shutdownNotifier,
            cfa);

    BddArgBasedRefiner refiner = new BddArgBasedRefiner(feasibilityChecker, pathInterpolator);
    return AbstractARGBasedRefiner.forARGBasedRefiner(refiner, cpa);
  }
}
