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
package org.sosy_lab.cpachecker.util.refinement;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector.PrefixPreference;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/**
 * Refiner implementation that delegates to a primary refiner
 * and if this fails, optionally delegates also to a secondary refiner.
 * Also supports Refinement Selection and can delegate to the refiner
 * which has prefixes with the better score.
 */
@Options(prefix="cegar")
public final class DelegatingARGBasedRefinerWithRefinementSelection
    implements ARGBasedRefiner, StatisticsProvider {

  @Option(secure=true, description="whether or not to use refinement selection to decide which domain to refine")
  private boolean useRefinementSelection = false;

  @Option(secure=true, description="if this score is exceeded by the first analysis, the auxilliary analysis will be refined")
  private int domainScoreThreshold = 1024;

  /**
   * classifier used to score sliced prefixes
   */
  private final PrefixSelector classfier;

  /**
   * refiner used for primary refinement
   */
  private final ARGBasedRefiner primaryRefiner;

  /**
   * prefix provider used for primary refinement
   */
  private final PrefixProvider primaryPrefixProvider;

  /**
   * predicate-analysis refiner used for secondary refinement
   */
  private final ARGBasedRefiner secondaryRefiner;

  /**
   * prefix provider used for secondary refinement
   */
  private final PrefixProvider secondaryPrefixProvider;

  private final StatCounter totalPrimaryRefinementsSelected = new StatCounter("Times selected refinement");
  private final StatCounter totalPrimaryRefinementsFinished = new StatCounter("Times finished refinement");
  private final StatCounter totalPrimaryExtraRefinementsSelected = new StatCounter("Times selected refinement (secondary was SAT)");
  private final StatCounter totalPrimaryExtraRefinementsFinished = new StatCounter("Times finished refinement (secondary was SAT)");

  private final StatCounter totalSecondaryRefinementsSelected = new StatCounter("Times selected refinement");
  private final StatCounter totalSecondaryRefinementsFinished = new StatCounter("Times finished refinement");
  private final StatCounter totalSecondaryExtraRefinementsSelected = new StatCounter("Times selected refinement (primary was SAT)");
  private final StatCounter totalSecondaryExtraRefinementsFinished = new StatCounter("Times finished refinement (primary was SAT)");

  public DelegatingARGBasedRefinerWithRefinementSelection(
      final Configuration pConfig,
      final PrefixSelector pClassifier,
      final ARGBasedRefiner pPrimaryRefiner,
      final PrefixProvider pPrimaryPrefixProvider,
      final ARGBasedRefiner pSecondaryRefiner,
      final PrefixProvider pSecondaryPrefixProvider) throws InvalidConfigurationException {
    pConfig.inject(this);

    classfier = pClassifier;

    primaryRefiner         = pPrimaryRefiner;
    primaryPrefixProvider  = pPrimaryPrefixProvider;

    secondaryRefiner         = pSecondaryRefiner;
    secondaryPrefixProvider  = pSecondaryPrefixProvider;
  }

  @Override
  public CounterexampleInfo performRefinementForPath(final ARGReachedSet reached, ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    int primaryScore = 0;
    int secondaryScore = Integer.MAX_VALUE;

    if (useRefinementSelection) {
      primaryScore = obtainScoreForPrimaryDomain(pErrorPath);

      // if score of primary analysis exceeds threshold, compute score for analysis
      if (primaryScore > domainScoreThreshold) {
        secondaryScore = obtainScoreForSecondaryDomain(pErrorPath);
      }
    }

    CounterexampleInfo cex;

    if (primaryScore < secondaryScore) {
      totalPrimaryRefinementsSelected.inc();

      cex = primaryRefiner.performRefinementForPath(reached, pErrorPath);
      if (cex.isSpurious()) {
        totalPrimaryRefinementsFinished.inc();
      }

      else {
        totalSecondaryExtraRefinementsSelected.inc();

        cex = secondaryRefiner.performRefinementForPath(reached, pErrorPath);
        if (cex.isSpurious()) {
          totalSecondaryExtraRefinementsFinished.inc();
        }
      }
    }

    else {
      totalSecondaryRefinementsSelected.inc();

      cex = secondaryRefiner.performRefinementForPath(reached, pErrorPath);
      if (cex.isSpurious()) {
        totalSecondaryRefinementsFinished.inc();
      }

      else {
        totalPrimaryExtraRefinementsSelected.inc();

        cex = primaryRefiner.performRefinementForPath(reached, cex.getTargetPath());
        if (cex.isSpurious()) {
          totalPrimaryExtraRefinementsFinished.inc();
        }
      }
    }

    return cex;
  }

  private int obtainScoreForPrimaryDomain(final ARGPath pErrorPath) throws CPAException, InterruptedException {
    List<InfeasiblePrefix> primaryPrefixes = getPrefixesOfPrimaryDomain(pErrorPath);

    // if path is feasible hand out a real bad score
    if (primaryPrefixes.isEmpty()) {
      return Integer.MAX_VALUE;
    }

    return classfier.obtainScoreForPrefixes(primaryPrefixes, PrefixPreference.DOMAIN_MIN);
  }

  private int obtainScoreForSecondaryDomain(final ARGPath pErrorPath) throws CPAException, InterruptedException {
    List<InfeasiblePrefix> secondaryPrefixes = getPrefixesOfSecondaryDomain(pErrorPath);

    return classfier.obtainScoreForPrefixes(secondaryPrefixes, PrefixPreference.DOMAIN_MIN);
  }

  private List<InfeasiblePrefix> getPrefixesOfPrimaryDomain(final ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    return primaryPrefixProvider.extractInfeasiblePrefixes(pErrorPath);
  }

  private List<InfeasiblePrefix> getPrefixesOfSecondaryDomain(final ARGPath pErrorPath)
      throws CPAException, InterruptedException {

    return secondaryPrefixProvider.extractInfeasiblePrefixes(pErrorPath);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public String getName() {
        return DelegatingARGBasedRefinerWithRefinementSelection.class.getSimpleName();
      }

      @Override
      public void printStatistics(final PrintStream pOut, final Result pResult, final UnmodifiableReachedSet pReached) {
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

    if (primaryRefiner instanceof StatisticsProvider) {
      ((StatisticsProvider)primaryRefiner).collectStatistics(pStatsCollection);
    }
    if (secondaryRefiner instanceof StatisticsProvider) {
      ((StatisticsProvider)secondaryRefiner).collectStatistics(pStatsCollection);
    }
  }
}

