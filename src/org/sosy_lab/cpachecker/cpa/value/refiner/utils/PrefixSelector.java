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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import java.util.List;
import java.util.Random;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

public class PrefixSelector {

  public static final String SUFFIX_REPLACEMENT = PrefixSelector.class.getSimpleName()  + " replaced this edge in suffix";

  private final Optional<VariableClassification> classification;
  private final Optional<LoopStructure> loopStructure;

  public static enum PrefixPreference {
    // returns the original error path
    DEFAULT(),

    // sensible alternative options
    SHORTEST(),
    LONGEST(),

    // use this only if you are feeling lucky
    RANDOM(),

    // heuristics based on approximating cost via variable domain types
    DOMAIN_BEST_SHALLOW(FIRST_LOWEST_SCORE),
    DOMAIN_WORST_SHALLOW(FIRST_HIGHEST_SCORE),
    DOMAIN_BEST_DEEP(LAST_LOWEST_SCORE),
    DOMAIN_WORST_DEEP(LAST_HIGHEST_SCORE),

    // same as above, but more precise
    DOMAIN_PRECISE_BEST_SHALLOW(FIRST_LOWEST_SCORE),
    DOMAIN_PRECISE_WORST_SHALLOW(FIRST_HIGHEST_SCORE),
    DOMAIN_PRECISE_BEST_DEEP(LAST_LOWEST_SCORE),
    DOMAIN_PRECISE_WORST_DEEP(LAST_HIGHEST_SCORE),

    // heuristics based on approximating the depth of the refinement root
    REFINE_SHALLOW(FIRST_LOWEST_SCORE),
    REFINE_DEEP(LAST_HIGHEST_SCORE),

    // heuristic based on the length of the interpolant sequence (+ loop-counter heuristic)
    ITP_LENGTH_SHORT_SHALLOW(FIRST_LOWEST_SCORE),
    ITP_LENGTH_LONG_SHALLOW(FIRST_HIGHEST_SCORE),
    ITP_LENGTH_SHORT_DEEP(LAST_LOWEST_SCORE),
    ITP_LENGTH_LONG_DEEP(LAST_HIGHEST_SCORE),

    // heuristic based on counting the number of assignments related to the use-def-chain
    ASSIGNMENTS_FEWEST_SHALLOW(FIRST_LOWEST_SCORE),
    ASSIGNMENTS_FEWEST_DEEP(LAST_LOWEST_SCORE),
    ASSIGNMENTS_MOST_SHALLOW(FIRST_HIGHEST_SCORE),
    ASSIGNMENTS_MOST_DEEP(LAST_HIGHEST_SCORE),

    // heuristic based on counting the number of assumption related to the use-def-chain
    ASSUMPTIONS_FEWEST_SHALLOW(FIRST_LOWEST_SCORE),
    ASSUMPTIONS_FEWEST_DEEP(LAST_LOWEST_SCORE),
    ASSUMPTIONS_MOST_SHALLOW(FIRST_HIGHEST_SCORE),
    ASSUMPTIONS_MOST_DEEP(LAST_HIGHEST_SCORE);

    private PrefixPreference () {}

    private PrefixPreference (Function<Pair<Integer, Integer>, Boolean> scorer) {
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
    case SHORTEST:
      return pInfeasiblePrefixes.get(0);

    case LONGEST:
      return Iterables.getLast(pInfeasiblePrefixes);

    case RANDOM:
      return pInfeasiblePrefixes.get(new Random().nextInt(pInfeasiblePrefixes.size()));

    // scoring based on domain-types
    case DOMAIN_BEST_SHALLOW:
    case DOMAIN_BEST_DEEP:
    case DOMAIN_WORST_SHALLOW:
    case DOMAIN_WORST_DEEP:
    case DOMAIN_PRECISE_BEST_SHALLOW:
    case DOMAIN_PRECISE_BEST_DEEP:
    case DOMAIN_PRECISE_WORST_SHALLOW:
    case DOMAIN_PRECISE_WORST_DEEP:
    //
    // scoring based on length of itp-sequence
    case ITP_LENGTH_SHORT_SHALLOW:
    case ITP_LENGTH_LONG_SHALLOW:
    case ITP_LENGTH_SHORT_DEEP:
    case ITP_LENGTH_LONG_DEEP:
    //
    // scoring based on depth of pivot state
    case REFINE_SHALLOW:
    case REFINE_DEEP:
    //
    //
    case ASSIGNMENTS_FEWEST_SHALLOW:
    case ASSIGNMENTS_FEWEST_DEEP:
    case ASSIGNMENTS_MOST_SHALLOW:
    case ASSIGNMENTS_MOST_DEEP:
    case ASSUMPTIONS_FEWEST_SHALLOW:
    case ASSUMPTIONS_FEWEST_DEEP:
    case ASSUMPTIONS_MOST_SHALLOW:
    case ASSUMPTIONS_MOST_DEEP:
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
    case DOMAIN_BEST_SHALLOW:
    case DOMAIN_BEST_DEEP:
    case DOMAIN_WORST_SHALLOW:
    case DOMAIN_WORST_DEEP:
      return obtainDomainTypeScoreForPath(pPrefix);

    case DOMAIN_PRECISE_BEST_SHALLOW:
    case DOMAIN_PRECISE_BEST_DEEP:
    case DOMAIN_PRECISE_WORST_SHALLOW:
    case DOMAIN_PRECISE_WORST_DEEP:
      return obtainPreciseDomainTypeScoreForPath(pPrefix);

    case ITP_LENGTH_SHORT_SHALLOW:
    case ITP_LENGTH_LONG_SHALLOW:
    case ITP_LENGTH_SHORT_DEEP:
    case ITP_LENGTH_LONG_DEEP:
      return obtainItpSequenceLengthForPath(pPrefix);

    case REFINE_SHALLOW:
    case REFINE_DEEP:
      return obtainPivotStateDepthForPath(pPrefix);

    case ASSIGNMENTS_FEWEST_SHALLOW:
    case ASSIGNMENTS_FEWEST_DEEP:
    case ASSIGNMENTS_MOST_SHALLOW:
    case ASSIGNMENTS_MOST_DEEP:
      return obtainAssignmentCountForPath(pPrefix);

    case ASSUMPTIONS_FEWEST_SHALLOW:
    case ASSUMPTIONS_FEWEST_DEEP:
    case ASSUMPTIONS_MOST_SHALLOW:
    case ASSUMPTIONS_MOST_DEEP:
      return obtainAssumptionCountForPath(pPrefix);

    default:
      assert false;
      return -1;
    }
  }

  private int obtainDomainTypeScoreForPath(final InfeasiblePrefix pPrefix) {
    return classification.get().obtainDomainTypeScoreForVariables(extractVariablesFromItpSequence(pPrefix), loopStructure);
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

  private int obtainItpSequenceLengthForPath(final InfeasiblePrefix pPrefix) {
    return pPrefix.getNonTrivialLength();
  }

  private int obtainPivotStateDepthForPath(final InfeasiblePrefix pPrefix) {
    return pPrefix.getDepthOfPivotState();
  }

  private int obtainAssignmentCountForPath(final InfeasiblePrefix prefix) {
    int count = 0;
    for (String variable : extractVariablesFromItpSequence(prefix)) {
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

  private int obtainAssumptionCountForPath(final InfeasiblePrefix prefix) {
    int count = 0;
    for (String variable : extractVariablesFromItpSequence(prefix)) {
      count = count + classification.get().getAssumedVariables().count(variable);
    }

    return count;
  }

  private Set<String> extractVariablesFromItpSequence(InfeasiblePrefix pPrefix) {
    return pPrefix.extractSetOfVariables();
  }

  public int obtainScoreForPrefixes(List<InfeasiblePrefix> pPrefixes, PrefixPreference preference) {
    if (!classification.isPresent()) {
      return Integer.MAX_VALUE;
    }

    int bestScore = Integer.MAX_VALUE;
    for (InfeasiblePrefix currentPrefix : pPrefixes) {

      int score = this.obtainDomainTypeScoreForPath(currentPrefix);
      if (preference.scorer.apply(Pair.of(score, bestScore))) {
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
