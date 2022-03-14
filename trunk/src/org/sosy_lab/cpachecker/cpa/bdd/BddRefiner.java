// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bdd;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisPathInterpolator;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisStrongestPostOperator;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisPrefixProvider;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.refinement.FeasibilityChecker;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

/** Refiner implementation that delegates to {@link ValueAnalysisPathInterpolator}. */
public abstract class BddRefiner implements Refiner {

  public static Refiner create(ConfigurableProgramAnalysis cpa)
      throws InvalidConfigurationException {
    BDDCPA bddCpa = CPAs.retrieveCPAOrFail(cpa, BDDCPA.class, BddRefiner.class);
    Configuration config = bddCpa.getConfiguration();
    LogManager logger = bddCpa.getLogger();
    CFA cfa = bddCpa.getCFA();
    ShutdownNotifier shutdownNotifier = bddCpa.getShutdownNotifier();

    bddCpa.injectRefinablePrecision();

    final StrongestPostOperator<ValueAnalysisState> strongestPostOperator =
        new ValueAnalysisStrongestPostOperator(logger, Configuration.defaultConfiguration(), cfa);

    final FeasibilityChecker<ValueAnalysisState> feasibilityChecker =
        new ValueAnalysisFeasibilityChecker(strongestPostOperator, logger, cfa, config);

    final ValueAnalysisPathInterpolator pathInterpolator =
        new ValueAnalysisPathInterpolator(
            feasibilityChecker,
            strongestPostOperator,
            new ValueAnalysisPrefixProvider(logger, cfa, config, shutdownNotifier),
            config,
            logger,
            shutdownNotifier,
            cfa);

    BddArgBasedRefiner refiner = new BddArgBasedRefiner(feasibilityChecker, pathInterpolator);
    return AbstractARGBasedRefiner.forARGBasedRefiner(refiner, cpa);
  }
}
