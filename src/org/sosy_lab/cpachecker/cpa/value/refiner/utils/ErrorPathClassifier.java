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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Optional;


public class ErrorPathClassifier {

  public static final int BOOLEAN_VAR   = 2;
  public static final int INTEQUAL_VAR  = 4;
  public static final int UNKNOWN_VAR   = 16;

  final Optional<VariableClassification> classification;

  public ErrorPathClassifier(Optional<VariableClassification> pClassification) {
    classification = pClassification;
  }

  public ARGPath obtainPrefixWithLowestScore(List<ARGPath> pPrefixes) {

    if(!classification.isPresent()) {
      return concatPrefixes(pPrefixes);
    }

    ARGPath currentErrorPath  = new ARGPath();
    long lowestScore          = Long.MAX_VALUE;
    int lowestIndex           = 0;

    for(ARGPath currentPrefix : pPrefixes) {
      assert(currentPrefix.getLast().getSecond().getEdgeType() == CFAEdgeType.AssumeEdge);

      currentErrorPath.addAll(currentPrefix);

      Set<String> useDefinitionInformation = obtainUseDefInformationOfErrorPath(currentErrorPath);

      long score = obtainScoreForVariables(useDefinitionInformation);

      if(score <= lowestScore) {
        lowestScore = score;
        lowestIndex = pPrefixes.indexOf(currentPrefix);
      }
    }

    return buildPath(lowestIndex, pPrefixes);
  }

  private Set<String> obtainUseDefInformationOfErrorPath(ARGPath currentErrorPath) {
    return new InitialAssumptionUseDefinitionCollector().obtainUseDefInformation(currentErrorPath);
  }

  private long obtainScoreForVariables(Set<String> useDefinitionInformation) {
    long score = 1;
    for(String variableName : useDefinitionInformation) {
      int factor = UNKNOWN_VAR;

      if(classification.get().getIntBoolVars().contains(variableName)) {
        factor = BOOLEAN_VAR;
      }

      else if(classification.get().getIntEqualVars().contains(variableName)) {
        factor = INTEQUAL_VAR;
      }

      score = score * factor;
    }

    return score;
  }

  private ARGPath buildPath(int lastPrefixIndex, List<ARGPath> pPrefixes) {
    ARGPath errorPath = new ARGPath();
    for(int j = 0; j <= lastPrefixIndex; j++) {
      errorPath.addAll(pPrefixes.get(j));

      // remove the last (assume) edge of all prefixes, except in the last prefix
      if(j != lastPrefixIndex) {
        Pair<ARGState, CFAEdge> lastStateOfCurrentPrefix = errorPath.removeLast();

        assert(lastStateOfCurrentPrefix.getSecond().getEdgeType() == CFAEdgeType.AssumeEdge);
      }
    }

    return errorPath;
  }

  private ARGPath concatPrefixes(List<ARGPath> pPrefixes) {
    ARGPath errorPath = new ARGPath();
    for(ARGPath currentPrefix : pPrefixes) {
      errorPath.addAll(currentPrefix);
    }

    return errorPath;
  }
}
