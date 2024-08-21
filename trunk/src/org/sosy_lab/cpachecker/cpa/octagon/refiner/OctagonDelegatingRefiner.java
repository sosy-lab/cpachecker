// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.octagon.refiner;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.octagon.OctagonCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisPathInterpolator;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStrongestPostOperator;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisPrefixProvider;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

/**
 * Refiner implementation that delegates to {@link ValueAnalysisPathInterpolator}, and if this
 * fails, optionally delegates also to {@link PredicateCPARefiner}.
 */
public abstract class OctagonDelegatingRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis cpa)
      throws InvalidConfigurationException {
    OctagonCPA octagonCPA =
        CPAs.retrieveCPAOrFail(cpa, OctagonCPA.class, OctagonDelegatingRefiner.class);
    final LogManager logger = octagonCPA.getLogger();
    final Configuration config = octagonCPA.getConfiguration();
    final CFA cfa = octagonCPA.getCFA();
    final ShutdownNotifier shutdownNotifier = octagonCPA.getShutdownNotifier();

    final StrongestPostOperator<ValueAnalysisState> valuePostOp =
        new ValueAnalysisStrongestPostOperator(logger, Configuration.defaultConfiguration(), cfa);

    final FeasibilityChecker<ValueAnalysisState> valueChecker =
        new ValueAnalysisFeasibilityChecker(valuePostOp, logger, cfa, config);

    final ValueAnalysisPathInterpolator interpolatingRefiner =
        new ValueAnalysisPathInterpolator(
            valueChecker,
            valuePostOp,
            new ValueAnalysisPrefixProvider(logger, cfa, config, shutdownNotifier),
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
