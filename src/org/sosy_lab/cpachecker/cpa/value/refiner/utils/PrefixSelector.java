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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

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

  public ARGPath selectSlicedPrefix(PrefixPreference pPrefixPreference,
      Map<ARGPath, List<Pair<ARGState, Set<String>>>> pPrefixToPrecisionMapping) {

    switch (pPrefixPreference) {
    case SHORTEST:
      return FluentIterable.from(pPrefixToPrecisionMapping.keySet()).get(0);

    case LONGEST:
      return FluentIterable.from(pPrefixToPrecisionMapping.keySet()).get(pPrefixToPrecisionMapping.size() - 1);

    case RANDOM:
      return FluentIterable.from(pPrefixToPrecisionMapping.keySet()).get(new Random().nextInt(pPrefixToPrecisionMapping.size()));

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
      return obtainScoreBasedPrefix(pPrefixPreference, pPrefixToPrecisionMapping);

    default:
      assert (false) : "invalid prefix-preference " + pPrefixPreference + " given";
      return null;
    }
  }

  private ARGPath obtainScoreBasedPrefix(PrefixPreference pPrefixPreference,
      Map<ARGPath, List<Pair<ARGState, Set<String>>>> pPrefixToPrecisionMapping) {
    if (!classification.isPresent()) {
      // TODO: log user-warning here
      return FluentIterable.from(pPrefixToPrecisionMapping.keySet()).get(0);
    }

    Integer bestScore = null;
    ARGPath bestPrefix = null;

    for (ARGPath currentPrefix : pPrefixToPrecisionMapping.keySet()) {
      int score = obtainScoreForPath(pPrefixPreference, currentPrefix, pPrefixToPrecisionMapping.get(currentPrefix));

      if (pPrefixPreference.scorer.apply(Pair.of(score, bestScore))) {
        bestScore = score;
        bestPrefix = currentPrefix;
      }
    }

    return bestPrefix;
  }

  private int obtainScoreForPath(PrefixPreference pPrefixPreference,
      ARGPath pPrefix,
      List<Pair<ARGState, Set<String>>> pItpSequence) {
    switch (pPrefixPreference) {
    case DOMAIN_BEST_SHALLOW:
    case DOMAIN_BEST_DEEP:
    case DOMAIN_WORST_SHALLOW:
    case DOMAIN_WORST_DEEP:
      return obtainDomainTypeScoreForPath(pItpSequence);

    case DOMAIN_PRECISE_BEST_SHALLOW:
    case DOMAIN_PRECISE_BEST_DEEP:
    case DOMAIN_PRECISE_WORST_SHALLOW:
    case DOMAIN_PRECISE_WORST_DEEP:
      return obtainPreciseDomainTypeScoreForPath(pItpSequence);

    case ITP_LENGTH_SHORT_SHALLOW:
    case ITP_LENGTH_LONG_SHALLOW:
    case ITP_LENGTH_SHORT_DEEP:
    case ITP_LENGTH_LONG_DEEP:
      return obtainItpSequenceLengthForPath(pItpSequence);

    case REFINE_SHALLOW:
    case REFINE_DEEP:
      return obtainPivotStateDepthForPath(pItpSequence);

    case ASSIGNMENTS_FEWEST_SHALLOW:
    case ASSIGNMENTS_FEWEST_DEEP:
    case ASSIGNMENTS_MOST_SHALLOW:
    case ASSIGNMENTS_MOST_DEEP:
      return obtainAssignmentCountForPath(pPrefix, pItpSequence);
    case ASSUMPTIONS_FEWEST_SHALLOW:
    case ASSUMPTIONS_FEWEST_DEEP:
    case ASSUMPTIONS_MOST_SHALLOW:
    case ASSUMPTIONS_MOST_DEEP:
      return obtainAssumptionCountForPath(pPrefix, pItpSequence);

    default:
      assert false;
      return -1;
    }
  }

  private int obtainDomainTypeScoreForPath(List<Pair<ARGState, Set<String>>> itpSequence) {
    return classification.get().obtainDomainTypeScoreForVariables(extractVariablesFromItpSequence(itpSequence), loopStructure);
  }

  private int obtainPreciseDomainTypeScoreForPath(List<Pair<ARGState, Set<String>>> itpSequence) {

    int score = 0;
    for (Pair<ARGState, Set<String>> itp : itpSequence) {
      int temp = classification.get().obtainDomainTypeScoreForVariables2(itp.getSecond(), loopStructure);

      // check for overflow
      if(score + temp < score) {
        score = Integer.MAX_VALUE;
        break;
      }

      score = score + temp;
    }
    return score;
  }

  private int obtainItpSequenceLengthForPath(List<Pair<ARGState, Set<String>>> itpSequence) {
    return FluentIterable.from(itpSequence).filter(new Predicate<Pair<ARGState, Set<String>>>() {
      @Override
      public boolean apply(Pair<ARGState, Set<String>> pInput) {
        return !pInput.getSecond().isEmpty();
      }}).size();
  }

  private int obtainPivotStateDepthForPath(List<Pair<ARGState, Set<String>>> itpSequence) {
    int depth = 0;

    for (Pair<ARGState, Set<String>> itp : itpSequence) {
      if(!itp.getSecond().isEmpty()) {
        return depth;
      }

      depth++;
    }

    assert false : "There must be at least one non-empty definition along the path";

    return -1;
  }

  private int obtainAssignmentCountForPath(final ARGPath prefix, List<Pair<ARGState, Set<String>>> itpSequence) {
    int count = 0;
    for (String variable : extractVariablesFromItpSequence(itpSequence)) {
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

  private int obtainAssumptionCountForPath(final ARGPath prefix, List<Pair<ARGState, Set<String>>> itpSequence) {
    int count = 0;
    for (String variable : extractVariablesFromItpSequence(itpSequence)) {
      count = count + classification.get().getAssumedVariables().count(variable);
    }

    return count;
  }

  private Set<String> extractVariablesFromItpSequence(List<Pair<ARGState, Set<String>>> itpSequence) {
    return FluentIterable.from(itpSequence).transformAndConcat(new Function<Pair<ARGState, Set<String>>, Iterable<String>>() {
      @Override
      public Iterable<String> apply(Pair<ARGState, Set<String>> itp) {
        return itp.getSecond();
      }}).toSet();
  }

  public int obtainScoreForPrefixes(List<ARGPath> pPrefixes, PrefixPreference preference) {
    if (!classification.isPresent()) {
      return Integer.MAX_VALUE;
    }

    int bestScore = Integer.MAX_VALUE;
    for (ARGPath currentPrefix : pPrefixes) {

      UseDefRelation useDefRelation = new UseDefRelation(currentPrefix, classification.get().getIntBoolVars());
      UseDefBasedInterpolator useDefInterpolator = new UseDefBasedInterpolator(
          currentPrefix,
          useDefRelation);

      Collection<ValueAnalysisInterpolant> interpolants = useDefInterpolator.obtainInterpolants().values();
      Set<String> variables = new HashSet<>();
      for (ValueAnalysisInterpolant itp : interpolants) {
        variables.addAll(FluentIterable.from(itp.getMemoryLocations()).transform(MemoryLocation.FROM_MEMORYLOCATION_TO_STRING).toSet());
      }

      int score = classification.get().obtainDomainTypeScoreForVariables(variables, loopStructure);

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
