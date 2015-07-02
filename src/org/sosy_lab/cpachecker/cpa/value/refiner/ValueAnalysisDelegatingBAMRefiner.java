/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.cpa.bam.AbstractBAMBasedRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix="cegar")
public class ValueAnalysisDelegatingBAMRefiner extends AbstractBAMBasedRefiner implements StatisticsProvider {

  /**
   * refiner used for value-analysis refinement
   */
  private final ValueAnalysisBAMRefiner valueCpaRefiner;

  /**
   * predicate-analysis refiner used for predicate refinement
   */
  private final BAMPredicateRefiner predicateCpaRefiner;

  private StatCounter totalPrimaryRefinementsInitiated = new StatCounter("Times initiated refinement");
  private StatCounter totalPrimaryRefinementsPerformed = new StatCounter("Times performed refinement");

  private StatCounter totalSecondaryRefinementsInitiated = new StatCounter("Times initiated refinement");
  private StatCounter totalSecondaryRefinementsPerformed = new StatCounter("Times performed refinement");

  public static ValueAnalysisDelegatingBAMRefiner create(ConfigurableProgramAnalysis cpa) throws CPAException, InvalidConfigurationException {
    if (!(cpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(ValueAnalysisDelegatingRefiner.class.getSimpleName() + " could not find the ValueAnalysisCPA");
    }

    return initialiseDelegatingRefiner(cpa);
  }

  private static ValueAnalysisDelegatingBAMRefiner initialiseDelegatingRefiner(ConfigurableProgramAnalysis cpa)
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

    return new ValueAnalysisDelegatingBAMRefiner(
        config,
        logger,
        controlFlowAutomaton,
        cpa,
        ValueAnalysisBAMRefiner.create(cpa),
        BAMPredicateRefiner.create(cpa));
  }

  protected ValueAnalysisDelegatingBAMRefiner(
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA pCfa,
      final ConfigurableProgramAnalysis pCpa,
      final ValueAnalysisBAMRefiner pValueRefiner,
      final BAMPredicateRefiner pPredicateRefiner) throws InvalidConfigurationException {

    super(pCpa);
    pConfig.inject(this);

    valueCpaRefiner = pValueRefiner;
    predicateCpaRefiner = pPredicateRefiner;
  }

  @Override
  protected CounterexampleInfo performRefinement0(final ARGReachedSet reached, ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    CounterexampleInfo cex;

    totalPrimaryRefinementsInitiated.inc();

    cex = valueCpaRefiner.performRefinement0(reached, pErrorPath);
    if (cex.isSpurious()) {
      totalPrimaryRefinementsPerformed.inc();
    }

    else {
      totalSecondaryRefinementsInitiated.inc();

      cex = predicateCpaRefiner.performRefinement0(reached, pErrorPath);
      if (cex.isSpurious()) {
        totalSecondaryRefinementsPerformed.inc();
      }
    }

    return cex;
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
        ValueAnalysisDelegatingBAMRefiner.this.printStatistics(pOut, pResult, pReached);
      }
    });

    //valueCpaRefiner.collectStatistics(pStatsCollection);
    predicateCpaRefiner.collectStatistics(pStatsCollection);
  }

  private void printStatistics(final PrintStream out, final Result pResult, final ReachedSet pReached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    out.println("Primary Analysis:");
    writer.beginLevel().put(totalPrimaryRefinementsInitiated);
    writer.beginLevel().put(totalPrimaryRefinementsPerformed);

    writer.spacer();
    out.println("Secondary Analysis:");
    writer.beginLevel().put(totalSecondaryRefinementsInitiated);
    writer.beginLevel().put(totalSecondaryRefinementsPerformed);
  }
}

