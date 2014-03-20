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
package org.sosy_lab.cpachecker.core.algorithm.testgen.analysis;

import java.util.Iterator;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.collect.Iterators;


public class DARTLikePathValidationStrategy extends AbstractPathValidationStrategy {

  private BranchingHistory branchingHistory;
  private PathChecker pathChecker;
  private int currentExecutedPathSize;


  public DARTLikePathValidationStrategy(PathChecker pPathChecker) {
    super();
    pathChecker = pPathChecker;
    branchingHistory = new BranchingHistory();
  }

  @Override
  public CounterexampleTraceInfo checkPath(List<CFAEdge> pPath) throws CPATransferException, InterruptedException {
    return pathChecker.checkPath(pPath);
  }

  @Override
  public boolean isVisitedBranching(ARGPath pNewARGPath, Pair<ARGState, CFAEdge> pCurrentElement, CFANode pNode,
      CFAEdge pOtherEdge) {
    int pathSize = pNewARGPath.size();
    // check if the given subpath 'matches' the predicted section
    if (branchingHistory.getPathDepths() <= pathSize) {
      if (branchingHistory.getPathDepths() < pathSize)
      {
        //        TODO Log it
      }
      return branchingHistory.isLastVisited(pCurrentElement.getSecond());
    }
    else {
      //it is allowed to visit the same branch multiple times, if the path is in a new section (not predicted)
      return false;
    }
  }

  @Override
  public void handleNewCheck(ARGPath pExecutedPath) {
    this.currentExecutedPathSize = pExecutedPath.size();
  }

  @Override
  public void handleNext(long pNodeCounter) {
    currentExecutedPathSize--;
  }

  @Override
  public void handleSinglePathElement(Pair<ARGState, CFAEdge> pCurrentElement) {
    if (branchingHistory.getPathDepths() == currentExecutedPathSize) {
      branchingHistory.removeEdge(pCurrentElement.getSecond());
    }
  }

  @Override
  public void handleVisitedBranching(ARGPath pNewARGPath, Pair<ARGState, CFAEdge> pCurrentElement) {
    if (branchingHistory.getPathDepths() == currentExecutedPathSize) {
      branchingHistory.removeBranchEdge(pCurrentElement.getSecond());
    }
  }

  @Override
  public void handleSpuriousPath(List<CFAEdge> pNewPath) {
    if(branchingHistory.getPathDepths() == currentExecutedPathSize) {
      branchingHistory.removeBranchEdge(pNewPath.get(pNewPath.size() - 1));
    }
  }

  @Override
  public void handleValidPath(ARGPath pNewARGPath, CounterexampleTraceInfo pTraceInfo) {
    branchingHistory.setPathDepths(pNewARGPath.size());
    branchingHistory.resetTo(pNewARGPath);
  }

  class BranchingHistory {

    Iterator<CFAEdge> descendingEdgePath;
    int pathDepths = 0;


    public BranchingHistory() {}


    public void resetTo(ARGPath argPath) {
      descendingEdgePath = Iterators.transform(argPath.descendingIterator(), Pair.<CFAEdge> getProjectionToSecond());
    }


    public boolean isLastVisited(CFAEdge edgeToCheck) {
      if (descendingEdgePath.hasNext()) { return descendingEdgePath.next().getPredecessor()
          .equals(edgeToCheck.getPredecessor()); }
      return false;
    }


    public void removeBranchEdge(CFAEdge edge) {
      if (descendingEdgePath.hasNext())
      {
        CFAEdge lastEdge = descendingEdgePath.next();
        if (lastEdge.getPredecessor().equals(edge.getPredecessor())) {
          descendingEdgePath.remove();
          pathDepths--;
        } else {
          throw new IllegalStateException("wrong state of path history.");
        }
      }
    }

    public void removeEdge(CFAEdge edge) {
      if (descendingEdgePath.hasNext())
      {
        CFAEdge lastEdge = descendingEdgePath.next();
        if (lastEdge.equals(edge)) {
          descendingEdgePath.remove();
          pathDepths--;
        } else {
          throw new IllegalStateException("wrong state of path history.");
        }
      }

    }

    public int getPathDepths() {
      return pathDepths;
    }

    public void setPathDepths(int pPathDepths) {
      pathDepths = pPathDepths;
    }

  }

}
