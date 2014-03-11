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
package org.sosy_lab.cpachecker.core.algorithm.testgen;

import java.util.Iterator;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.testgen.model.PredicatePathAnalysisResult;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;


public class BasicTestGenPathAnalysisStrategy implements TestGenPathAnalysisStrategy {

  private PathChecker pathChecker;

  public BasicTestGenPathAnalysisStrategy(PathChecker pPathChecker) {
    super();
    pathChecker = pPathChecker;
  }


  @Override
  public PredicatePathAnalysisResult findNewFeasiblePathUsingPredicates(ARGPath pExecutedPath)
      throws CPATransferException, InterruptedException {
    List<CFAEdge> newPath = Lists.newArrayList(pExecutedPath.asEdgesList());
    ARGPath p = new ARGPath();
    Pair<ARGState,CFAEdge> decidingElement;
    Pair<ARGState,CFAEdge> wrongElement;
    Iterator<Pair<ARGState, CFAEdge>> branchingEdges =
        Iterators.filter(Iterators.consumingIterator(pExecutedPath.descendingIterator()),
            new Predicate<Pair<ARGState, CFAEdge>>() {

              @Override
              public boolean apply(Pair<ARGState, CFAEdge> pInput) {
                CFAEdge lastEdge = pInput.getSecond();
                if (lastEdge == null) {
                return false;
                }
                CFANode decidingNode = lastEdge.getPredecessor();
                //num of leaving edges does not include a summary edge, so the check is valid.
                if (decidingNode.getNumLeavingEdges() == 2) {
                return true;
                }
                return false;
              }
            });
    while (branchingEdges.hasNext())
    {
      Pair<ARGState, CFAEdge> wrongPair = branchingEdges.next();
      wrongElement = wrongPair;
      decidingElement = pExecutedPath.getLast();
      CFAEdge wrongEdge = wrongPair.getSecond();
      CFANode decidingNode = wrongEdge.getPredecessor();
      CFAEdge otherEdge = null;
      for (CFAEdge cfaEdge : CFAUtils.leavingEdges(decidingNode)) {
        if (cfaEdge.equals(wrongEdge)) {
          continue;
        } else {
          otherEdge = cfaEdge;
          break;
        }
      }
      //should not happen; If it does make it visible.
      assert otherEdge != null;
      newPath = Lists.newArrayList(pExecutedPath.asEdgesList());
      newPath.add(otherEdge);
      CounterexampleTraceInfo traceInfo = pathChecker.checkPath(newPath);
      //      traceInfo.
      if (!traceInfo.isSpurious()) { return new PredicatePathAnalysisResult(traceInfo,decidingElement , wrongElement); }

    }
    return PredicatePathAnalysisResult.INVALID;
  }

}
