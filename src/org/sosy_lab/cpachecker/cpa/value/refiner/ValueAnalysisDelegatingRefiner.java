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

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateBasedPrefixProvider;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisPrefixProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector.PrefixPreference;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/**
 * Refiner implementation that delegates to {@link ValueAnalysisPathInterpolator},
 * and if this fails, optionally delegates also to {@link PredicateCPARefiner}.
 */
@Options(prefix="cegar")
public class ValueAnalysisDelegatingRefiner extends AbstractARGBasedRefiner implements StatisticsProvider {

  @Option(secure=true, description="whether or not to use refinement selection to decide which domain to refine")
  private boolean useRefinementSelection = false;

  @Option(secure=true, description="whether or not to let auxiliary refiner score and refine a path that is feasible for the primary refiner,"
      + " this allows to only extract prefixes that are exclusive to the auxiliary refiner")
  private boolean useFeasiblePathForAuxRefiner = false;

  @Option(secure=true, description="if this score is exceeded by the first analysis, the auxilliary analysis will be refined")
  private int domainScoreThreshold = 1024;

  /**
   * classifier used to score sliced prefixes
   */
  private final PrefixSelector classfier;

  /**
   * refiner used for value-analysis refinement
   */
  private final ValueAnalysisRefiner valueCpaRefiner;

  /**
   * prefix provider used for value-analysis refinement
   */
  private final PrefixProvider valueCpaPrefixProvider;

  /**
   * predicate-analysis refiner used for predicate refinement
   */
  private final PredicateCPARefiner predicateCpaRefiner;

  /**
   * prefix provider used for predicate-analysis refinement
   */
  private final PrefixProvider predicateCpaPrefixProvider;

  StatCounter totalPrimaryRefinementsSelected = new StatCounter("Times selected refinement");
  StatCounter totalPrimaryRefinementsFinished = new StatCounter("Times finished refinement");
  StatCounter totalPrimaryExtraRefinementsSelected = new StatCounter("Times selected refinement (secondary was SAT)");
  StatCounter totalPrimaryExtraRefinementsFinished = new StatCounter("Times finished refinement (secondary was SAT)");

  StatCounter totalSecondaryRefinementsSelected = new StatCounter("Times selected refinement");
  StatCounter totalSecondaryRefinementsFinished = new StatCounter("Times finished refinement");
  StatCounter totalSecondaryExtraRefinementsSelected = new StatCounter("Times selected refinement (primary was SAT)");
  StatCounter totalSecondaryExtraRefinementsFinished = new StatCounter("Times finished refinement (primary was SAT)");

  public static ValueAnalysisDelegatingRefiner create(ConfigurableProgramAnalysis cpa) throws CPAException, InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(ValueAnalysisDelegatingRefiner.class.getSimpleName() + " could not find the ValueAnalysisCPA");
    }

    return initialiseDelegatingRefiner(cpa);
  }

  private static ValueAnalysisDelegatingRefiner initialiseDelegatingRefiner(ConfigurableProgramAnalysis cpa)
      throws CPAException, InvalidConfigurationException {

    ValueAnalysisCPA valueCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(ValueAnalysisCPA.class);
    if (valueCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisDelegatingRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    PredicateCPA predicateCpa = ((WrapperCPA)cpa).retrieveWrappedCpa(PredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisDelegatingRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    Configuration config      = valueCpa.getConfiguration();
    LogManager logger         = valueCpa.getLogger();
    CFA controlFlowAutomaton  = valueCpa.getCFA();

    return new ValueAnalysisDelegatingRefiner(
        config,
        logger,
        controlFlowAutomaton,
        cpa,
        ValueAnalysisRefiner.create(cpa),
        new ValueAnalysisPrefixProvider(logger, controlFlowAutomaton, config),
        PredicateRefiner.create(cpa),
        new PredicateBasedPrefixProvider(config, logger, predicateCpa.getSolver(), predicateCpa.getPathFormulaManager()));
  }

  protected ValueAnalysisDelegatingRefiner(
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA pCfa,
      final ConfigurableProgramAnalysis pCpa,
      final ValueAnalysisRefiner pValueRefiner,
      final PrefixProvider pValueCpaPrefixProvider,
      final PredicateCPARefiner pPredicateRefiner,
      final PrefixProvider pPredicateCpaPrefixProvider) throws InvalidConfigurationException {

    super(pCpa);
    pConfig.inject(this);

    classfier = new PrefixSelector(pCfa.getVarClassification(), pCfa.getLoopStructure());

    valueCpaRefiner         = pValueRefiner;
    valueCpaPrefixProvider  = pValueCpaPrefixProvider;

    predicateCpaRefiner         = pPredicateRefiner;
    predicateCpaPrefixProvider  = pPredicateCpaPrefixProvider;
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARGReachedSet reached, ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    int vaScore = 0;
    int paScore = Integer.MAX_VALUE;

    if (useRefinementSelection) {
      vaScore = obtainScoreForValueDomain(pErrorPath);

      // if score of primary analysis exceeds threshold, always refine secondary analysis
      if(vaScore > domainScoreThreshold) {
        paScore = -1;

        if(useFeasiblePathForAuxRefiner) {
          pErrorPath = ((ValueAnalysisPrefixProvider)valueCpaPrefixProvider).extractFeasilbePath(pErrorPath);
        }

        paScore = obtainScoreForPredicateDomain(pErrorPath);

        /** EXPERIMENTAL
         * instead of fixed scores, compute scores for both domains
         * and select based on these scores
         * Problem: hard to compare which scores favor the one or the other analysis,
         * also "impossible" to use two different scoring schema for the two analysis
        // hand the auxiliary analysis a path that is feasible
        // for the primary analysis, so that only new prefixes are found
        if(useFeasiblePathForAuxRefiner) {
          pErrorPath = ((ValueAnalysisPrefixProvider)valueCpaPrefixProvider).extractFeasilbePath(pErrorPath);
        }

        paScore = obtainScoreForPredicateDomain(pErrorPath);
        **/
      }
    }

    CounterexampleInfo cex;

    if (vaScore < paScore) {
      totalPrimaryRefinementsSelected.inc();

      cex = valueCpaRefiner.performRefinement(reached);
      if (cex.isSpurious()) {
        totalPrimaryRefinementsFinished.inc();
      }

      else {
        totalSecondaryExtraRefinementsSelected.inc();

        cex = predicateCpaRefiner.performRefinement(reached, pErrorPath);
        if (cex.isSpurious()) {
          totalSecondaryExtraRefinementsFinished.inc();
        }
      }
    }

    else {
      totalSecondaryRefinementsSelected.inc();

      cex = predicateCpaRefiner.performRefinement(reached, pErrorPath);
      if (cex.isSpurious()) {
        totalSecondaryRefinementsFinished.inc();
      }

      else {
        totalPrimaryExtraRefinementsSelected.inc();

        cex = valueCpaRefiner.performRefinement(reached, cex.getTargetPath());
        if (cex.isSpurious()) {
          totalPrimaryExtraRefinementsFinished.inc();
        }
      }
    }

    return cex;
  }

  private int obtainScoreForValueDomain(final ARGPath pErrorPath) throws CPAException, InterruptedException {
    List<InfeasiblePrefix> vaPrefixes = getPrefixesOfValueDomain(pErrorPath);

    // if path is feasible hand out a real bad score
    if(vaPrefixes.isEmpty()) {
      return Integer.MAX_VALUE;
    }

    return classfier.obtainScoreForPrefixes(vaPrefixes, PrefixPreference.DOMAIN_GOOD_LONG);
  }

  /** Experimental **/
  @SuppressWarnings("unused")
  private int obtainScoreForPredicateDomain(final ARGPath pErrorPath) throws CPAException, InterruptedException {
    List<InfeasiblePrefix> paPrefixes = getPrefixesOfPredicateDomain(pErrorPath);

    return classfier.obtainScoreForPrefixes(paPrefixes, PrefixPreference.DOMAIN_GOOD_LONG);
  }

  private List<InfeasiblePrefix> getPrefixesOfValueDomain(final ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    return valueCpaPrefixProvider.extractInfeasiblePrefixes(pErrorPath);
  }

  private List<InfeasiblePrefix> getPrefixesOfPredicateDomain(final ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    return predicateCpaPrefixProvider.extractInfeasiblePrefixes(pErrorPath);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public String getName() {
        return ValueAnalysisDelegatingRefiner.class.getSimpleName();
      }

      @Override
      public void printStatistics(final PrintStream pOut, final Result pResult, final ReachedSet pReached) {
        StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut).beginLevel();

        pOut.println("Primary Analysis:");
        writer.put(totalPrimaryRefinementsSelected)
          .put(totalPrimaryRefinementsFinished)
          .put(totalPrimaryExtraRefinementsSelected)
          .put(totalPrimaryExtraRefinementsFinished)
          .spacer();

        pOut.println("Secondary Analysis:");
        writer.put(totalSecondaryRefinementsSelected)
          .put(totalSecondaryRefinementsFinished)
          .put(totalSecondaryExtraRefinementsSelected)
          .put(totalSecondaryExtraRefinementsFinished);
      }
    });

    valueCpaRefiner.collectStatistics(pStatsCollection);
    predicateCpaRefiner.collectStatistics(pStatsCollection);
  }
}

