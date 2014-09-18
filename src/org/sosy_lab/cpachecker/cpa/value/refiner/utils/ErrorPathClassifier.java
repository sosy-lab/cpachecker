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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Triple;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

public class ErrorPathClassifier {

  private static final int BOOLEAN_VAR   = 2;
  private static final int INTEQUAL_VAR  = 4;
  private static final int UNKNOWN_VAR   = 16;

  private static final int MAX_PREFIX_LENGTH = 1000;

  private static int invocationCounter = 0;

  private final Optional<VariableClassification> classification;
  private final Optional<LoopStructure> loopStructure;

  public static enum ErrorPathPrefixPreference {
    // returns the original error path
    DEFAULT(),

    // sensible alternative options
    SHORTEST(),
    LONGEST(),

    // heuristics based on approximating cost via variable domain types
    DOMAIN_BEST_SHALLOW(FIRST_LOWEST_SCORE),
    DOMAIN_BEST_BOUNDED(FINAL_LOWEST_SCORE_BOUNDED),
    DOMAIN_BEST_DEEP(FINAL_LOWEST_SCORE),

    // heuristics based on approximating the depth of the refinement root
    REFINE_SHALLOW(FIRST_HIGHEST_SCORE),
    REFINE_DEEP(FINAL_LOWEST_SCORE),

    // use these only if you are feeling lucky
    RANDOM(),
    MEDIAN(),
    MIDDLE(),

    // use these if you want to go for a coffee or ten
    DOMAIN_WORST_SHALLOW(FIRST_HIGHEST_SCORE),
    DOMAIN_WORST_DEEP(FINAL_HIGHEST_SCORE);

    private ErrorPathPrefixPreference () {}

    private ErrorPathPrefixPreference (Function<Triple<Long, Long, Integer>, Boolean> scorer) {
      this.scorer = scorer;
    }

    private Function<Triple<Long, Long, Integer>, Boolean> scorer = INDIFFERENT_SCOREKEEPER;
  }

  private static final Function<Triple<Long, Long, Integer>, Boolean> INDIFFERENT_SCOREKEEPER = new Function<Triple<Long, Long, Integer>, Boolean>() {
    @Override
    public Boolean apply(Triple<Long, Long, Integer> prefixParameters) {
      return Boolean.TRUE;
    }};

  private static final Function<Triple<Long, Long, Integer>, Boolean> FIRST_HIGHEST_SCORE = new Function<Triple<Long, Long, Integer>, Boolean>() {
    @Override
    public Boolean apply(Triple<Long, Long, Integer> prefixParameters) {
      return prefixParameters.getSecond() == null
          || prefixParameters.getFirst() > prefixParameters.getSecond();
    }};

  private static final Function<Triple<Long, Long, Integer>, Boolean> FINAL_HIGHEST_SCORE = new Function<Triple<Long, Long, Integer>, Boolean>() {
    @Override
    public Boolean apply(Triple<Long, Long, Integer> prefixParameters) {
      return prefixParameters.getSecond() == null
          || prefixParameters.getFirst() >= prefixParameters.getSecond();
    }};


  private static final Function<Triple<Long, Long, Integer>, Boolean> FIRST_LOWEST_SCORE = new Function<Triple<Long, Long, Integer>, Boolean>() {
    @Override
    public Boolean apply(Triple<Long, Long, Integer> prefixParameters) {
      return prefixParameters.getSecond() == null
          || prefixParameters.getFirst() < prefixParameters.getSecond();
    }};

  private static final Function<Triple<Long, Long, Integer>, Boolean> FINAL_LOWEST_SCORE = new Function<Triple<Long, Long, Integer>, Boolean>() {
    @Override
    public Boolean apply(Triple<Long, Long, Integer> prefixParameters) {
      return prefixParameters.getSecond() == null
          || prefixParameters.getFirst() <= prefixParameters.getSecond();
    }};

  private static final Function<Triple<Long, Long, Integer>, Boolean> FINAL_LOWEST_SCORE_BOUNDED = new Function<Triple<Long, Long, Integer>, Boolean>() {
    @Override
    public Boolean apply(Triple<Long, Long, Integer> prefixParameters) {
      if (prefixParameters.getSecond() == null) {
        return true;
      } else if (prefixParameters.getThird() < MAX_PREFIX_LENGTH) {
        return prefixParameters.getFirst() <= prefixParameters.getSecond();
      } else {
        return prefixParameters.getFirst() < prefixParameters.getSecond();
      }
    }};

  public ErrorPathClassifier(Optional<VariableClassification> pClassification,
      Optional<LoopStructure> pLoopStructure) throws InvalidConfigurationException {
    classification  = pClassification;
    loopStructure   = pLoopStructure;
  }

  public MutableARGPath obtainPrefix(ErrorPathPrefixPreference preference,
      MutableARGPath errorPath,
      List<MutableARGPath> pPrefixes) {

    switch (preference) {
    case SHORTEST:
      return obtainShortestPrefix(pPrefixes);

    case LONGEST:
      return obtainLongestPrefix(pPrefixes);

    case DOMAIN_BEST_SHALLOW:
    case DOMAIN_WORST_SHALLOW:
      return obtainDomainTypeHeuristicBasedPrefix(pPrefixes, preference);

    case REFINE_SHALLOW:
    case REFINE_DEEP:
      return obtainRefinementRootHeuristicBasedPrefix(pPrefixes, preference);

    case RANDOM:
      return obtainRandomPrefix(pPrefixes);

    case MEDIAN:
      return obtainMedianPrefix(pPrefixes);

    case MIDDLE:
      return obtainMiddlePrefix(pPrefixes);

    default:
      return errorPath;
    }
  }

  private MutableARGPath obtainShortestPrefix(List<MutableARGPath> pPrefixes) {
    return buildPath(0, pPrefixes);
  }

  private MutableARGPath obtainLongestPrefix(List<MutableARGPath> pPrefixes) {
    return buildPath(pPrefixes.size() - 1, pPrefixes);
  }

  private MutableARGPath obtainDomainTypeHeuristicBasedPrefix(List<MutableARGPath> pPrefixes, ErrorPathPrefixPreference preference) {
    if (!classification.isPresent()) {
      return concatPrefixes(pPrefixes);
    }

    MutableARGPath currentErrorPath = new MutableARGPath();
    Long bestScore                  = null;
    int bestIndex                   = 0;

    for (MutableARGPath currentPrefix : pPrefixes) {
      assert (currentPrefix.getLast().getSecond().getEdgeType() == CFAEdgeType.AssumeEdge);

      currentErrorPath.addAll(currentPrefix);

      Set<String> useDefinitionInformation = obtainUseDefInformationOfErrorPath(currentErrorPath);

      Long score = obtainDomainTypeScoreForVariables(useDefinitionInformation);

      if (preference.scorer.apply(Triple.of(score, bestScore, currentErrorPath.size()))) {
        bestScore = score;
        bestIndex = pPrefixes.indexOf(currentPrefix);
      }
    }

    return buildPath(bestIndex, pPrefixes);
  }

  private MutableARGPath obtainRefinementRootHeuristicBasedPrefix(List<MutableARGPath> pPrefixes, ErrorPathPrefixPreference preference) {
    if (!classification.isPresent()) {
      return concatPrefixes(pPrefixes);
    }

    MutableARGPath currentErrorPath = new MutableARGPath();
    Long bestScore                  = null;
    int bestIndex                   = 0;

    for (MutableARGPath currentPrefix : pPrefixes) {
      assert (currentPrefix.getLast().getSecond().getEdgeType() == CFAEdgeType.AssumeEdge);

      currentErrorPath.addAll(currentPrefix);

      // gets the score for the prefix of how "local" it is
      AssumptionUseDefinitionCollector collector = new InitialAssumptionUseDefinitionCollector();
      collector.obtainUseDefInformation(currentErrorPath);
      Long score = Long.valueOf(collector.getDependenciesResolvedOffset() * (-1));

      if (preference.scorer.apply(Triple.of(score, bestScore, currentErrorPath.size()))) {
        bestScore = score;
        bestIndex = pPrefixes.indexOf(currentPrefix);
      }
    }

    return buildPath(bestIndex, pPrefixes);
  }

  // not really a sensible heuristic at all, just here for comparison reasons
  private MutableARGPath obtainRandomPrefix(List<MutableARGPath> pPrefixes) {
    return buildPath(new Random().nextInt(pPrefixes.size()), pPrefixes);
  }

  // not really a sensible heuristic at all, just here for comparison reasons
  private MutableARGPath obtainMedianPrefix(List<MutableARGPath> pPrefixes) {
    return buildPath(pPrefixes.size() / 2, pPrefixes);
  }

  // not really a sensible heuristic at all, just here for comparison reasons
  private MutableARGPath obtainMiddlePrefix(List<MutableARGPath> pPrefixes) {
    int totalLength = 0;
    for (MutableARGPath p : pPrefixes) {
      totalLength += p.size();
    }

    int length = 0;
    int index = 0;
    for (MutableARGPath p : pPrefixes) {
      length += p.size();
      if (length > totalLength / 2) {
        break;
      }
      index++;
    }

    return buildPath(index, pPrefixes);
  }

  private Set<String> obtainUseDefInformationOfErrorPath(MutableARGPath currentErrorPath) {
    return new InitialAssumptionUseDefinitionCollector().obtainUseDefInformation(currentErrorPath);
  }

  private Long obtainDomainTypeScoreForVariables(Set<String> useDefinitionInformation) {
    Long domainTypeScore = 1L;
    for (String variableName : useDefinitionInformation) {
      int factor = UNKNOWN_VAR;

      if (classification.get().getIntBoolVars().contains(variableName)) {
        factor = BOOLEAN_VAR;

      } else if (classification.get().getIntEqualVars().contains(variableName)) {
        factor = INTEQUAL_VAR;
      }

      domainTypeScore = domainTypeScore * factor;

      if (loopStructure.isPresent()
          && loopStructure.get().getLoopIncDecVariables().contains(variableName)) {
        domainTypeScore = domainTypeScore + Integer.MAX_VALUE;
      }
    }

    return domainTypeScore;
  }

  /**
   * This methods builds a new path from the given prefixes. It makes all
   * contradicting assume edge but the last ineffective, so that only the last
   * assumption leads to a contradiction.
   *
   * @param bestIndex the index of the prefix with the best score
   * @param pPrefixes the list of prefixes
   * @return a new path with the last assumption leading to a contradiction
   */
  private MutableARGPath buildPath(int bestIndex, List<MutableARGPath> pPrefixes) {
    MutableARGPath errorPath = new MutableARGPath();
    for (int j = 0; j <= bestIndex; j++) {
      errorPath.addAll(pPrefixes.get(j));

      if (j != bestIndex) {
        replaceAssumeEdgeWithBlankEdge(errorPath);
      }
    }

    // add bogus transition to prefix - needed, because during interpolation,
    // the last edge is never interpolated (assumed to be the error state),
    // so we need to add an extra transition, e.g., duplicate the last edge,
    // so that the assertion holds that the last transition is infeasible and
    // yields an interpolant that represents FALSE / a contradiction
    errorPath.add(BOGUS_TRANSITION);

    return errorPath;
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
        "replacement for assume edge")));
  }

  private MutableARGPath concatPrefixes(List<MutableARGPath> pPrefixes) {
    MutableARGPath errorPath = new MutableARGPath();
    for (MutableARGPath currentPrefix : pPrefixes) {
      errorPath.addAll(currentPrefix);
    }

    return errorPath;
  }

  private static final CFAEdge BOGUS_EDGE = new CDeclarationEdge("",
      FileLocation.DUMMY,
      new CFANode("bogus"),
      new CFANode("bogus"),
      new CVariableDeclaration(FileLocation.DUMMY, false, CStorageClass.AUTO, CNumericTypes.INT, "", "", "", null));

  /**
   * a bogus transition, containing the null-state, and a declaration edge, with basically no side effect (may not be a
   * blank edge, due to implementation details)
   */
  private static final Pair<ARGState, CFAEdge> BOGUS_TRANSITION = Pair.<ARGState, CFAEdge>of(null, BOGUS_EDGE);

  /**
   * This method export the current error path, visualizing the individual prefixes.
   *
   * @param errorPath the original error path
   * @param pPrefixes the list of prefixes
   */
  private void exportToDot(MutableARGPath errorPath, List<MutableARGPath> pPrefixes) {
    SetMultimap<ARGState, ARGState> successorRelation = buildSuccessorRelation(errorPath.getLast().getFirst());

    Set<ARGState> failingStates = new HashSet<>();
    for(MutableARGPath path : pPrefixes) {
      failingStates.add(path.getLast().getFirst());
    }

    int assertFailCnt = failingStates.size();
    StringBuilder result = new StringBuilder().append("digraph tree {" + "\n");
    for (Map.Entry<ARGState, ARGState> current : successorRelation.entries()) {
      result.append(current.getKey().getStateId() + " [label=\"" + current.getKey().getStateId() + "\"]" + "\n");
      result.append(current.getKey().getStateId() + " -> " + current.getValue().getStateId() + "\n");

      CFAEdge edge = current.getKey().getEdgeToChild(current.getValue());

      if(failingStates.contains(current.getValue())) {
        result.append(current.getKey().getStateId() + " [shape=diamond, style=filled, fillcolor=\"red\"]" + "\n");
        result.append(current.getKey().getStateId() + " -> stop" + assertFailCnt + "\n");
        result.append("stop" + assertFailCnt + " [shape=point]\n");
        assertFailCnt--;
      }

      else if(edge.getEdgeType() == CFAEdgeType.AssumeEdge) {
        result.append(current.getKey().getStateId() + " [shape=diamond]" + "\n");
      }

      assert (!current.getKey().isTarget());
    }
    result.append("}");

    try {
      Files.writeFile(Paths.get("output/itpPaths" + (invocationCounter++) + ".dot"), result.toString());
    } catch (IOException e) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * This method creates a successor relation from the root to the target state.
   *
   * @param target the state to which the successor relation should be built.
   * @return the successor relation from the root state to the given target state
   */
  private SetMultimap<ARGState, ARGState> buildSuccessorRelation(ARGState target) {
    Deque<ARGState> todo = new ArrayDeque<>();
    todo.add(target);
    ARGState itpTreeRoot = null;

    SetMultimap<ARGState, ARGState> successorRelation = LinkedHashMultimap.create();

    // build the tree, bottom-up, starting from the target states
    while (!todo.isEmpty()) {
      final ARGState currentState = todo.removeFirst();

      if (currentState.getParents().iterator().hasNext()) {
        ARGState parentState = currentState.getParents().iterator().next();
        todo.add(parentState);
        successorRelation.put(parentState, currentState);

      } else if (itpTreeRoot == null) {
        itpTreeRoot = currentState;
      }
    }

    return successorRelation;
  }
}