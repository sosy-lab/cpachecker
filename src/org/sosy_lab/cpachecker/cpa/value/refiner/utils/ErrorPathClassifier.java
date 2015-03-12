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

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.UseDefBasedInterpolator.UseDefRelation;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

public class ErrorPathClassifier {

  private static final int MAX_PREFIX_LENGTH = 1000;
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

    // heuristics based on approximating cost via variable domain types
    DOMAIN_BEST_SHALLOW(FIRST_LOWEST_SCORE),
    DOMAIN_BEST_BOUNDED(BOUNDED_LOWEST_SCORE),
    DOMAIN_BEST_DEEP(LAST_LOWEST_SCORE),

    // heuristics based on approximating the depth of the refinement root
    REFINE_SHALLOW(FIRST_HIGHEST_SCORE),
    REFINE_DEEP(LAST_LOWEST_SCORE),

    // use these only if you are feeling lucky
    RANDOM(),
    MEDIAN(),
    MIDDLE(),

    // use these if you want to go for a coffee or ten
    DOMAIN_WORST_SHALLOW(FIRST_HIGHEST_SCORE),
    DOMAIN_WORST_DEEP(LAST_HIGHEST_SCORE);

    private PrefixPreference () {}

    private PrefixPreference (Function<Triple<Integer, Integer, Integer>, Boolean> scorer) {
      this.scorer = scorer;
    }

    private Function<Triple<Integer, Integer, Integer>, Boolean> scorer = INDIFFERENT_SCOREKEEPER;
  }

  private static final Function<Triple<Integer, Integer, Integer>, Boolean> INDIFFERENT_SCOREKEEPER = new Function<Triple<Integer, Integer, Integer>, Boolean>() {
    @Override
    public Boolean apply(Triple<Integer, Integer, Integer> prefixParameters) {
      return Boolean.TRUE;
    }};

  private static final Function<Triple<Integer, Integer, Integer>, Boolean> FIRST_HIGHEST_SCORE = new Function<Triple<Integer, Integer, Integer>, Boolean>() {
    @Override
    public Boolean apply(Triple<Integer, Integer, Integer> prefixParameters) {
      return prefixParameters.getSecond() == null
          || prefixParameters.getFirst() > prefixParameters.getSecond();
    }};

  private static final Function<Triple<Integer, Integer, Integer>, Boolean> LAST_HIGHEST_SCORE = new Function<Triple<Integer, Integer, Integer>, Boolean>() {
    @Override
    public Boolean apply(Triple<Integer, Integer, Integer> prefixParameters) {
      return prefixParameters.getSecond() == null
          || prefixParameters.getFirst() >= prefixParameters.getSecond();
    }};


  private static final Function<Triple<Integer, Integer, Integer>, Boolean> FIRST_LOWEST_SCORE = new Function<Triple<Integer, Integer, Integer>, Boolean>() {
    @Override
    public Boolean apply(Triple<Integer, Integer, Integer> prefixParameters) {
      return prefixParameters.getSecond() == null
          || prefixParameters.getFirst() < prefixParameters.getSecond();
    }};

  private static final Function<Triple<Integer, Integer, Integer>, Boolean> LAST_LOWEST_SCORE = new Function<Triple<Integer, Integer, Integer>, Boolean>() {
    @Override
    public Boolean apply(Triple<Integer, Integer, Integer> prefixParameters) {
      return prefixParameters.getSecond() == null
          || prefixParameters.getFirst() <= prefixParameters.getSecond();
    }};

  private static final Function<Triple<Integer, Integer, Integer>, Boolean> BOUNDED_LOWEST_SCORE = new Function<Triple<Integer, Integer, Integer>, Boolean>() {
    @Override
    public Boolean apply(Triple<Integer, Integer, Integer> prefixParameters) {
      if (prefixParameters.getSecond() == null) {
        return true;
      } else if (prefixParameters.getThird() < MAX_PREFIX_LENGTH) {
        return prefixParameters.getFirst() <= prefixParameters.getSecond();
      } else {
        return prefixParameters.getFirst() < prefixParameters.getSecond();
      }
    }};

  public ErrorPathClassifier(Optional<VariableClassification> pClassification,
                             Optional<LoopStructure> pLoopStructure) {
    classification  = pClassification;
    loopStructure   = pLoopStructure;
  }

  public ARGPath obtainSlicedPrefix(PrefixPreference preference,
      ARGPath errorPath,
      List<ARGPath> pPrefixes) {

    switch (preference) {
    case SHORTEST:
      return obtainShortestPrefix(pPrefixes, errorPath);

    case LONGEST:
      return obtainLongestPrefix(pPrefixes, errorPath);

    case DOMAIN_BEST_SHALLOW:
    case DOMAIN_BEST_BOUNDED:
    case DOMAIN_BEST_DEEP:
    case DOMAIN_WORST_SHALLOW:
    case DOMAIN_WORST_DEEP:
      return obtainDomainTypeHeuristicBasedPrefix(pPrefixes, preference, errorPath);

    case REFINE_SHALLOW:
    case REFINE_DEEP:
      return obtainRefinementRootHeuristicBasedPrefix(pPrefixes, preference, errorPath);

    case RANDOM:
      return obtainRandomPrefix(pPrefixes, errorPath);

    case MEDIAN:
      return obtainMedianPrefix(pPrefixes, errorPath);

    case MIDDLE:
      return obtainMiddlePrefix(pPrefixes, errorPath);

    default:
      return errorPath;
    }
  }

  private ARGPath obtainShortestPrefix(List<ARGPath> pPrefixes, ARGPath originalErrorPath) {
    return buildPath(0, pPrefixes, originalErrorPath);
  }

  private ARGPath obtainLongestPrefix(List<ARGPath> pPrefixes, ARGPath originalErrorPath) {
    return buildPath(pPrefixes.size() - 1, pPrefixes, originalErrorPath);
  }

  private ARGPath obtainDomainTypeHeuristicBasedPrefix(List<ARGPath> pPrefixes, PrefixPreference preference, ARGPath originalErrorPath) {
    if (!classification.isPresent()) {
      return concatPrefixes(pPrefixes);
    }

    MutableARGPath currentErrorPath = new MutableARGPath();
    Integer bestScore = null;
    Integer bestIndex = 0;

    for (ARGPath currentPrefix : limitNumberOfPrefixesToAnalyze(pPrefixes, preference)) {
      assert (Iterables.getLast(currentPrefix.asEdgesList()).getEdgeType() == CFAEdgeType.AssumeEdge);

      currentErrorPath.addAll(pathToList(currentPrefix));

      int score = obtainDomainTypeScoreForPath(currentErrorPath);

      if (preference.scorer.apply(Triple.of(score, bestScore, currentErrorPath.size()))) {
        bestScore = score;
        bestIndex = pPrefixes.indexOf(currentPrefix);
      }

      currentErrorPath.removeLast();
    }

    return buildPath(bestIndex, pPrefixes, originalErrorPath);
  }

  private ARGPath obtainRefinementRootHeuristicBasedPrefix(List<ARGPath> pPrefixes, PrefixPreference preference, ARGPath originalErrorPath) {
    if (!classification.isPresent()) {
      return concatPrefixes(pPrefixes);
    }

    MutableARGPath currentErrorPath = new MutableARGPath();
    Integer bestScore = null;
    Integer bestIndex = 0;

    for (ARGPath currentPrefix : limitNumberOfPrefixesToAnalyze(pPrefixes, preference)) {
      assert (Iterables.getLast(currentPrefix.asEdgesList()).getEdgeType() == CFAEdgeType.AssumeEdge);

      currentErrorPath.addAll(pathToList(currentPrefix));

      // gets the score for the prefix of how "local" it is
      AssumptionUseDefinitionCollector collector = new InitialAssumptionUseDefinitionCollector();
      collector.obtainUseDefInformation(currentErrorPath);
      int score = collector.getDependenciesResolvedOffset() * (-1);

      if (preference.scorer.apply(Triple.of(score, bestScore, currentErrorPath.size()))) {
        bestScore = score;
        bestIndex = pPrefixes.indexOf(currentPrefix);
      }

      currentErrorPath.removeLast();
    }

    return buildPath(bestIndex, pPrefixes, originalErrorPath);
  }

  // not really a sensible heuristic at all, just here for comparison reasons
  private ARGPath obtainRandomPrefix(List<ARGPath> pPrefixes, ARGPath originalErrorPath) {
    return buildPath(new Random().nextInt(pPrefixes.size()), pPrefixes, originalErrorPath);
  }

  // not really a sensible heuristic at all, just here for comparison reasons
  private ARGPath obtainMedianPrefix(List<ARGPath> pPrefixes, ARGPath originalErrorPath) {
    return buildPath(pPrefixes.size() / 2, pPrefixes, originalErrorPath);
  }

  // not really a sensible heuristic at all, just here for comparison reasons
  private ARGPath obtainMiddlePrefix(List<ARGPath> pPrefixes, ARGPath originalErrorPath) {
    int totalLength = 0;
    for (ARGPath p : pPrefixes) {
      totalLength += p.size();
    }

    int length = 0;
    int index = 0;
    for (ARGPath p : pPrefixes) {
      length += p.size();
      if (length > totalLength / 2) {
        break;
      }
      index++;
    }

    return buildPath(index, pPrefixes, originalErrorPath);
  }


  /**
   * This method limits the number of prefixes to analyze (in case it is ridiculously high)
   */
  private List<ARGPath> limitNumberOfPrefixesToAnalyze(List<ARGPath> pPrefixes, PrefixPreference preference) {

    switch (preference) {

    case DOMAIN_BEST_BOUNDED:
    case DOMAIN_BEST_SHALLOW:
    case DOMAIN_WORST_SHALLOW:
    case REFINE_SHALLOW:
      return pPrefixes.subList(0, Math.min(pPrefixes.size() - 1, MAX_PREFIX_NUMBER));

    case DOMAIN_BEST_DEEP:
    case DOMAIN_WORST_DEEP:
    case REFINE_DEEP:
      return pPrefixes.subList(Math.max(0, pPrefixes.size() - MAX_PREFIX_NUMBER - 1), pPrefixes.size() - 1);

    default:
      assert (false) : "No need to filter for " + preference;
    }

    return pPrefixes;
  }

  private int obtainDomainTypeScoreForPath(MutableARGPath currentErrorPath) {
    UseDefRelation useDefRelation = new UseDefRelation(currentErrorPath.immutableCopy(),
      classification.get().getIntBoolVars(),
      "NONE");

    return classification.get().obtainDomainTypeScoreForVariables(useDefRelation.getUsesAsQualifiedName(), loopStructure);
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
  private ARGPath buildPath(final int bestIndex, final List<ARGPath> pPrefixes, final ARGPath originalErrorPath) {
    MutableARGPath errorPath = new MutableARGPath();
    for (int j = 0; j <= bestIndex; j++) {
      List<Pair<ARGState, CFAEdge>> list = pathToList(pPrefixes.get(j));

      errorPath.addAll(list);

      if (j != bestIndex) {
        replaceAssumeEdgeWithBlankEdge(errorPath);
      }
    }

    // append the (feasible) suffix
    for(Pair<ARGState, CFAEdge> element : Iterables.skip(pathToList(originalErrorPath), errorPath.size())) {
      // keep the original target ...
      if(element.getFirst().isTarget()) {
        errorPath.add(element);
      }

      // ... while replacing each transition by a no-op (should make, e.g., interpolation simpler/faster)
      else {
        errorPath.add(Pair.<ARGState, CFAEdge>of(element.getFirst(), new BlankEdge("",
            FileLocation.DUMMY,
            element.getSecond().getPredecessor(),
            element.getSecond().getSuccessor(),
            SUFFIX_REPLACEMENT)));
      }
    }

    return errorPath.immutableCopy();
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

  private ARGPath concatPrefixes(List<ARGPath> pPrefixes) {
    MutableARGPath errorPath = new MutableARGPath();
    for (ARGPath currentPrefix : pPrefixes) {
      errorPath.addAll(pathToList(currentPrefix));
    }

    return errorPath.immutableCopy();
  }

  public int obtainScoreForPrefixes(List<ARGPath> pPrefixes, PrefixPreference preference) {
    if (!classification.isPresent()) {
      return Integer.MAX_VALUE;
    }

    MutableARGPath currentErrorPath = new MutableARGPath();
    int bestScore = Integer.MAX_VALUE;

    for (ARGPath currentPrefix : pPrefixes) {

      currentErrorPath.addAll(pathToList(currentPrefix));

      int score = obtainDomainTypeScoreForPath(currentErrorPath);

      if (preference.scorer.apply(Triple.of(score, bestScore, currentErrorPath.size()))) {
        bestScore = score;
      }
    }

    return bestScore;
  }
}