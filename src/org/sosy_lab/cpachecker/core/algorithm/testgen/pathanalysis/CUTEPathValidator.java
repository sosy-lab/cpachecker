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
package org.sosy_lab.cpachecker.core.algorithm.testgen.pathanalysis;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.testgen.iteration.PredicatePathAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.testgen.pathanalysis.BasicPathSelector.PathInfo;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;


public class CUTEPathValidator extends AbstractPathValidator{

  private final PathChecker patchChecher;
  private final BranchingHistory branchingHistory;
  protected Pair<CFAEdge, Boolean> oldElement;

  public CUTEPathValidator(PathChecker pPatchChecher, StartupConfig pConfig) {
    super(pConfig);
    this.patchChecher = pPatchChecher;
    branchingHistory = new BranchingHistory();
    oldElement = null;
  }

  @Override
  public void handleNewCheck(ARGPath pExecutedPath) {
    oldElement = null;
  }

  @Override
  public CounterexampleTraceInfo validatePath(List<CFAEdge> pPath) throws CPATransferException,
      InterruptedException {
    return patchChecher.checkPath(pPath);
  }

  @Override
  public boolean isVisitedBranching(ARGPath pNewARGPath, Pair<ARGState, CFAEdge> pCurrentElement, CFANode pNode,
      CFAEdge pOtherEdge) {
    return isVisited(pCurrentElement, pOtherEdge);
  }

  @Override
  public void handleVisitedBranching(ARGPath pNewARGPath, Pair<ARGState, CFAEdge> pCurrentElement) {
    // nothing to to

  }

  @Override
  public void handleNext(PathInfo pathInfo, CFAEdge edge) {
    handleNextNode(pathInfo.getCurrentPathSize(), edge);
    logger.logf(Level.FINEST, "StackState: %d %d (%d)", pathInfo.getCurrentPathSize(),
        branchingHistory.getCurrentDepths(),
        branchingHistory.getPathDepths());
  }

  @Override
  public void handleValidPath(PredicatePathAnalysisResult result) {
    branchingHistory.resetTo(result.getPath());
  }

  private boolean isVisited(Pair<ARGState, CFAEdge> currentElement, CFAEdge otherEdge) {
    if (oldElement != null)
    {
      logger.log(Level.FINER, "Matching path length. Possibly handled this branch earlier");
      if (branchingHistory.isVisited(otherEdge, oldElement)) {
        return true;
      }
      else
      {
        logger.log(Level.FINER, "Same path length but not in predicted section.");
        return false;
      }
    }
    return false;
  }


  private Pair<CFAEdge, Boolean> handleNextNode(long currentPathSize, CFAEdge edge) {
    if (branchingHistory.getCurrentDepths() > currentPathSize + 1)
    {
      branchingHistory.consumeUntilSameSize(currentPathSize);
      logger.logf(Level.FINER, "comsumed until %d %d (%d)", currentPathSize, branchingHistory.getCurrentDepths(),
          branchingHistory.getPathDepths());
    }
    if (branchingHistory.isPathCandidateForPredictedSection(edge, currentPathSize))
    {
      branchingHistory.hasNext();
      oldElement = branchingHistory.next();
      logger.logf(Level.FINER, "Is path candidate for predicted section");
    }
    return oldElement;
  }



  class BranchingHistory {

    Iterator<CFAEdge> descendingEdgePath;
    Map<CFAEdge, Boolean> visitedEdges;
    Iterator<Pair<CFAEdge, Boolean>> edgeHistory;

    long pathDepths = 0;
    long currentDepths = 0;


    public BranchingHistory() {
      descendingEdgePath = Collections.emptyIterator();
      visitedEdges = Maps.newHashMap();
      edgeHistory = Iterators.transform(descendingEdgePath, new Function<CFAEdge, Pair<CFAEdge, Boolean>>() {

        @Override
        public Pair<CFAEdge, Boolean> apply(CFAEdge pInput) {
          return Pair.of(pInput, visitedEdges.get(pInput));
        }

      });
    }

    public void consumeUntilSameSize(long pCurrentSizeOfPath) {
      while (edgeHistory.hasNext() && (pCurrentSizeOfPath + 1) < currentDepths)
      {
        next();
      }
    }

    public boolean isPathCandidateForPredictedSection(CFAEdge pEdge, long pCurrentPathLength) {
      return pCurrentPathLength < currentDepths;
    }

    public void resetTo(ARGPath argPath) {
      descendingEdgePath = Iterators.transform(argPath.descendingIterator(), Pair.<CFAEdge> getProjectionToSecond());
      edgeHistory = Iterators.transform(descendingEdgePath, new Function<CFAEdge, Pair<CFAEdge, Boolean>>() {

        @Override
        public Pair<CFAEdge, Boolean> apply(CFAEdge pInput) {
          return Pair.of(pInput, visitedEdges.get(pInput));
        }

      });
      pathDepths = argPath.size();
      currentDepths = pathDepths;
      visitedEdges.put(argPath.getLast().getSecond(), true);
    }


    public boolean isVisited(CFAEdge edgeToCheck, Pair<CFAEdge, Boolean> oldEdge) {

      //      Pair<CFAEdge, Boolean> oldEdge = edgeHistory.next();
      assert oldEdge.getFirst().getPredecessor().equals(edgeToCheck.getPredecessor()) : "Illegal State of history. Wrong edge executed.";
      if (oldEdge.getSecond() == null)
      {
        logger.log(Level.FINER, "Didn't find a 'visited' match. Not a branching edge or a skipped edge.");
        return false;
      }
      return oldEdge.getSecond();
    }

    public boolean hasNext() {
      return edgeHistory.hasNext();
    }

    public Pair<CFAEdge, Boolean> next() {
      assert edgeHistory.hasNext() : "Illegal State of history. Check if this method was called to often.";
      currentDepths--;
      return edgeHistory.next();
    }

    public long getPathDepths() {
      return pathDepths;
    }

    public long getCurrentDepths() {
      return currentDepths;
    }

  }

}
