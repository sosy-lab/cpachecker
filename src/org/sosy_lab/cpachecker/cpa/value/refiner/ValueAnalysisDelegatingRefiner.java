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
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ErrorPathClassifier;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ErrorPathClassifier.PrefixPreference;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.PrefixProvider;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/**
 * Refiner implementation that delegates to {@link ValueAnalysisPathInterpolator},
 * and if this fails, optionally delegates also to {@link PredicateCPARefiner}.
 */
@Options(prefix="cegar")
public class ValueAnalysisDelegatingRefiner extends AbstractARGBasedRefiner implements StatisticsProvider {

  @Option(secure=true, description="whether or not to use refinement selection to decide which domain to refine")
  private boolean useRefinementSelection = false;

  /**
   * refiner used for value-analysis refinement
   */
  private ValueAnalysisRefiner valueCpaRefiner;

  /**
   * prefix provider used for value-analysis refinement
   */
  private PrefixProvider valueCpaPrefixProvider;

  /**
   * predicate-analysis refiner used for predicate refinement
   */
  private PredicateCPARefiner predicateCpaRefiner;

  /**
   * prefix provider used for predicate-analysis refinement
   */
  private PrefixProvider predicateCpaPrefixProvider;

  private final CFA cfa;


  StatCounter totalVaRefinements  = new StatCounter("Number of VA refinements");
  StatInt avgPrefixesVA           = new StatInt(StatKind.AVG, "Avg. number of VA-prefixes");
  StatInt avgScoreVA              = new StatInt(StatKind.AVG, "Avg. score of best VA-prefixes");

  StatCounter totalPaRefinements  = new StatCounter("Number of PA refinements");
  StatInt avgScorePA              = new StatInt(StatKind.AVG, "Avg. score of best PA-prefixes");
  StatInt avgPrefixesPA           = new StatInt(StatKind.AVG, "Avg. number of PA-prefixes");

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
      final PrefixProvider pPredicateCpaPrefixProvider) throws CPAException, InvalidConfigurationException {

    super(pCpa);
    pConfig.inject(this);

    cfa = pCfa;

    valueCpaRefiner         = pValueRefiner;
    valueCpaPrefixProvider  = pValueCpaPrefixProvider;

    predicateCpaRefiner         = pPredicateRefiner;
    predicateCpaPrefixProvider  = pPredicateCpaPrefixProvider;
  }

  @Override
  protected CounterexampleInfo performRefinement(final ARGReachedSet reached, final ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    int vaScore = 0;
    int paScore = 1;

    if (useRefinementSelection) {
      ErrorPathClassifier classfier = new ErrorPathClassifier(cfa.getVarClassification(), cfa.getLoopStructure());

      List<ARGPath> vaPrefixes = getPrefixesOfValueDomain(pErrorPath);
      vaScore = classfier.obtainScoreForPrefixes(vaPrefixes, PrefixPreference.DOMAIN_BEST_DEEP);
      this.avgPrefixesVA.setNextValue(vaPrefixes.size());
      this.avgScoreVA.setNextValue(vaScore);

      List<ARGPath> paPrefixes = getPrefixesOfPredicateDomain(pErrorPath);
      paScore = classfier.obtainScoreForPrefixes(paPrefixes, PrefixPreference.DOMAIN_BEST_DEEP);
      this.avgPrefixesPA.setNextValue(paPrefixes.size());
      this.avgScorePA.setNextValue(paScore);
    }

    CounterexampleInfo cex;

    if (vaScore <= paScore) {
      cex = valueCpaRefiner.performRefinement(reached);
      if(cex.isSpurious()) {
        totalVaRefinements.inc();
      }

      else {
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

    writer.put(totalPaRefinements);
    writer.put(avgPrefixesPA);
    writer.put(avgScorePA);
    writer.put(totalPaRefinementsExtra);
  }
}
