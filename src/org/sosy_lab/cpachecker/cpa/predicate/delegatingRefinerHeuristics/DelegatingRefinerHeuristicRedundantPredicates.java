// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
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

  private final double redundancyThreshold;
  private final FormulaManagerView formulaManager;
  private final LogManager logger;
  private final DelegatingRefinerAtomNormalizer normalizer;
  private final DelegatingRefinerDslMatcher matcher;

  private double previousRedundancy = -1.0;
  private double previousDominantCount = 0.0;

  public DelegatingRefinerHeuristicRedundantPredicates(
      double pAcceptableRedundancyThreshold,
      final FormulaManagerView pFormulaManager,
      final LogManager pLogger) {
    this.redundancyThreshold = pAcceptableRedundancyThreshold;
    this.formulaManager = checkNotNull(pFormulaManager);
    this.logger = pLogger;
    normalizer = new DelegatingRefinerAtomNormalizer(formulaManager, logger);

    try {
      ImmutableList<DelegatingRefinerPatternRule> allPatternRules =
          DelegatingRefinerDslLoader.loadDsl(
              Path.of(
                  "src/org/sosy_lab/cpachecker/cpa/predicate/delegatingRefinerHeuristics/redundancyRules.dsl"));
      this.matcher = new DelegatingRefinerDslMatcher(allPatternRules);
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

    double maxRedundancyDetected = calculateMaxRedundancy(patternFrequency);

    boolean isRedundancyPlateauing =
        (previousRedundancy >= 0.0)
            && (Math.abs(maxRedundancyDetected - previousRedundancy) < EPSILON);

    previousRedundancy = maxRedundancyDetected;

    Multiset.Entry<String> dominantPattern = getMostFrequent(patternFrequency);

    boolean isDominantPatternGrowing = dominantPattern.getCount() > previousDominantCount;

    previousDominantCount = dominantPattern.getCount();

    logger.logf(
        Level.INFO,
        "Maximal redundancy: %.2f for threshold %.2f.",
        maxRedundancyDetected,
        redundancyThreshold);

    if (patternFrequency.size() > 500 && isRedundancyPlateauing && isDominantPatternGrowing) {
      logger.logf(
          Level.INFO,
          "Redundancy is plateauing at: %.2f and only one pattern is growing at %.2f.",
          previousRedundancy,
          previousDominantCount);
      return false;
    }
    return maxRedundancyDetected <= redundancyThreshold;
  }

  private void collectAndCategorizePatterns(
      ImmutableList<ReachedSetDelta> pDeltas,
      ImmutableMultiset.Builder<String> pPatternBuilder,
      ImmutableMultiset.Builder<String> pCategoryBuilder) {

    for (ReachedSetDelta delta : pDeltas) {
      for (AbstractState pState : delta.getAddedStates()) {
        PredicateAbstractState predState =
            checkNotNull(AbstractStates.extractStateByType(pState, PredicateAbstractState.class));

        if (predState.isAbstractionState()) {
          BooleanFormula abstractionFormula = predState.getAbstractionFormula().asFormula();
          ImmutableList<DelegatingRefinerNormalizedAtom> atoms =
              normalizer.normalizeFormula(abstractionFormula);

          for (DelegatingRefinerNormalizedAtom atom : atoms) {
            processAtom(atom, pPatternBuilder, pCategoryBuilder);
          }
        }
      }
    }
  }

  private void processAtom(
      DelegatingRefinerNormalizedAtom atom,
      ImmutableCollection.Builder<String> pPatternBuilder,
      ImmutableCollection.Builder<String> pCategoryBuilder) {
    String sExpr = atom.toSExpr();
    for (String subAtom : DelegatingRefinerDslMatcher.extractAtoms(sExpr)) {
      DelegatingRefinerNormalizedFormula normalizedFormula = matcher.applyPatternRule(subAtom);
      if (normalizedFormula != null) {
        pPatternBuilder.add(normalizedFormula.id());
        pCategoryBuilder.add(normalizedFormula.category());
      } else {
        logger.logf(Level.FINEST, "No rule matched formula: %s.", atom);
      }
    }
  }

  private void logPatterns(
      ImmutableMultiset<String> pPatternFrequency, ImmutableMultiset<String> pCategoryFrequency) {
    for (Multiset.Entry<String> pattern : pPatternFrequency.entrySet()) {
      logger.logf(
          Level.FINEST,
          "Pattern %s was registered %d number of times.",
          pattern.getElement(),
          pattern.getCount());
    }

    for (Multiset.Entry<String> category : pCategoryFrequency.entrySet()) {
      logger.logf(
          Level.FINEST,
          "Pattern %s was registered %d number of times.",
          category.getElement(),
          category.getCount());
    }

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

  private double calculateMaxRedundancy(ImmutableMultiset<String> pPatternFrequency) {
    int totalPatterns = pPatternFrequency.size();
    if (totalPatterns == 0) {
      return 0.0;
    }
    Multiset.Entry<String> dominantPattern = getMostFrequent(pPatternFrequency);
    if (dominantPattern == null) {
      return 0.0;
    }
    return (double) dominantPattern.getCount() / totalPatterns;
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
