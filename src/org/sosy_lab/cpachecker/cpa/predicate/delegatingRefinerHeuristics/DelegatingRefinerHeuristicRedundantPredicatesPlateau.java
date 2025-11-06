// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

/**
 * A heuristic that detects saturation in predicate abstraction by analyzing pattern redundancy
 * across the added predicates stored in the {@link ReachedSetDelta}. Inherits from {@link
 * DelegatingRefinerHeuristicRedundantPredicates}. For each predicate, its abstraction formula is
 * split into atomic expressions, normalized and matched against a declarative DSL rules set. The
 * heuristic tracks the frequency of matched patterns and their semantic categories and returns
 * {@code false} if redundancy has plateaued and only a single patterns continues to grow.
 */
@Options(
    prefix =
        "cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristicRedundantPredicatesPlateau")
public class DelegatingRefinerHeuristicRedundantPredicatesPlateau
    extends DelegatingRefinerHeuristicRedundantPredicates {
  private static final double EPSILON = 0.03;

  @Option(
      secure = true,
      name = "maxPlateauSteps",
      description = "Number of steps a redundancy threshold is allowed to plateau")
  private int maxPlateauSteps = 3;

  private double previousRedundancyPatterns = -1.0;
  private String lastDominantPatternKey = null;
  private int plateauSteps = 0;
  private int previousDominantPatternCount = 0;

  /**
   * Construct a redundant predicates heuristic that checks if redundancy has plateaued and only a
   * single patterns continues to grow.
   *
   * @param pConfiguration configuration used to inject the DSL rule path
   * @param pFormulaManager formula manager used for normalization
   * @param pLogger logger for diagnostic output
   * @throws InvalidConfigurationException if the provided maxPlateauSteps is negative
   */
  public DelegatingRefinerHeuristicRedundantPredicatesPlateau(
      Configuration pConfiguration, FormulaManagerView pFormulaManager, LogManager pLogger)
      throws InvalidConfigurationException {

    super(pConfiguration, pFormulaManager, pLogger);
    pConfiguration.inject(this, DelegatingRefinerHeuristicRedundantPredicatesPlateau.class);

    if (maxPlateauSteps < 0) {
      throw new InvalidConfigurationException(
          "Maximal number of plateau steps must not be negative.");
    }
  }

  /**
   * Evaluates if in the added states redundancy has plateaued and only a single patterns continues
   * to grow. It analyzes the abstraction formulas from the newly added states, normalizes them and
   * matches them against a set of DSL rules. From these resulting patterns, redundancy is
   * evaluated.
   *
   * @param pReached the current ReachedSet (not used directly)
   * @param pDeltas the list of changes in the ReachedSet, including newly added states
   * @return {@code false}, if any stop condition is met, {@code true} otherwise
   */
  @Override
  public boolean fulfilled(ReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {

    ImmutableMultiset.Builder<String> patternFrequencyBuilder = ImmutableMultiset.builder();
    ImmutableMultiset.Builder<String> categoryFrequencyBuilder = ImmutableMultiset.builder();

    collectAndCategorizePatterns(pDeltas, patternFrequencyBuilder, categoryFrequencyBuilder);

    ImmutableMultiset<String> patternFrequency = patternFrequencyBuilder.build();
    ImmutableMultiset<String> categoryFrequency = categoryFrequencyBuilder.build();

    logPatterns(patternFrequency, categoryFrequency);

    if (isPlateauingAndDominantPatternGrowing(patternFrequency)) {
      return true;
    }

    return false;
  }

  private boolean isPlateauingAndDominantPatternGrowing(
      ImmutableMultiset<String> pPatternFrequency) {
    int total = pPatternFrequency.size();
    if (total == 0) {
      return false;
    }

    Multiset.Entry<String> currentDominant = getMostFrequent(pPatternFrequency);
    if (currentDominant == null) {
      return false;
    }

    double currentRedundancy = (double) currentDominant.getCount() / total;
    boolean isRedundancyPlateauing =
        (previousRedundancyPatterns >= 0.0)
            && (Math.abs(currentRedundancy - previousRedundancyPatterns) < EPSILON);

    previousRedundancyPatterns = currentRedundancy;

    if (isRedundancyPlateauing
        && lastDominantPatternKey != null
        && lastDominantPatternKey.equals(currentDominant.getElement())) {
      plateauSteps++;
    } else {
      plateauSteps = 0;
    }

    boolean isDominantPatternGrowing = currentDominant.getCount() > previousDominantPatternCount;
    previousDominantPatternCount = currentDominant.getCount();
    lastDominantPatternKey = currentDominant.getElement();

    if (isRedundancyPlateauing && isDominantPatternGrowing && plateauSteps >= maxPlateauSteps) {
      logger.logf(
          Level.FINE,
          "Stop condition Plateau: Redundancy plateauing and pattern %s is dominant (%.2f).",
          currentDominant.getElement(),
          currentRedundancy);
      return true;
    }
    return false;
  }
}
