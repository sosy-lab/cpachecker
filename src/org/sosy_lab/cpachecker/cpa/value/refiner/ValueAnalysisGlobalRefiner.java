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

import java.util.List;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.SortingPathExtractor;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisInterpolantManager;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

@Options(prefix = "cpa.value.refinement")
public class ValueAnalysisGlobalRefiner extends ValueAnalysisRefiner {

  @Option(
      secure = true,
      description = "whether to use the top-down interpolation strategy or the bottom-up interpolation strategy")
  private boolean useTopDownInterpolationStrategy = true;

  public static ValueAnalysisGlobalRefiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    final ARGCPA argCpa = retrieveCPA(pCpa, ARGCPA.class);
    final ValueAnalysisCPA valueAnalysisCpa = retrieveCPA(pCpa, ValueAnalysisCPA.class);

    valueAnalysisCpa.injectRefinablePrecision();

    final LogManager logger = valueAnalysisCpa.getLogger();
    final Configuration config = valueAnalysisCpa.getConfiguration();
    final CFA cfa = valueAnalysisCpa.getCFA();

    final StrongestPostOperator<ValueAnalysisState> strongestPostOp =
        new ValueAnalysisStrongestPostOperator(logger, Configuration.builder().build(), cfa);

    final ValueAnalysisFeasibilityChecker checker =
        new ValueAnalysisFeasibilityChecker(strongestPostOp, logger, cfa, config);

    return new ValueAnalysisGlobalRefiner(argCpa,
        checker,
        strongestPostOp,
        new ValueAnalysisPrefixProvider(logger, cfa, config),
        new PrefixSelector(cfa.getVarClassification(),
                           cfa.getLoopStructure()),
        config,
        logger,
        valueAnalysisCpa.getShutdownNotifier(),
        cfa);
  }

  ValueAnalysisGlobalRefiner(
      final ARGCPA pArgCpa,
      final ValueAnalysisFeasibilityChecker pFeasibilityChecker,
      final StrongestPostOperator<ValueAnalysisState> pStrongestPostOperator,
      final GenericPrefixProvider<ValueAnalysisState> pPrefixProvider,
      final PrefixSelector pPrefixSelector,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa
  ) throws InvalidConfigurationException {

    super(pArgCpa,
        pFeasibilityChecker,
        pStrongestPostOperator,
        new SortingPathExtractor(pPrefixProvider,
            pPrefixSelector,
            pLogger,
            pConfig),
        pPrefixProvider,
        pConfig,
        pLogger,
        pShutdownNotifier,
        pCfa);

    pConfig.inject(this, ValueAnalysisGlobalRefiner.class);
  }

  /**
   * This method creates the interpolation tree, depending on the selected interpolation strategy.
   */
  @Override
  protected InterpolationTree<ValueAnalysisState, ValueAnalysisInterpolant> createInterpolationTree(
      final List<ARGPath> targetsPaths) {
    return new InterpolationTree<>(
        ValueAnalysisInterpolantManager.getInstance(),
        logger,
        targetsPaths,
        useTopDownInterpolationStrategy);
  }
}

