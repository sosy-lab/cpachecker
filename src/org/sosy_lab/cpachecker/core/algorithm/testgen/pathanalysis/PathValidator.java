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

import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.testgen.iteration.PredicatePathAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.testgen.pathanalysis.BasicPathSelector.PathInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

/**
 * validates given paths using SMT-Solving. May reject other paths as well, depending on previous calls to this instance.
 * @See {@link PathSelector} and {@link BasicPathSelector}
 */
public interface PathValidator {

  public CounterexampleTraceInfo validatePathCandidate(Pair<ARGState, CFAEdge> pCurrentElement, List<CFAEdge> pNewPath)throws CPATransferException, InterruptedException;

  public CounterexampleTraceInfo validatePath(List<CFAEdge> pAsEdgesList) throws CPATransferException, InterruptedException;

  /**
   * checks if the given node is a possible candidate for a new path branching point.
   * Should not modify internal state and never the given path.
   * If modification is required use {@link #handleVisitedBranching(ARGPath, Pair)}.
   * @param pNewARGPath
   * @param pCurrentElement
   * @param pNode
   * @param pOtherEdge
   * @return
   */
  public boolean isVisitedBranching(final ARGPath pNewARGPath,final Pair<ARGState, CFAEdge> pCurrentElement,final CFANode pNode,
      final CFAEdge pOtherEdge);

  // the following methods are hooks to trigger an action of this validator when the defined event occurs in the PathSelector.

  /**
   * triggered on a new check directly before the iteration starts.
   *  (good as a hook to prepare or reset internals for a new run)
   * @param pExecutedPath
   */
  public void handleNewCheck(ARGPath pExecutedPath);

  /**
   * triggered after an element of the path is consumed.
   * Does not trigger if the path ends at the consumed node (no leaving edge) or the element is invalid.
   * @param pathInfo
   * @param edge
   */
  public void handleNext(PathInfo pathInfo, CFAEdge edge);

  /**
   * triggered after a new feasible path is identified.
   * @param result
   */
  public void handleValidPath(PredicatePathAnalysisResult result);

  /**
   * triggered if a path candidate is identified as infeasible.
   * @param pNewPath
   */
  public void handleSpuriousPath(List<CFAEdge> pNewPath);

  /**
   *
   * @param pCurrentElement
   */
  public void handleSinglePathElement(Pair<ARGState, CFAEdge> pCurrentElement);

  /**
   * triggered on {@link #isVisitedBranching(ARGPath, Pair, CFANode, CFAEdge)} == true.
   *
   * @param pNewARGPath
   * @param pCurrentElement
   */
  public void handleVisitedBranching(ARGPath pNewARGPath, Pair<ARGState, CFAEdge> pCurrentElement);

}
