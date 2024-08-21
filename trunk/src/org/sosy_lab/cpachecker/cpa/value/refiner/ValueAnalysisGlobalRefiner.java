// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.SortingPathExtractor;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisInterpolantManager;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisPrefixProvider;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;

@Options(prefix = "cpa.value.refinement")
public class ValueAnalysisGlobalRefiner extends ValueAnalysisRefiner {

  @Option(
      secure = true,
      description =
          "whether to use the top-down interpolation strategy or the bottom-up interpolation"
              + " strategy")
  private boolean useTopDownInterpolationStrategy = true;

  public static Refiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    return AbstractARGBasedRefiner.forARGBasedRefiner(create0(pCpa), pCpa);
  }

  public static ARGBasedRefiner create0(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    final ValueAnalysisCPA valueAnalysisCpa =
        CPAs.retrieveCPAOrFail(pCpa, ValueAnalysisCPA.class, ValueAnalysisGlobalRefiner.class);

    valueAnalysisCpa.injectRefinablePrecision();

    final LogManager logger = valueAnalysisCpa.getLogger();
    final Configuration config = valueAnalysisCpa.getConfiguration();
    final CFA cfa = valueAnalysisCpa.getCFA();

    final StrongestPostOperator<ValueAnalysisState> strongestPostOp =
        new ValueAnalysisStrongestPostOperator(logger, Configuration.defaultConfiguration(), cfa);

    final ValueAnalysisFeasibilityChecker checker =
        new ValueAnalysisFeasibilityChecker(strongestPostOp, logger, cfa, config);

    return new ValueAnalysisGlobalRefiner(
        checker,
        strongestPostOp,
        new ValueAnalysisPrefixProvider(
            logger, cfa, config, valueAnalysisCpa.getShutdownNotifier()),
        new PrefixSelector(cfa.getVarClassification(), cfa.getLoopStructure(), logger),
        config,
        logger,
        valueAnalysisCpa.getShutdownNotifier(),
        cfa);
  }

  ValueAnalysisGlobalRefiner(
      final ValueAnalysisFeasibilityChecker pFeasibilityChecker,
      final StrongestPostOperator<ValueAnalysisState> pStrongestPostOperator,
      final GenericPrefixProvider<ValueAnalysisState> pPrefixProvider,
      final PrefixSelector pPrefixSelector,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {

    super(
        pFeasibilityChecker,
        pStrongestPostOperator,
        new SortingPathExtractor(pPrefixProvider, pPrefixSelector, pLogger, pConfig),
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
