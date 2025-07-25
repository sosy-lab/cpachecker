// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.refinement;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

public class PrefixSelector {

  private final ScorerFactory factory;
  private final Optional<VariableClassification> classification;

  public InfeasiblePrefix selectSlicedPrefix(
      List<PrefixPreference> pPrefixPreference, List<InfeasiblePrefix> pInfeasiblePrefixes) {
    return Ordering.compound(createComparators(pPrefixPreference)).min(pInfeasiblePrefixes);
  }

  public int obtainScoreForPrefixes(
      final List<InfeasiblePrefix> pPrefixes, final PrefixPreference pPreference) {

    if (!classification.isPresent()) {
      return Scorer.DEFAULT_SCORE;
    }

    Scorer scorer = factory.createScorer(pPreference);
    return pPrefixes.stream()
        .mapToInt(p -> scorer.computeScore(p))
        .min()
        .orElse(Scorer.DEFAULT_SCORE);
  }

  private List<Comparator<InfeasiblePrefix>> createComparators(
      List<PrefixPreference> pPrefixPreference) {
    return Lists.transform(pPrefixPreference, p -> factory.createScorer(p).getComparator());
  }

  public static final List<PrefixPreference> NO_SELECTION =
      Collections.singletonList(PrefixPreference.NONE);

  public enum PrefixPreference {

    // heuristics based on the length of the infeasible prefix
    LENGTH_MIN,
    LENGTH_MAX,

    // heuristics based on domain-type score over variables in the interpolant sequence
    DOMAIN_MIN,
    DOMAIN_MAX,

    // heuristics based on loop-counter variables referenced in the interpolant sequence
    LOOPS_MIN,
    LOOPS_MAX,

    // heuristics based on approximating the depth of the refinement root
    PIVOT_MIN,
    PIVOT_MAX,

    // heuristic based on the width of the interpolant sequence
    WIDTH_MIN,
    WIDTH_MAX,

    // heuristic based on counting the number of assignments over variables in the interpolant
    // sequence
    ASSIGNMENTS_MIN,
    ASSIGNMENTS_MAX,

    // heuristic based on counting the number of assumptions over variables in the interpolant
    // sequence
    ASSUMPTIONS_MIN,
    ASSUMPTIONS_MAX,

    // use this only if you are feeling lucky
    RANDOM,

    // signals to not perform any selection
    NONE
  }

  private static class ScorerFactory {

    private final LogManagerWithoutDuplicates logger;

    private final Optional<VariableClassification> classification;
    private final Optional<LoopStructure> loopStructure;

    // We instantiate this only once, because it is pseudo-random (i.e., deterministic),
    // and if we instantiate it every time, we get the same sequence of random numbers.
    private final Scorer randomScorer = new RandomScorer();

    ScorerFactory(
        final Optional<VariableClassification> pClassification,
        final Optional<LoopStructure> pLoopStructure,
        final LogManager pLogger) {
      classification = pClassification;
      loopStructure = pLoopStructure;
      logger = new LogManagerWithoutDuplicates(pLogger);
    }

    Scorer createScorer(PrefixPreference pPreference) {
      return switch (pPreference) {
        case LENGTH_MIN -> new LengthScorer();
        case LENGTH_MAX -> new LengthScorer().invert();
        case DOMAIN_MIN -> new DomainScorer(classification, loopStructure, logger);
        case DOMAIN_MAX -> new DomainScorer(classification, loopStructure, logger).invert();
        case LOOPS_MIN -> new LoopScorer(classification, loopStructure, logger);
        case LOOPS_MAX -> new LoopScorer(classification, loopStructure, logger).invert();
        case WIDTH_MIN -> new WidthScorer();
        case WIDTH_MAX -> new WidthScorer().invert();
        case PIVOT_MIN -> new DepthScorer();
        case PIVOT_MAX -> new DepthScorer().invert();
        case ASSIGNMENTS_MIN -> new AssignmentScorer(classification);
        case ASSIGNMENTS_MAX -> new AssignmentScorer(classification).invert();
        case ASSUMPTIONS_MIN -> new AssumptionScorer(classification);
        case ASSUMPTIONS_MAX -> new AssumptionScorer(classification).invert();
        case RANDOM -> randomScorer;
        // illegal arguments
        case NONE ->
            throw new IllegalArgumentException(
                "Illegal prefix preference " + pPreference + " given!");
      };
    }
  }

  @FunctionalInterface
  private interface Scorer {
    int DEFAULT_SCORE = Integer.MAX_VALUE;

    int computeScore(final InfeasiblePrefix pPrefix);

    default Comparator<InfeasiblePrefix> getComparator() {
      return Comparator.comparingInt(this::computeScore);
    }

    default Scorer invert() {
      Scorer delegate = this;
      return pPrefix -> {
        int score = delegate.computeScore(pPrefix);
        if (score == Integer.MIN_VALUE) {
          return Scorer.DEFAULT_SCORE;
        }
        return -score;
      };
    }
  }

  private static class DomainScorer implements Scorer {

    private final LogManagerWithoutDuplicates logger;
    private final Optional<VariableClassification> classification;
    private final Optional<LoopStructure> loopStructure;

    DomainScorer(
        final Optional<VariableClassification> pClassification,
        final Optional<LoopStructure> pLoopStructure,
        final LogManagerWithoutDuplicates pLogger) {
      classification = pClassification;
      loopStructure = pLoopStructure;
      logger = pLogger;
    }

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      return classification
          .orElseThrow()
          .obtainDomainTypeScoreForVariables(
              pPrefix.extractSetOfIdentifiers(), loopStructure, logger);
    }
  }

  private static class LoopScorer implements Scorer {

    private final LogManagerWithoutDuplicates logger;
    private final Optional<VariableClassification> classification;
    private final Optional<LoopStructure> loopStructure;

    LoopScorer(
        final Optional<VariableClassification> pClassification,
        final Optional<LoopStructure> pLoopStructure,
        final LogManagerWithoutDuplicates pLogger) {
      classification = pClassification;
      loopStructure = pLoopStructure;
      logger = pLogger;
    }

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      int score =
          classification
              .orElseThrow()
              .obtainDomainTypeScoreForVariables(
                  pPrefix.extractSetOfIdentifiers(), loopStructure, logger);

      // TODO next line looks like a bug. The score is either MAX_INT or ZERO afterward.
      if (score != DEFAULT_SCORE) {
        score = 0;
      }

      return score;
    }
  }

  private static class WidthScorer implements Scorer {

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      return pPrefix.getNonTrivialLength();
    }
  }

  private static class DepthScorer implements Scorer {

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      return pPrefix.getDepthOfPivotState();
    }
  }

  private static class LengthScorer implements Scorer {

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      return pPrefix.getPath().size();
    }
  }

  private static class AssignmentScorer implements Scorer {

    private final Optional<VariableClassification> classification;

    AssignmentScorer(final Optional<VariableClassification> pClassification) {
      classification = pClassification;
    }

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      int count = 0;
      for (String variable : pPrefix.extractSetOfIdentifiers()) {
        count = count + classification.orElseThrow().getAssignedVariables().count(variable);
      }

      return count;
    }
  }

  private static class AssumptionScorer implements Scorer {

    private final Optional<VariableClassification> classification;

    AssumptionScorer(final Optional<VariableClassification> pClassification) {
      classification = pClassification;
    }

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      int count = 0;
      for (String variable : pPrefix.extractSetOfIdentifiers()) {
        count = count + classification.orElseThrow().getAssumedVariables().count(variable);
      }

      return count;
    }
  }

  private static class RandomScorer implements Scorer {

    private static final Random random = new Random(0);

    @Override
    public int computeScore(final InfeasiblePrefix pPrefix) {
      return random.nextInt(1000);
    }
  }

  public PrefixSelector(
      Optional<VariableClassification> pClassification,
      Optional<LoopStructure> pLoopStructure,
      LogManager pLogger) {
    factory = new ScorerFactory(pClassification, pLoopStructure, pLogger);
    classification = pClassification;
  }
}
