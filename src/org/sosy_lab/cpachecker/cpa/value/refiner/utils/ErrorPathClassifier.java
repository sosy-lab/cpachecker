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
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Optional;

public class ErrorPathClassifier {

  private static final int BOOLEAN_VAR   = 2;
  private static final int INTEQUAL_VAR  = 4;
  private static final int UNKNOWN_VAR   = 16;

  private static final int MAX_PREFIX_LENGTH = 1000;

  private final Optional<VariableClassification> classification;

  public static enum ErrorPathPrefixPreference {
    DEFAULT,
    SHORTEST,
    LONGEST,
    MEDIAN,
    MIDDLE,
    BEST,
    WORST
  }

  public ErrorPathClassifier(Optional<VariableClassification> pClassification) throws InvalidConfigurationException {
    classification = pClassification;
  }

  public ARGPath obtainPrefix(ErrorPathPrefixPreference preference, ARGPath errorPath, List<ARGPath> pPrefixes) {
    switch (preference) {
    case SHORTEST:
      return obtainShortestPrefix(pPrefixes);

    case LONGEST:
      return obtainLongestPrefix(pPrefixes);

    case MEDIAN:
      return obtainMedianPrefix(pPrefixes);

    case MIDDLE:
      return obtainMiddlePrefix(pPrefixes);

    case BEST:
      return obtainBestPrefix(pPrefixes);

    case WORST:
      return obtainWorstPrefix(pPrefixes);

    default:
      return errorPath;
    }
  }

  public ARGPath obtainShortestPrefix(List<ARGPath> pPrefixes) {
    return buildPath(0, pPrefixes);
  }

  public ARGPath obtainLongestPrefix(List<ARGPath> pPrefixes) {
    return buildPath(pPrefixes.size() - 1, pPrefixes);
  }

  public ARGPath obtainMedianPrefix(List<ARGPath> pPrefixes) {
    return buildPath(pPrefixes.size() / 2, pPrefixes);
  }

  public ARGPath obtainMiddlePrefix(List<ARGPath> pPrefixes) {
    int totalLength = 0;
    for(ARGPath p : pPrefixes) {
      totalLength += p.size();
    }

    int length = 0;
    int index = 0;
    for(ARGPath p : pPrefixes) {
      length += p.size();
      if(length > totalLength / 2) {
        break;
      }
      index++;
    }

    return buildPath(index, pPrefixes);
  }

  public ARGPath obtainBestPrefix(List<ARGPath> pPrefixes) {

    if (!classification.isPresent()) {
      return concatPrefixes(pPrefixes);
    }

    ARGPath currentErrorPath  = new ARGPath();
    Long bestScore            = null;
    int bestIndex             = 0;

    for (ARGPath currentPrefix : pPrefixes) {
      assert(currentPrefix.getLast().getSecond().getEdgeType() == CFAEdgeType.AssumeEdge);

      currentErrorPath.addAll(currentPrefix);

      Set<String> useDefinitionInformation = obtainUseDefInformationOfErrorPath(currentErrorPath);

      Long score = obtainScoreForVariables(useDefinitionInformation);

      // score <= bestScore chooses the last, based on iteration order, that has the best or equal-to-best score
      // maybe a real tie-breaker rule would be better, e.g. total number of variables, number of references, etc.
      if (bestScore == null || isBestScore(score, bestScore, currentErrorPath)) {
        bestScore = score;
        bestIndex = pPrefixes.indexOf(currentPrefix);
      }
    }

    return buildPath(bestIndex, pPrefixes);
  }

  public ARGPath obtainWorstPrefix(List<ARGPath> pPrefixes) {

    if (!classification.isPresent()) {
      return concatPrefixes(pPrefixes);
    }

    ARGPath currentErrorPath  = new ARGPath();
    Long bestScore            = null;
    int bestIndex             = 0;

    for (ARGPath currentPrefix : pPrefixes) {
      assert(currentPrefix.getLast().getSecond().getEdgeType() == CFAEdgeType.AssumeEdge);

      currentErrorPath.addAll(currentPrefix);

      Set<String> useDefinitionInformation = obtainUseDefInformationOfErrorPath(currentErrorPath);

      Long score = obtainScoreForVariables(useDefinitionInformation);

      if (bestScore == null || isWorstScore(score, bestScore)) {
        bestScore = score;
        bestIndex = pPrefixes.indexOf(currentPrefix);
      }
    }

    return buildPath(bestIndex, pPrefixes);
  }

  /**
   * This method checks if the currentScore is better then the current optimum.
   *
   * A lower score is always favored. In case of a draw, the later, deeper score
   * is favored, unless the error path exceeds the {@link #MAX_PREFIX_LENGTH} limit,
   * then the earlier, more shallow score is favored. This avoids extremely long
   * error traces (that take longer during interpolation).
   *
   * @param currentScore the current score
   * @param currentBestScore the current optimum
   * @param currentErrorPath the current error path
   * @return true, if the current score is a new optimum, else false
   */
  private boolean isBestScore(Long currentScore, Long currentBestScore, ARGPath currentErrorPath) {
    if (currentErrorPath.size() < MAX_PREFIX_LENGTH) {
      return currentScore <= currentBestScore;
    }

    else {
      return currentScore < currentBestScore;
    }
  }

  private boolean isWorstScore(Long currentScore, Long currentBestScore) {
    return currentScore >= currentBestScore;
  }

  private Set<String> obtainUseDefInformationOfErrorPath(ARGPath currentErrorPath) {
    return new InitialAssumptionUseDefinitionCollector().obtainUseDefInformation(currentErrorPath);
  }

  private Long obtainScoreForVariables(Set<String> useDefinitionInformation) {
    Long score = 1L;
    for (String variableName : useDefinitionInformation) {
      int factor = UNKNOWN_VAR;

      if (classification.get().getIntBoolVars().contains(variableName)) {
        factor = BOOLEAN_VAR;
      }

      else if (classification.get().getIntEqualVars().contains(variableName)) {
        factor = INTEQUAL_VAR;
      }

      score = score * factor;

      if (classification.get().getLoopIncDecVariables().contains(variableName)) {
        score = score + Integer.MAX_VALUE;
      }
    }

    return score;
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
  private ARGPath buildPath(int bestIndex, List<ARGPath> pPrefixes) {
    ARGPath errorPath = new ARGPath();
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
  private void replaceAssumeEdgeWithBlankEdge(final ARGPath pErrorPath) {
    Pair<ARGState, CFAEdge> assumeState = pErrorPath.removeLast();

    assert(assumeState.getSecond().getEdgeType() == CFAEdgeType.AssumeEdge);

    pErrorPath.add(Pair.<ARGState, CFAEdge>of(assumeState.getFirst(), new BlankEdge("",
        FileLocation.DUMMY,
        assumeState.getSecond().getPredecessor(),
        assumeState.getSecond().getSuccessor(),
        "replacement for assume edge")));
  }

  private ARGPath concatPrefixes(List<ARGPath> pPrefixes) {
    ARGPath errorPath = new ARGPath();
    for (ARGPath currentPrefix : pPrefixes) {
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
}
