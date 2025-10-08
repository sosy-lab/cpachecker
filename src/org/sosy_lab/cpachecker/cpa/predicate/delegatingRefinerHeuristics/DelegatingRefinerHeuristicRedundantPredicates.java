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
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
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
 * A heuristic that checks for redundant patterns in the newly added predicates. In order to do so,
 * the predicates are split into singular atomic formulas, normalized and matched against a set of
 * declarative DSL rules defining the singular patterns. The heuristic tracks the frequency of each
 * pattern across all newly added predicates. If any singular pattern dominates beyond the
 * acceptable redundancy threshold, the heuristic returns false.
 */
public class DelegatingRefinerHeuristicRedundantPredicates implements DelegatingRefinerHeuristic {
  private static final double EPSILON = 0.01;
  private static final double CATEGORY_REDUNDANCY_THRESHOLD = 0.7;
  private static final Path DSL_RULE_PATH =
      Path.of(
          "src/org/sosy_lab/cpachecker/cpa/predicate/delegatingRefinerUtils/redundancyRules.json");

  private final double redundancyThreshold;
  private final FormulaManagerView formulaManager;
  private final LogManager logger;
  private final DelegatingRefinerAtomNormalizer normalizer;
  private final DelegatingRefinerMatchingVisitor matcher;

  private double previousRedundancyPatterns = -1.0;
  private double previousDominantPatternCount = 0.0;

  public DelegatingRefinerHeuristicRedundantPredicates(
      double pAcceptableRedundancyThreshold,
      final FormulaManagerView pFormulaManager,
      final LogManager pLogger)
      throws InvalidConfigurationException {
    if (pAcceptableRedundancyThreshold < 0.0 || pAcceptableRedundancyThreshold > 1.0) {
      throw new InvalidConfigurationException(
          "Acceptable redundancy rate must be between 0.0 and 1.0.");
    }
    this.redundancyThreshold = pAcceptableRedundancyThreshold;
    this.formulaManager = checkNotNull(pFormulaManager);
    this.logger = pLogger;
    normalizer = new DelegatingRefinerAtomNormalizer(formulaManager);

    try {
      ImmutableList<DelegatingRefinerPatternRule> allPatternRules =
          DelegatingRefinerDslLoader.loadDsl(DSL_RULE_PATH);
      this.matcher = new DelegatingRefinerMatchingVisitor(allPatternRules);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load DSL rules for redundancy matching.", e);
    }
  }

  @Override
  public boolean fulfilled(
      UnmodifiableReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {

    ImmutableMultiset.Builder<String> patternFrequencyBuilder = ImmutableMultiset.builder();
    ImmutableMultiset.Builder<String> categoryFrequencyBuilder = ImmutableMultiset.builder();

    collectAndCategorizePatterns(pDeltas, patternFrequencyBuilder, categoryFrequencyBuilder);

    ImmutableMultiset<String> patternFrequency = patternFrequencyBuilder.build();
    ImmutableMultiset<String> categoryFrequency = categoryFrequencyBuilder.build();

    logPatterns(patternFrequency, categoryFrequency);

    double maxRedundancyDetectedPatterns = calculateMaxRedundancy(patternFrequency);
    boolean isRedundancyPlateauingPatterns =
        (previousRedundancyPatterns >= 0.0)
            && (Math.abs(maxRedundancyDetectedPatterns - previousRedundancyPatterns) < EPSILON);
    previousRedundancyPatterns = maxRedundancyDetectedPatterns;

    Multiset.Entry<String> currentDominantPattern = getMostFrequent(patternFrequency);

    boolean isDominantPatternGrowing =
        currentDominantPattern.getCount() > previousDominantPatternCount;
    previousDominantPatternCount = currentDominantPattern.getCount();

    if (patternFrequency.size() > 1000
        && isRedundancyPlateauingPatterns
        && isDominantPatternGrowing) {
      logger.logf(
          Level.INFO,
          "Redundancy is plateauing and only pattern %s is growing ",
          currentDominantPattern);
      return false;
    }

    double maxRedundancyDetectedCategories = calculateMaxRedundancy(categoryFrequency);
    boolean isOneCategoryDominant = maxRedundancyDetectedCategories > CATEGORY_REDUNDANCY_THRESHOLD;

    if (isOneCategoryDominant) {
      Multiset.Entry<String> currentDominantCategory = getMostFrequent(categoryFrequency);
      logger.logf(
          Level.INFO,
          "Category %s is dominant at %.2f",
          currentDominantCategory,
          maxRedundancyDetectedCategories);
      return false;
    }

    boolean isPatternRedundancyAboveThreshold = maxRedundancyDetectedPatterns > redundancyThreshold;
    if (isPatternRedundancyAboveThreshold) {
      logger.logf(
          Level.INFO,
          "Redundancy in patterns too high: %.2f for threshold %.2f.",
          maxRedundancyDetectedPatterns,
          redundancyThreshold);
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
      logger.logf(Level.FINEST, "Dominant category is %s.", dominantCategory);
    }

    Multiset.Entry<String> dominantPattern = getMostFrequent(pPatternFrequency);
    if (dominantPattern != null) {
      logger.logf(
          Level.FINEST,
          "Dominant pattern is %s for %s.",
          dominantPattern,
          pPatternFrequency.size());
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

  public double getRedundancyThreshold() {
    return redundancyThreshold;
  }
}
