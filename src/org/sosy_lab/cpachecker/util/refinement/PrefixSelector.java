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

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

public class PrefixSelector {

  private final Optional<VariableClassification> classification;
  private final Optional<LoopStructure> loopStructure;

  public enum PrefixPreference {
    // returns the original error path
    NONE(),

    // use this only if you are feeling lucky
    RANDOM(),

    // sensible alternative options
    LENGTH_SHORT(),
    LENGTH_LONG(),

    // heuristics based on approximating cost via variable domain types
    DOMAIN_GOOD_SHORT(FIRST_LOWEST_SCORE),
    DOMAIN_GOOD_LONG(LAST_LOWEST_SCORE),
    DOMAIN_BAD_SHORT(FIRST_HIGHEST_SCORE),
    DOMAIN_BAD_LONG(LAST_HIGHEST_SCORE),

    // same as above, but in addition, prefers narrow precisions
    DOMAIN_GOOD_WIDTH_NARROW_SHORT(FIRST_LOWEST_SCORE),

    // same as above, but more precise
    DOMAIN_PRECISE_GOOD_SHORT(FIRST_LOWEST_SCORE),
    DOMAIN_PRECISE_GOOD_LONG(LAST_LOWEST_SCORE),
    DOMAIN_PRECISE_BAD_SHORT(FIRST_HIGHEST_SCORE),
    DOMAIN_PRECISE_BAD_LONG(LAST_HIGHEST_SCORE),

    // heuristics based on approximating the depth of the refinement root
    PIVOT_SHALLOW_SHORT(FIRST_LOWEST_SCORE),
    PIVOT_SHALLOW_LONG(LAST_LOWEST_SCORE),
    PIVOT_DEEP_SHORT(FIRST_HIGHEST_SCORE),
    PIVOT_DEEP_LONG(LAST_HIGHEST_SCORE),

    // heuristic based on the length of the interpolant sequence (+ loop-counter heuristic)
    WIDTH_NARROW_SHORT(FIRST_LOWEST_SCORE),
    WIDTH_NARROW_LONG(LAST_LOWEST_SCORE),
    WIDTH_WIDE_SHORT(FIRST_HIGHEST_SCORE),
    WIDTH_WIDE_LONG(LAST_HIGHEST_SCORE),

    // same as above, but in addition, avoids loop counters based on domain-type scores
    WIDTH_NARROW_NO_LOOP_SHORT(FIRST_LOWEST_SCORE),

    // heuristic based on counting the number of assignments related to the use-def-chain
    ASSIGNMENTS_FEWEST_SHORT(FIRST_LOWEST_SCORE),
    ASSIGNMENTS_FEWEST_LONG(LAST_LOWEST_SCORE),
    ASSIGNMENTS_MOST_SHORT(FIRST_HIGHEST_SCORE),
    ASSIGNMENTS_MOST_LONG(LAST_HIGHEST_SCORE),

    // heuristic based on counting the number of assumption related to the use-def-chain
    ASSUMPTIONS_FEWEST_SHORT(FIRST_LOWEST_SCORE),
    ASSUMPTIONS_FEWEST_LONG(LAST_LOWEST_SCORE),
    ASSUMPTIONS_MOST_SHORT(FIRST_HIGHEST_SCORE),
    ASSUMPTIONS_MOST_LONG(LAST_HIGHEST_SCORE);

    PrefixPreference () {}

    PrefixPreference (Function<Pair<Integer, Integer>, Boolean> scorer) {
      this.scorer = scorer;
    }

    private Function<Pair<Integer, Integer>, Boolean> scorer = INDIFFERENT_SCOREKEEPER;
  }

  public PrefixSelector(Optional<VariableClassification> pClassification,
                             Optional<LoopStructure> pLoopStructure) {
    classification  = pClassification;
    loopStructure   = pLoopStructure;
  }

  public InfeasiblePrefix selectSlicedPrefix(PrefixPreference pPrefixPreference,
      List<InfeasiblePrefix> pInfeasiblePrefixes) {

    switch (pPrefixPreference) {
    case LENGTH_SHORT:
      return pInfeasiblePrefixes.get(0);

    case LENGTH_LONG:
      return Iterables.getLast(pInfeasiblePrefixes);

    case RANDOM:
      return pInfeasiblePrefixes.get(new Random().nextInt(pInfeasiblePrefixes.size()));

    // scoring based on domain-types
    case DOMAIN_GOOD_SHORT:
    case DOMAIN_GOOD_LONG:
    case DOMAIN_BAD_SHORT:
    case DOMAIN_BAD_LONG:
    case DOMAIN_PRECISE_GOOD_SHORT:
    case DOMAIN_PRECISE_GOOD_LONG:
    case DOMAIN_PRECISE_BAD_SHORT:
    case DOMAIN_PRECISE_BAD_LONG:
    //
    // scoring based on domain-types and width of precision
    case DOMAIN_GOOD_WIDTH_NARROW_SHORT:
    //
    // scoring based on width of precision
    case WIDTH_NARROW_SHORT:
    case WIDTH_NARROW_LONG:
    case WIDTH_WIDE_SHORT:
    case WIDTH_WIDE_LONG:
    //
    // scoring based on width of precision and presence of loop counters
    case WIDTH_NARROW_NO_LOOP_SHORT:
    //
    // scoring based on depth of pivot state
    case PIVOT_SHALLOW_SHORT:
    case PIVOT_SHALLOW_LONG:
    case PIVOT_DEEP_SHORT:
    case PIVOT_DEEP_LONG:
    //
    //
    case ASSIGNMENTS_FEWEST_SHORT:
    case ASSIGNMENTS_FEWEST_LONG:
    case ASSIGNMENTS_MOST_SHORT:
    case ASSIGNMENTS_MOST_LONG:
    case ASSUMPTIONS_FEWEST_SHORT:
    case ASSUMPTIONS_FEWEST_LONG:
    case ASSUMPTIONS_MOST_SHORT:
    case ASSUMPTIONS_MOST_LONG:
      return obtainScoreBasedPrefix(pPrefixPreference, pInfeasiblePrefixes);

    default:
      assert (false) : "invalid prefix-preference " + pPrefixPreference + " given";
      return null;
    }
  }

  private InfeasiblePrefix obtainScoreBasedPrefix(PrefixPreference pPrefixPreference,
      List<InfeasiblePrefix> pInfeasiblePrefixes) {
    if (!classification.isPresent()) {
      // TODO: log user-warning here
      return pInfeasiblePrefixes.get(0);
    }

    Integer bestScore = null;
    InfeasiblePrefix bestPrefix = null;

    for (InfeasiblePrefix currentPrefix : pInfeasiblePrefixes) {
      int score = obtainScoreForPrefix(pPrefixPreference, currentPrefix);

      if (pPrefixPreference.scorer.apply(Pair.of(score, bestScore))) {
        bestScore = score;
        bestPrefix = currentPrefix;
      }
    }

    return bestPrefix;
  }

  private int obtainScoreForPrefix(PrefixPreference pPrefixPreference, InfeasiblePrefix pPrefix) {
    switch (pPrefixPreference) {
    case DOMAIN_GOOD_SHORT:
    case DOMAIN_GOOD_LONG:
    case DOMAIN_BAD_SHORT:
    case DOMAIN_BAD_LONG:
      return obtainDomainTypeScoreForPath(pPrefix);

    case DOMAIN_GOOD_WIDTH_NARROW_SHORT:
      return obtainDomainTypeScoreAndWidthForPath(pPrefix);

    case DOMAIN_PRECISE_GOOD_SHORT:
    case DOMAIN_PRECISE_GOOD_LONG:
    case DOMAIN_PRECISE_BAD_SHORT:
    case DOMAIN_PRECISE_BAD_LONG:
      return obtainPreciseDomainTypeScoreForPath(pPrefix);

    case WIDTH_NARROW_SHORT:
    case WIDTH_NARROW_LONG:
    case WIDTH_WIDE_SHORT:
    case WIDTH_WIDE_LONG:
      return obtainWidthOfPrecisionForPath(pPrefix);

    case WIDTH_NARROW_NO_LOOP_SHORT:
      return obtainWidthOfPrecisionAndFilterLoopCounters(pPrefix);

    case PIVOT_SHALLOW_SHORT:
    case PIVOT_SHALLOW_LONG:
    case PIVOT_DEEP_SHORT:
    case PIVOT_DEEP_LONG:
      return obtainPivotStateDepthForPath(pPrefix);

    case ASSIGNMENTS_FEWEST_SHORT:
    case ASSIGNMENTS_FEWEST_LONG:
    case ASSIGNMENTS_MOST_SHORT:
    case ASSIGNMENTS_MOST_LONG:
      return obtainAssignmentCountForPath(pPrefix);

    case ASSUMPTIONS_FEWEST_SHORT:
    case ASSUMPTIONS_FEWEST_LONG:
    case ASSUMPTIONS_MOST_SHORT:
    case ASSUMPTIONS_MOST_LONG:
      return obtainAssumptionCountForPath(pPrefix);

    default:
      assert false;
      return -1;
    }
  }

  private int obtainDomainTypeScoreForPath(final InfeasiblePrefix pPrefix) {
    return classification.get().obtainDomainTypeScoreForVariables(extractVariablesFromItpSequence(pPrefix), loopStructure);
  }

  private int obtainDomainTypeScoreAndWidthForPath(final InfeasiblePrefix pPrefix) {
    int score = classification.get().obtainDomainTypeScoreForVariables(extractVariablesFromItpSequence(pPrefix), loopStructure);

    if (score * 1000 < score) {
      return Integer.MAX_VALUE;
    }
    score = score * 1000;

    int width = obtainWidthOfPrecisionForPath(pPrefix);

    // if overflow, return MAX penalty
    if((score + width) < score) {
      return Integer.MAX_VALUE;
    }

    return score + width;
  }

  private int obtainPreciseDomainTypeScoreForPath(final InfeasiblePrefix pPrefix) {
    int score = 0;
    for (Set<String> variables : pPrefix.extractListOfVariables()) {
      int temp = classification.get().obtainDomainTypeScoreForVariables2(variables, loopStructure);

      // check for overflow
      if(score + temp < score) {
        score = Integer.MAX_VALUE;
        break;
      }

      score = score + temp;
    }
    return score;
  }

  private int obtainWidthOfPrecisionForPath(final InfeasiblePrefix pPrefix) {
    return pPrefix.getNonTrivialLength();
  }

  private int obtainWidthOfPrecisionAndFilterLoopCounters(final InfeasiblePrefix pPrefix) {
    int score = obtainDomainTypeScoreForPath(pPrefix);

    if(score == Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }

    return obtainWidthOfPrecisionForPath(pPrefix);
  }

  private int obtainPivotStateDepthForPath(final InfeasiblePrefix pPrefix) {
    return pPrefix.getDepthOfPivotState();
  }

  private int obtainAssignmentCountForPath(final InfeasiblePrefix pPrefix) {
    int count = 0;
    for (String variable : extractVariablesFromItpSequence(pPrefix)) {
      count = count + classification.get().getAssignedVariables().count(variable);

      // special case for (RERS)-input variables
      // this variable has always (at least) 6 values (1, 2, 3, 4, 5, 6)
      // but it is only assigned implicitly thru assume edges
      if (variable.endsWith("::input")) {
        count = 6;
        break;
      }
    }

    return count;
  }

  private int obtainAssumptionCountForPath(final InfeasiblePrefix pPrefix) {
    int count = 0;
    for (String variable : extractVariablesFromItpSequence(pPrefix)) {
      count = count + classification.get().getAssumedVariables().count(variable);
    }

    return count;
  }

  private Set<String> extractVariablesFromItpSequence(final InfeasiblePrefix pPrefix) {
    return pPrefix.extractSetOfVariables();
  }

  public int obtainScoreForPrefixes(final List<InfeasiblePrefix> pPrefixes, final PrefixPreference pPreference) {
    if (!classification.isPresent()) {
      return Integer.MAX_VALUE;
    }

    int bestScore = Integer.MAX_VALUE;
    for (InfeasiblePrefix currentPrefix : pPrefixes) {

      int score = this.obtainDomainTypeScoreForPath(currentPrefix);
      if (pPreference.scorer.apply(Pair.of(score, bestScore))) {
        bestScore = score;
      }
    }

    return bestScore;
  }

  //*************************//
  //  functions for scoring  //
  //*************************//
  private static final Function<Pair<Integer, Integer>, Boolean> INDIFFERENT_SCOREKEEPER = new Function<Pair<Integer, Integer>, Boolean>() {
    @Override
    public Boolean apply(Pair<Integer, Integer> prefixParameters) {
      return Boolean.TRUE;
    }};

  private static final Function<Pair<Integer, Integer>, Boolean> FIRST_HIGHEST_SCORE = new Function<Pair<Integer, Integer>, Boolean>() {
    @Override
    public Boolean apply(Pair<Integer, Integer> prefixParameters) {
      return prefixParameters.getSecond() == null
          || prefixParameters.getFirst() > prefixParameters.getSecond();
    }};

  private static final Function<Pair<Integer, Integer>, Boolean> LAST_HIGHEST_SCORE = new Function<Pair<Integer, Integer>, Boolean>() {
    @Override
    public Boolean apply(Pair<Integer, Integer> prefixParameters) {
      return prefixParameters.getSecond() == null
          || prefixParameters.getFirst() >= prefixParameters.getSecond();
    }};

  private static final Function<Pair<Integer, Integer>, Boolean> FIRST_LOWEST_SCORE = new Function<Pair<Integer, Integer>, Boolean>() {
    @Override
    public Boolean apply(Pair<Integer, Integer> prefixParameters) {
      return prefixParameters.getSecond() == null
          || prefixParameters.getFirst() < prefixParameters.getSecond();
    }};

  private static final Function<Pair<Integer, Integer>, Boolean> LAST_LOWEST_SCORE = new Function<Pair<Integer, Integer>, Boolean>() {
    @Override
    public Boolean apply(Pair<Integer, Integer> prefixParameters) {
      return prefixParameters.getSecond() == null
          || prefixParameters.getFirst() <= prefixParameters.getSecond();
    }};
}
