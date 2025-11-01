// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.io.Resources;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils.DelegatingRefinerAtomNormalizer;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils.DelegatingRefinerDslLoader;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils.DelegatingRefinerMatchingVisitor;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils.DelegatingRefinerNormalizedFormula;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils.DelegatingRefinerPatternRule;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils.DelegatingRefinerSExpression;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils.DelegatingRefinerSExpressionSExpressionOperator;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * A heuristic that detects saturation in predicate abstraction by analyzing pattern redundancy
 * across the added predicates stored in the {@link ReachedSetDelta}. For each predicate, its
 * abstraction formula is split into atomic expressions, normalized and matched against a
 * declarative DSL rules set. The heuristic tracks the frequency of matched patterns and their
 * semantic categories to identify dominant structures. The heuristic returns {@code false} if any
 * of the following stop conditions are met:
 *
 * <ul>
 *   <li>Redundancy has plateaued and only a single patterns continues to grow.
 *   <li>A single semantic category dominates the predicate set.
 *   <li>Overall pattern redundancy exceeds beyond the configured threshold.
 * </ul>
 */
@Options(prefix = "cpa.predicate.delegatingRefinerHeuristics.RedundantPredicates")
public class DelegatingRefinerHeuristicRedundantPredicates implements DelegatingRefinerHeuristic {
  private static final double EPSILON = 0.01;
  private static final String DSL_RESOURCE_NAME = "delegatingRefiner-redundancyRules.json";
  private static final int MAX_PLATEAU_STEPS = 3;

  @Option(
      secure = true,
      name = "patternSizeTrigger",
      description = "Total size of patterns to trigger a stop condition check")
  private int patternSizeTrigger = 6000;

  @Option(
      secure = true,
      name = "categoryRedundancyThreshold",
      description = "Threshold of maximum acceptable dominance of one category")
  private double categoryRedundancyThreshold = 0.8;

  @Option(secure = true, name = "dslRulePath", description = "Path to the DSL rules file")
  private Path dslRulePath = null;

  private final double redundancyThreshold;
  private double previousRedundancyPatterns = -1.0;
  private double previousDominantPatternCount = 0.0;
  private int plateauSteps = 0;
  private String lastDominantPatternKey = null;

  private final FormulaManagerView formulaManager;
  private final LogManager logger;
  private final DelegatingRefinerAtomNormalizer normalizer;
  private final DelegatingRefinerMatchingVisitor matcher;

  /**
   * Construct a redundant predicates heuristic.
   *
   * @param pConfiguration configuration used to inject the DSL rule path
   * @param pAcceptableRedundancyThreshold maximum redundancy rate acceptable
   * @param pFormulaManager formula manager used for normalization
   * @param pLogger logger for diagnostic output
   * @throws InvalidConfigurationException if the provided redundancy rate is smaller than 0.0 (=
   *     0%) or higher than 1.0 (= 100%)
   * @throws IllegalStateException if the DSL rules cannot be loaded
   */
  public DelegatingRefinerHeuristicRedundantPredicates(
      Configuration pConfiguration,
      double pAcceptableRedundancyThreshold,
      final FormulaManagerView pFormulaManager,
      final LogManager pLogger)
      throws InvalidConfigurationException {

    pConfiguration.inject(this);
    if (pAcceptableRedundancyThreshold < 0.0 || pAcceptableRedundancyThreshold > 1.0) {
      throw new InvalidConfigurationException(
          "Acceptable redundancy rate must be between 0.0 and 1.0.");
    }
    this.redundancyThreshold = pAcceptableRedundancyThreshold;
    this.formulaManager = checkNotNull(pFormulaManager);
    this.logger = pLogger;
    normalizer = new DelegatingRefinerAtomNormalizer(formulaManager);

    ImmutableList<DelegatingRefinerPatternRule> allPatternRules;
    try {
      if (dslRulePath != null && Files.exists(dslRulePath)) {
        logger.logf(Level.FINEST, "Loading redundancy rules from file: %s ", dslRulePath);
        try (Reader reader = Files.newBufferedReader(dslRulePath)) {
          allPatternRules = DelegatingRefinerDslLoader.loadDsl(reader);
        }
      } else {
        logger.logf(
            Level.FINEST,
            "Loading redundancy rules from class path resource: %s",
            DSL_RESOURCE_NAME);
        try (BufferedReader reader =
            Resources.asCharSource(
                    Resources.getResource(
                        DelegatingRefinerHeuristicRedundantPredicates.class, DSL_RESOURCE_NAME),
                    StandardCharsets.UTF_8)
                .openBufferedStream()) {
          allPatternRules = DelegatingRefinerDslLoader.loadDsl(reader);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load DSL rules for redundancy matching.", e);
    }

    this.matcher = new DelegatingRefinerMatchingVisitor(allPatternRules);
  }

  /**
   * Evaluates if the added states in the pDeltas are dominated by one dominant pattern or a
   * category. It analyzes the abstraction formulas from the newly added states, normalizes them and
   * matches them against a set of DSL rules. From these resulting patterns, redundancy metrics are
   * computed.
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

    if (checkStopConditions(patternFrequency, categoryFrequency)) {
      return false;
    }

    return true;
  }

  private void collectAndCategorizePatterns(
      ImmutableList<ReachedSetDelta> pDeltas,
      ImmutableMultiset.Builder<String> pPatternBuilder,
      ImmutableMultiset.Builder<String> pCategoryBuilder) {

    for (ReachedSetDelta delta : pDeltas) {
      for (AbstractState pState : delta.addedStates()) {
        PredicateAbstractState predState =
            checkNotNull(AbstractStates.extractStateByType(pState, PredicateAbstractState.class));

        if (predState.isAbstractionState()) {
          BooleanFormula abstractionFormula = predState.getAbstractionFormula().asFormula();
          DelegatingRefinerSExpression rootExpression = normalizer.buildAtom(abstractionFormula);
          collectMatches(rootExpression, matcher, pPatternBuilder, pCategoryBuilder);
        }
      }
    }
  }

  private void collectMatches(
      DelegatingRefinerSExpression pExpression,
      DelegatingRefinerMatchingVisitor pMatcher,
      ImmutableMultiset.Builder<String> pPatternBuilder,
      ImmutableMultiset.Builder<String> pCategoryBuilder) {
    ImmutableList<DelegatingRefinerNormalizedFormula> matches = pExpression.accept(pMatcher);
    for (DelegatingRefinerNormalizedFormula match : matches) {
      pPatternBuilder.add(match.id());
      pCategoryBuilder.add(match.category());
    }
    if (pExpression instanceof DelegatingRefinerSExpressionSExpressionOperator operator) {
      for (DelegatingRefinerSExpression subAtom : operator.sExpressionList()) {
        collectMatches(subAtom, pMatcher, pPatternBuilder, pCategoryBuilder);
      }
    }
  }

  private void logPatterns(
      ImmutableMultiset<String> pPatternFrequency, ImmutableMultiset<String> pCategoryFrequency) {
    Multiset.Entry<String> dominantCategory = getMostFrequent(pCategoryFrequency);
    if (dominantCategory != null) {
      logger.logf(Level.FINER, "Dominant category is %s.", dominantCategory);
    }

    Multiset.Entry<String> dominantPattern = getMostFrequent(pPatternFrequency);
    if (dominantPattern != null) {
      logger.logf(
          Level.FINER, "Dominant pattern is %s for %s.", dominantPattern, pPatternFrequency.size());
    }
  }

  private <T> double calculateMaxRedundancy(ImmutableMultiset<T> pMultiset) {
    int total = pMultiset.size();
    if (total == 0) {
      return 0.0;
    }
    Multiset.Entry<T> dominant = getMostFrequent(pMultiset);
    if (dominant == null) {
      return 0.0;
    }
    return (double) dominant.getCount() / total;
  }

  private <T> Multiset.Entry<T> getMostFrequent(ImmutableMultiset<T> pMultiset) {
    Multiset.Entry<T> dominant = null;
    for (Multiset.Entry<T> entry : pMultiset.entrySet()) {
      if (dominant == null || entry.getCount() > dominant.getCount()) {
        dominant = entry;
      }
    }
    return dominant;
  }

  private boolean checkStopConditions(
      ImmutableMultiset<String> pPatternFrequency, ImmutableMultiset<String> pCategoryFrequency) {
    int patternSize = pPatternFrequency.size();
    double maxRedundancyDetectedPatterns = calculateMaxRedundancy(pPatternFrequency);
    Multiset.Entry<String> currentDominantPattern = getMostFrequent(pPatternFrequency);

    if (isPlateauingAndDominantPatternGrowing(
            maxRedundancyDetectedPatterns, currentDominantPattern, patternSize)
        || isCategoryDominant(pCategoryFrequency, patternSize)
        || isPatternRedundancyAboveThreshold(pPatternFrequency)) {
      return true;
    }
    return false;
  }

  private boolean isPlateauingAndDominantPatternGrowing(
      double pMaxRedundancyDetectedPatterns,
      Multiset.Entry<String> pCurrentDominantPattern,
      int pPatternSize) {

    if (pCurrentDominantPattern == null) {
      return false;
    }

    boolean isRedundancyPlateauingPatterns =
        (previousRedundancyPatterns >= 0.0)
            && (Math.abs(pMaxRedundancyDetectedPatterns - previousRedundancyPatterns) < EPSILON);
    previousRedundancyPatterns = pMaxRedundancyDetectedPatterns;

    if (isRedundancyPlateauingPatterns
        && lastDominantPatternKey != null
        && lastDominantPatternKey.equals(pCurrentDominantPattern.getElement())) {
      plateauSteps++;
    } else {
      plateauSteps = 0;
    }

    boolean isDominantPatternGrowing =
        pCurrentDominantPattern.getCount() > previousDominantPatternCount;
    previousDominantPatternCount = pCurrentDominantPattern.getCount();
    lastDominantPatternKey = pCurrentDominantPattern.getElement();

    if (pPatternSize > patternSizeTrigger
        && isRedundancyPlateauingPatterns
        && isDominantPatternGrowing
        && plateauSteps > MAX_PLATEAU_STEPS) {
      logger.logf(
          Level.FINE,
          "Stop condition isPlateauingAndDominantPatternGrowing: Redundancy is plateauing and only"
              + " pattern %s is growing, total pattern size is at %d. Heuristic %s is no longer"
              + " applicable.",
          pCurrentDominantPattern,
          pPatternSize,
          this.getClass().getSimpleName());
      return true;
    }
    return false;
  }

  private boolean isCategoryDominant(
      ImmutableMultiset<String> pCategoryFrequency, int pPatternSize) {
    double maxRedundancyDetectedCategories = calculateMaxRedundancy(pCategoryFrequency);
    boolean isOneCategoryDominant = maxRedundancyDetectedCategories > categoryRedundancyThreshold;

    if (pPatternSize > patternSizeTrigger && isOneCategoryDominant) {
      Multiset.Entry<String> currentDominantCategory = getMostFrequent(pCategoryFrequency);
      logger.logf(
          Level.FINE,
          "Stop condition isCategoryDominant: Category %s is dominant at %.2f. Heuristic %s is no"
              + " longer applicable.",
          currentDominantCategory,
          maxRedundancyDetectedCategories,
          this.getClass().getSimpleName());
      return true;
    }
    return false;
  }

  private boolean isPatternRedundancyAboveThreshold(ImmutableMultiset<String> pPatternFrequency) {
    double maxRedundancyDetectedPatterns = calculateMaxRedundancy(pPatternFrequency);
    boolean isPatternRedundancyAboveThreshold = maxRedundancyDetectedPatterns > redundancyThreshold;
    if (isPatternRedundancyAboveThreshold) {
      logger.logf(
          Level.FINE,
          " Stop condition isPatternRedundancyAboveThreshold: Redundancy in patterns too high: %.2f"
              + " for threshold %.2f. Heuristic %s is no longer applicable.",
          maxRedundancyDetectedPatterns,
          redundancyThreshold,
          this.getClass().getSimpleName());
      return true;
    }
    return false;
  }

  /**
   * Returns the current acceptable redundancy threshold. Used for testing.
   *
   * @return the redundancy threshold used in the stop conditions
   */
  public double getRedundancyThreshold() {
    return redundancyThreshold;
  }
}
