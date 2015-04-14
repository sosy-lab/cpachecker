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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

public class ErrorPathClassifier {

  private static final int MAX_PREFIX_NUMBER = 1000;

  public static final String SUFFIX_REPLACEMENT = ErrorPathClassifier.class.getSimpleName()  + " replaced this edge in suffix";
  private static final String PREFIX_REPLACEMENT = ErrorPathClassifier.class.getSimpleName()  + " replaced this assume edge in prefix";

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
    REFINE_SHALLOW(FIRST_HIGHEST_SCORE),
    REFINE_DEEP(LAST_LOWEST_SCORE),

    // heuristic based on the length of the interpolant sequence (+ loop-counter heuristic)
    ITP_LENGTH_SHORT_SHALLOW(FIRST_LOWEST_SCORE),
    ITP_LENGTH_LONG_SHALLOW(FIRST_HIGHEST_SCORE),
    ITP_LENGTH_SHORT_DEEP(LAST_LOWEST_SCORE),
    ITP_LENGTH_LONG_DEEP(LAST_HIGHEST_SCORE),

    FEASIBLE();

    private PrefixPreference () {}

    private PrefixPreference (Function<Pair<Integer, Integer>, Boolean> scorer) {
      this.scorer = scorer;
    }

    private Function<Pair<Integer, Integer>, Boolean> scorer = INDIFFERENT_SCOREKEEPER;
  }

  public ErrorPathClassifier(Optional<VariableClassification> pClassification,
                             Optional<LoopStructure> pLoopStructure) {
    classification  = pClassification;
    loopStructure   = pLoopStructure;
  }

  public ARGPath obtainSlicedPrefix(PrefixPreference preference, ARGPath errorPath, List<ARGPath> pPrefixes) {

    switch (preference) {
    case SHORTEST:
      return obtainShortestPrefix(pPrefixes, errorPath);

    case LONGEST:
      return obtainLongestPrefix(pPrefixes, errorPath);

    case RANDOM:
      return obtainRandomPrefix(pPrefixes, errorPath);

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
      return obtainScoreBasedPrefix(pPrefixes, preference, errorPath);

    case FEASIBLE:
      return buildFeasiblePath(pPrefixes, errorPath);

    default:
      return errorPath;
    }
  }

  // picks the very first infeasible prefix
  private ARGPath obtainShortestPrefix(List<ARGPath> pPrefixes, ARGPath originalErrorPath) {
    return buildInfeasiblePath(0, pPrefixes, originalErrorPath);
  }

  // picks the last infeasible prefix
  private ARGPath obtainLongestPrefix(List<ARGPath> pPrefixes, ARGPath originalErrorPath) {
    return buildInfeasiblePath(pPrefixes.size() - 1, pPrefixes, originalErrorPath);
  }

  // not really a sensible heuristic at all, just here for comparison reasons
  private ARGPath obtainRandomPrefix(List<ARGPath> pPrefixes, ARGPath originalErrorPath) {
    return buildInfeasiblePath(new Random().nextInt(pPrefixes.size()), pPrefixes, originalErrorPath);
  }

  private ARGPath obtainScoreBasedPrefix(List<ARGPath> pPrefixes, PrefixPreference preference, ARGPath originalErrorPath) {
    if (!classification.isPresent()) {
      return originalErrorPath;
    }

    MutableARGPath currentErrorPath = new MutableARGPath();
    Integer bestScore = null;
    Integer bestIndex = 0;
    Integer currIndex = 0;
    for (ARGPath currentPrefix : limitNumberOfPrefixesToAnalyze(pPrefixes, preference)) {
      assert (Iterables.getLast(currentPrefix.asEdgesList()).getEdgeType() == CFAEdgeType.AssumeEdge);

      currentErrorPath.addAll(pathToList(currentPrefix));

      int score = obtainScoreForPath(currentErrorPath.immutableCopy(), preference);
      if (preference.scorer.apply(Pair.of(score, bestScore))) {
        bestScore = score;
        bestIndex = currIndex;
      }

      currIndex++;

      replaceAssumeEdgeWithBlankEdge(currentErrorPath);
    }

    return buildInfeasiblePath(bestIndex, pPrefixes, originalErrorPath);
  }

  private int obtainScoreForPath(ARGPath pPrefix, PrefixPreference pPreference) {
    switch (pPreference) {
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

    default:
      assert false;
      return -1;
    }
  }

  private int obtainDomainTypeScoreForPath(final ARGPath prefix) {
    UseDefRelation useDefRelation = new UseDefRelation(prefix, classification.get().getIntBoolVars());

    return classification.get().obtainDomainTypeScoreForVariables(useDefRelation.getUsesAsQualifiedName(), loopStructure);
  }

  private int obtainPreciseDomainTypeScoreForPath(final ARGPath prefix) {
    UseDefRelation useDefRelation = new UseDefRelation(prefix, classification.get().getIntBoolVars());

    int score = 0;
    for (Collection<ASimpleDeclaration> uses : useDefRelation.getExpandedUses(prefix).values()) {
      int temp = classification.get().obtainDomainTypeScoreForVariables2(
          FluentIterable.from(uses).transform(ASimpleDeclaration.GET_QUALIFIED_NAME).toSet(),
          loopStructure);

      // check for overflow
      if(score + temp < score) {
        score = Integer.MAX_VALUE;
        break;
      }

      score = score + temp;
    }
    return score;
  }

  private int obtainItpSequenceLengthForPath(final ARGPath prefix) {
    UseDefRelation useDefRelation = new UseDefRelation(prefix, classification.get().getIntBoolVars());

    // values are in reverse order, with the first item being the use of the failing
    // assume, hence, the index of first empty element, in relation to the first element,
    // is equal to the length of the itp-sequence
    Collection<Collection<ASimpleDeclaration>> expandedUses = useDefRelation.getExpandedUses(prefix).values();
    int length = Iterables.indexOf(expandedUses,
        new Predicate<Collection<ASimpleDeclaration>>() {
          @Override
          public boolean apply(Collection<ASimpleDeclaration> values) {
            return values.isEmpty();
          }
    });

    assert (length != -1) : "No empty 'use' found, but itp-sequence can never start from initial node";

    // mixing this heuristic with a loop-counter heuristic
    // gives slightly better results (evaluated with value analysis)
    // if(obtainDomainTypeScoreForPath(prefix) == Integer.MAX_VALUE) {
    //   length = 0;
    // }
    return length;
  }

  private int obtainPivotStateDepthForPath(final ARGPath prefix) {
    UseDefRelation useDefRelation = new UseDefRelation(prefix, classification.get().getIntBoolVars());

    PathIterator iterator = prefix.pathIterator();
    while(iterator.hasNext()) {

      if(useDefRelation.hasDef(iterator.getAbstractState(), iterator.getOutgoingEdge())) {
        return iterator.getIndex()  * (-1);
      }

      iterator.advance();
    }

    assert false : "There must be at least one non-empty definition along the path";

    return -1;
  }

  /**
   * This method limits the number of prefixes to analyze (in case it is ridiculously high)
   */
  private List<ARGPath> limitNumberOfPrefixesToAnalyze(List<ARGPath> pPrefixes, PrefixPreference preference) {

    if(pPrefixes.size() <= MAX_PREFIX_NUMBER) {
      return pPrefixes;
    }

    switch (preference) {

    case DOMAIN_BEST_SHALLOW:
    case DOMAIN_WORST_SHALLOW:
    case DOMAIN_PRECISE_BEST_SHALLOW:
    case DOMAIN_PRECISE_WORST_SHALLOW:
    case REFINE_SHALLOW:
    case ITP_LENGTH_SHORT_SHALLOW:
    case ITP_LENGTH_LONG_SHALLOW:
      return pPrefixes.subList(0, Math.min(pPrefixes.size(), MAX_PREFIX_NUMBER));

    case DOMAIN_BEST_DEEP:
    case DOMAIN_WORST_DEEP:
    case DOMAIN_PRECISE_BEST_DEEP:
    case DOMAIN_PRECISE_WORST_DEEP:
    case REFINE_DEEP:
    case ITP_LENGTH_SHORT_DEEP:
    case ITP_LENGTH_LONG_DEEP:

      // merge all prefixes up to index "extraPrefixes" into a single prefix
      int extraPrefixes = pPrefixes.size() - MAX_PREFIX_NUMBER;
      MutableARGPath firstPrefix = new MutableARGPath();
      for (ARGPath prefix : pPrefixes.subList(0, extraPrefixes)) {
        firstPrefix.addAll(pathToList(prefix));
        replaceAssumeEdgeWithBlankEdge(firstPrefix);
      }
      firstPrefix.addAll(pathToList(pPrefixes.get(extraPrefixes)));

      // merge with the remaining ones
      List<ARGPath> prefixes = new ArrayList<>();
      prefixes.add(firstPrefix.immutableCopy());
      prefixes.addAll(pPrefixes.subList(extraPrefixes + 1, pPrefixes.size()));

      return prefixes;

    default:
      assert (false) : "No need to filter for " + preference;
    }

    return pPrefixes;
  }

  /**
   * This methods builds a new path from the given prefixes. It makes all
   * contradicting assume edge but the last ineffective, so that only the last
   * assumption leads to a contradiction.
   *
   * @param bestIndex the index of the prefix with the best score
   * @param pPrefixes the list of prefixes
   * @param originalErrorPath the original error path
   * @return a new path with the last assumption leading to a contradiction
   */
  private ARGPath buildInfeasiblePath(final int bestIndex, final List<ARGPath> pPrefixes, final ARGPath originalErrorPath) {
    MutableARGPath infeasibleErrorPath = new MutableARGPath();
    for (int j = 0; j <= bestIndex; j++) {
      infeasibleErrorPath.addAll(pathToList(pPrefixes.get(j)));

      if (j != bestIndex) {
        replaceAssumeEdgeWithBlankEdge(infeasibleErrorPath);
      }
    }

    appendFeasibleSuffix(originalErrorPath, infeasibleErrorPath);

    return infeasibleErrorPath.immutableCopy();
  }

  private ARGPath buildFeasiblePath(final List<ARGPath> pPrefixes, final ARGPath originalErrorPath) {
    MutableARGPath feasibleErrorPath = new MutableARGPath();
    for (ARGPath prefix : pPrefixes) {
      feasibleErrorPath.addAll(pathToList(prefix));
      replaceAssumeEdgeWithBlankEdge(feasibleErrorPath);
    }

    appendFeasibleSuffix(originalErrorPath, feasibleErrorPath);

    return feasibleErrorPath.immutableCopy();
  }

  private void appendFeasibleSuffix(final ARGPath originalErrorPath, MutableARGPath errorPath) {
    for(Pair<ARGState, CFAEdge> element : Iterables.skip(pathToList(originalErrorPath), errorPath.size())) {
      // when encountering the original target, add it as is, ...
      if(element.getFirst().isTarget()) {
        errorPath.add(element);
      }

      // ... but replace all other transitions by no-op operations
      else {
        errorPath.add(Pair.<ARGState, CFAEdge>of(element.getFirst(), new BlankEdge("",
            FileLocation.DUMMY,
            element.getSecond().getPredecessor(),
            element.getSecond().getSuccessor(),
            SUFFIX_REPLACEMENT)));
      }
    }
  }

  private static List<Pair<ARGState, CFAEdge>> pathToList(ARGPath path) {
    return Pair.zipList(path.asStatesList(), path.asEdgesList());
  }

  /**
   * This method replaces the final (assume) edge of each prefix, except for the
   * last, with a blank edge, and as such, avoiding a contradiction along that
   * path at the removed assumptions.
   *
   * @param pErrorPath the error path from which to remove the final assume edge
   */
  private void replaceAssumeEdgeWithBlankEdge(final MutableARGPath pErrorPath) {
    Pair<ARGState, CFAEdge> assumeState = pErrorPath.removeLast();

    assert (assumeState.getSecond().getEdgeType() == CFAEdgeType.AssumeEdge);

    pErrorPath.add(Pair.<ARGState, CFAEdge>of(assumeState.getFirst(), new BlankEdge("",
        FileLocation.DUMMY,
        assumeState.getSecond().getPredecessor(),
        assumeState.getSecond().getSuccessor(),
        PREFIX_REPLACEMENT)));
  }

  public int obtainScoreForPrefixes(List<ARGPath> pPrefixes, PrefixPreference preference) {
    if (!classification.isPresent()) {
      return Integer.MAX_VALUE;
    }

    MutableARGPath currentErrorPath = new MutableARGPath();
    int bestScore = Integer.MAX_VALUE;

    for (ARGPath currentPrefix : pPrefixes) {

      currentErrorPath.addAll(pathToList(currentPrefix));

      int score = obtainDomainTypeScoreForPath(currentErrorPath.immutableCopy());

      if (preference.scorer.apply(Pair.of(score, bestScore))) {
        bestScore = score;
      }
    }

    return bestScore;
  }

  // functions for

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
