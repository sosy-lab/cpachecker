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
import org.sosy_lab.cpachecker.util.refiner.ErrorPathClassifier;
import org.sosy_lab.cpachecker.util.refiner.ErrorPathClassifier.PrefixPreference;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.PrefixProvider;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
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

  @Option(secure=true, description="the maximum score for which always the primary refinement will be performed")
  private int scoringThreshold = 65536;

  /**
   * classifier used to score sliced prefixes
   */
  private final ErrorPathClassifier classfier;

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

  StatCounter totalVaRefinements  = new StatCounter("Number of VA refinements");
  StatInt avgPrefixesVA           = new StatInt(StatKind.AVG, "Avg. number of VA-prefixes");
  StatInt avgScoreVA              = new StatInt(StatKind.AVG, "Avg. score of best VA-prefixes");
  StatTimer timeForVAPrefixes     = new StatTimer("Time for computing VA-prefixes");

  StatCounter totalPaRefinements  = new StatCounter("Number of PA refinements");
  StatInt avgScorePA              = new StatInt(StatKind.AVG, "Avg. score of best PA-prefixes");
  StatInt avgPrefixesPA           = new StatInt(StatKind.AVG, "Avg. number of PA-prefixes");
  StatTimer timeForPAPrefixes     = new StatTimer("Time for computing PA-prefixes");

  StatCounter totalVaRefinementsExtra = new StatCounter("Number of VA refinements (PA was SAT)");
  StatCounter totalPaRefinementsExtra = new StatCounter("Number of PA refinements (VA was SAT)");

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
        new ValueAnalysisFeasibilityChecker(logger, controlFlowAutomaton, config),
        PredicateRefiner.create(cpa),
        new PredicateBasedPrefixProvider(logger, predicateCpa.getSolver(), predicateCpa.getPathFormulaManager()));
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

    classfier = new ErrorPathClassifier(pCfa.getVarClassification(), pCfa.getLoopStructure());

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

      // don't bother to extract prefixes in auxiliary analysis
      // if score of primary analysis is beneath the threshold
      if(vaScore > scoringThreshold) {

        // hand the auxiliary analysis a path that is feasible
        // for the primary analysis, so that only new prefixes are found
        if(useFeasiblePathForAuxRefiner) {
          List<ARGPath> vaPrefixes = getPrefixesOfValueDomain(pErrorPath);
          pErrorPath = classfier.obtainSlicedPrefix(PrefixPreference.FEASIBLE, pErrorPath, vaPrefixes);
        }

        paScore = obtainScoreForPredicateDomain(pErrorPath);
      }
    }

    CounterexampleInfo cex;

    if (vaScore <= paScore) {
      cex = valueCpaRefiner.performRefinement(reached);

      if (cex.isSpurious()) {
        totalVaRefinements.inc();
      }

      else {
        valueCpaRefiner.resetPreviousErrorPathId();
        cex = predicateCpaRefiner.performRefinement(reached, pErrorPath);

        if(cex.isSpurious()) {
          totalPaRefinementsExtra.inc();
        }
      }
    }

    else {
      cex = predicateCpaRefiner.performRefinement(reached, pErrorPath);

      if (cex.isSpurious()) {
        totalPaRefinements.inc();
      }

      else {
        cex = valueCpaRefiner.performRefinement(reached);

        if (cex.isSpurious()) {
          totalVaRefinementsExtra.inc();
        }
      }
    }

    return cex;
  }

  private int obtainScoreForValueDomain(final ARGPath pErrorPath) throws CPAException, InterruptedException {
    timeForVAPrefixes.start();
    List<ARGPath> vaPrefixes = getPrefixesOfValueDomain(pErrorPath);
    timeForVAPrefixes.stop();

    // if path is feasible hand out a real bad score
    if(vaPrefixes.get(0) == pErrorPath) {
      return Integer.MAX_VALUE;
    }

    this.avgPrefixesVA.setNextValue(vaPrefixes.size());

    int vaScore = classfier.obtainScoreForPrefixes(vaPrefixes, PrefixPreference.DOMAIN_BEST_DEEP);
    this.avgScoreVA.setNextValue(vaScore);
    return vaScore;
  }

  private int obtainScoreForPredicateDomain(final ARGPath pErrorPath) throws CPAException, InterruptedException {
    timeForPAPrefixes.start();
    List<ARGPath> paPrefixes = getPrefixesOfPredicateDomain(pErrorPath);
    timeForPAPrefixes.stop();
    this.avgPrefixesPA.setNextValue(paPrefixes.size());

    int paScore = classfier.obtainScoreForPrefixes(paPrefixes, PrefixPreference.DOMAIN_BEST_DEEP);
    this.avgScorePA.setNextValue(paScore);
    return paScore;
  }

  private List<ARGPath> getPrefixesOfValueDomain(final ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    return valueCpaPrefixProvider.getInfeasilbePrefixes(pErrorPath);
  }

  private List<ARGPath> getPrefixesOfPredicateDomain(final ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    return predicateCpaPrefixProvider.getInfeasilbePrefixes(pErrorPath);
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
        ValueAnalysisDelegatingRefiner.this.printStatistics(pOut, pResult, pReached);
      }
    });

    valueCpaRefiner.collectStatistics(pStatsCollection);
    predicateCpaRefiner.collectStatistics(pStatsCollection);
  }

  private void printStatistics(final PrintStream out, final Result pResult, final ReachedSet pReached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    writer.put(totalVaRefinements);
    writer.put(avgPrefixesVA);
    writer.put(avgScoreVA);
    writer.put(totalVaRefinementsExtra);
    writer.put(timeForVAPrefixes);

    writer.put(totalPaRefinements);
    writer.put(avgPrefixesPA);
    writer.put(avgScorePA);
    writer.put(totalPaRefinementsExtra);
    writer.put(timeForPAPrefixes);
  }
}
