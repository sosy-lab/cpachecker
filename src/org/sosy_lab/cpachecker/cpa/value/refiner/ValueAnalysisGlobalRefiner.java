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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import static com.google.common.collect.FluentIterable.from;

import java.util.Collection;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.refiner.StrongestPostOperator;

import com.google.common.collect.FluentIterable;

@Options(prefix = "cpa.value.refinement")
public class ValueAnalysisGlobalRefiner extends ValueAnalysisRefiner {

  @Option(
      secure = true,
      description = "whether to use the top-down interpolation strategy or the bottom-up interpolation strategy")
  private boolean useTopDownInterpolationStrategy = true;

  public static ValueAnalysisGlobalRefiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    final ValueAnalysisCPA valueAnalysisCpa = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisGlobalRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    valueAnalysisCpa.injectRefinablePrecision();

    final LogManager logger = valueAnalysisCpa.getLogger();
    final CFA cfa = valueAnalysisCpa.getCFA();
    final Configuration config = valueAnalysisCpa.getConfiguration();

    final StrongestPostOperator strongestPostOperator = new ValueAnalysisTransferRelation(
        Configuration.builder().build(), logger, cfa);

    final ValueAnalysisFeasibilityChecker feasibilityChecker =
        new ValueAnalysisFeasibilityChecker(strongestPostOperator, logger, cfa, config);

    ValueAnalysisGlobalRefiner refiner = new ValueAnalysisGlobalRefiner(
        feasibilityChecker,
        strongestPostOperator,
        config,
        logger,
        valueAnalysisCpa.getShutdownNotifier(),
        cfa);

    return refiner;
  }

  ValueAnalysisGlobalRefiner(
      final ValueAnalysisFeasibilityChecker pFeasibilityChecker,
      final StrongestPostOperator pStrongestPostOperator,
      final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa)
      throws InvalidConfigurationException {

    super(pFeasibilityChecker, pStrongestPostOperator, pConfig, pLogger, pShutdownNotifier, pCfa);

    pConfig.inject(this, ValueAnalysisGlobalRefiner.class);
  }

  /**
   * This method creates the interpolation tree, depending on the selected interpolation strategy.
   */
  @Override
  protected ValueAnalysisInterpolationTree createInterpolationTree(Collection<ARGState> targets) {
    return new ValueAnalysisInterpolationTree(logger, targets, useTopDownInterpolationStrategy);
  }

  /**
   * This method extracts all target states available in the ARG (hence, global refinement).
   */
  @Override
  protected FluentIterable<ARGState> extractTargetStatesFromArg(final ARGReachedSet pReached) {
    return from(pReached.asReachedSet())
        .transform(AbstractStates.toState(ARGState.class))
        .filter(AbstractStates.IS_TARGET_STATE);
  }
}

