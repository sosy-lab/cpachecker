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
package org.sosy_lab.cpachecker.cpa.octagon.refiner;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.octagon.OctagonCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisPathInterpolator;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStrongestPostOperator;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

/**
 * Refiner implementation that delegates to {@link ValueAnalysisPathInterpolator},
 * and if this fails, optionally delegates also to {@link PredicateCPARefiner}.
 */
public abstract class OctagonDelegatingRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis cpa) throws InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(OctagonDelegatingRefiner.class.getSimpleName() + " could not find the OctagonCPA");
    }

    OctagonCPA octagonCPA = ((WrapperCPA)cpa).retrieveWrappedCpa(OctagonCPA.class);
    if (octagonCPA == null) {
      throw new InvalidConfigurationException(OctagonDelegatingRefiner.class.getSimpleName() + " needs an OctagonCPA");
    }

    final LogManager logger = octagonCPA.getLogger();
    final Configuration config = octagonCPA.getConfiguration();
    final CFA cfa = octagonCPA.getCFA();
    final ShutdownNotifier shutdownNotifier = octagonCPA.getShutdownNotifier();

    final StrongestPostOperator<ValueAnalysisState> valuePostOp =
        new ValueAnalysisStrongestPostOperator(logger, Configuration.builder().build(), cfa);

    final FeasibilityChecker<ValueAnalysisState> valueChecker =
        new ValueAnalysisFeasibilityChecker(valuePostOp, logger, cfa, config);

    final ValueAnalysisPathInterpolator interpolatingRefiner =
        new ValueAnalysisPathInterpolator(
            valueChecker,
            valuePostOp,
            new ValueAnalysisPrefixProvider(logger, cfa, config),
            config,
            logger,
            shutdownNotifier,
            cfa);

    final OctagonArgBasedDelegatingRefiner refiner =
        new OctagonArgBasedDelegatingRefiner(
            config,
            logger,
            shutdownNotifier,
            cfa,
            octagonCPA.getManager(),
            octagonCPA.getTransferRelation(),
            valueChecker,
            interpolatingRefiner);

    return AbstractARGBasedRefiner.forARGBasedRefiner(refiner, cpa);
  }
}
