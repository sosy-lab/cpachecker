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

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.testgen.TestGenStatistics;
import org.sosy_lab.cpachecker.core.algorithm.testgen.iteration.PredicatePathAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.CFAUtils2;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.base.Optional;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;


public class BasicPathSelector implements PathSelector {

  private TestGenStatistics stats;
  ConfigurableProgramAnalysis cpa;
  private LogManager logger;

  protected PathValidator pathValidator;

  public BasicPathSelector(PathValidator pPathValidator, StartupConfig config, TestGenStatistics pStats) {
    super();
    pathValidator = pPathValidator;
    this.logger = config.getLog();
    stats = pStats;
  }

  @Override
  public PredicatePathAnalysisResult findNewFeasiblePathUsingPredicates(final ARGPath pExecutedPath,
      ReachedSet reachedStates)
      throws CPATransferException, InterruptedException {
    /*
     * create copy of the given path, because it will be modified with this algorithm.
     * represents the current new valid path.
     */
    MutableARGPath newARGPath = pExecutedPath.mutableCopy();
    PathInfo pathInfo = new PathInfo(newARGPath.size());
    //    ARGPath newARGPathView = Collections.unmodifiableList(newARGPath);
    /*
     * only by edge representation of the new path.
     */
    List<CFAEdge> newPath = Lists.newArrayList(newARGPath.asEdgesList());
    /*
     * element removed from the path in the previous iteration
     */
    Pair<ARGState, CFAEdge> lastElement = null;
    Pair<ARGState, CFAEdge> currentElement;
    /*
     * this is a variation of the solve_path_constraint(..., path_constraint, stack) function of DART.
     *
     */
    /*
     * create a descending consuming iterator to iterate through the path from last to first, while consuming elements.
     * Elements are consumed because the new path is a subpath of the original.
     */
    Iterator<Pair<ARGState, CFAEdge>> descendingPathElements =
        Iterators.consumingIterator(newARGPath.descendingIterator());
    pathValidator.handleNewCheck(pExecutedPath);
    while (descendingPathElements.hasNext())
    {
      pathInfo.increaseNodeCount();
      currentElement = descendingPathElements.next();
      CFAEdge edge = currentElement.getSecond();
      //handle last node of the given path. (should never be a decision node, so we skip it)
      if (edge == null) {
        lastElement = currentElement;
        continue;
      }
      pathValidator.handleNext(pathInfo, edge);

      CFANode node = edge.getPredecessor();
      //num of leaving edges does not include a summary edge, so the check is valid.
      if (node.getNumLeavingEdges() != 2) {
        pathValidator.handleSinglePathElement(currentElement);
        lastElement = currentElement;
        continue;
      }
      /*
       * current node is a branching / deciding node. select the edge that isn't represented
       * with the current path.
       */
      pathInfo.increaseBranchCount();
      CFANode decidingNode = node;
      CFAEdge wrongEdge = edge;

      /*
       * (DART: negate the path constraint)
       */
      Optional<CFAEdge> otherEdge = CFAUtils2.getAlternativeLeavingEdge(decidingNode, wrongEdge);
      //no edge found should not happen because we filtered nodes such that only those with more than one leaving edge encounter this.; If it does make it visible.
      assert otherEdge.isPresent();

      /*
       * (DART: the j = pathLength-1 case)
       */
      if (pathValidator.isVisitedBranching(newARGPath, currentElement, node, otherEdge.get()))
      {
        logger.log(Level.FINER, "Branch on path was handled in an earlier iteration -> skipping branching.");
        lastElement = currentElement;
        pathValidator.handleVisitedBranching(newARGPath, currentElement);
        continue;
      }

      if (lastElement == null)
      {
        /*
         * if the last element is not set, we encountered a branching node where both paths are infeasible
         * for the current value mapping or both successors were handled already with a previous iteration.
         * (the successors are in reached and the CPAAlgorithms stops if all successors were reached before).
         */
        logger.log(Level.FINER,
            "encountered an executed path that continues into an already reached region. -> Skipping");
        lastElement = currentElement;
        continue;
      }

      /*
       * identified a decision node and selected a new edge.
       * extract the edge-list of the path and add the new edge to it.
       * Don't modify the ARGPath yet, because it is possible that the current decision is infeasible
       */
      logger.logf(Level.FINER, "identified new path candidate (visited branchings: %d, nodes: %d)",
          pathInfo.getBranchCount(),
          pathInfo.getNodeCount());
      newPath = Lists.newArrayList(newARGPath.asEdgesList());
      newPath.add(otherEdge.get());
      /*
       * evaluate path candidate symbolically using SMT-solving
       */
      stats.beforePathCheck();
      CounterexampleTraceInfo traceInfo = pathValidator.validatePathCandidate(currentElement, newPath);
      stats.afterPathCheck();
      /*
       * check if path is feasible. If it's not continue to identify another decision node
       * If path is feasible, add the ARGState belonging to the decision node and the new edge to the ARGPath. Exit and Return result.
       */
      if (!traceInfo.isSpurious())
      {
        newARGPath.add(Pair.of(currentElement.getFirst(), otherEdge.get()));
        logger.logf(Level.FINEST, "selected new path %s", newPath.toString());
        PredicatePathAnalysisResult result = new PredicatePathAnalysisResult(traceInfo, currentElement.getFirst(), lastElement.getFirst(), newARGPath.immutableCopy());
        pathValidator.handleValidPath(result);
        return result;
      }
      else {
        lastElement = currentElement;
        logger.logf(Level.FINER, "path candidate is infeasible");
        continue;
      }

    }
    //all possible paths explored. (DART: the j = -1 case)
    logger.logf(Level.FINER, "No possible path left to explore");
    return PredicatePathAnalysisResult.INVALID;
  }



  @Override
  public CounterexampleTraceInfo computePredicateCheck(ARGPath pExecutedPath) throws CPATransferException,
      InterruptedException {
    return pathValidator.validatePath(pExecutedPath.getInnerEdges()
        );
  }


  public class PathInfo {

    private final long pathSize;
    private long currentPathSize;
    private long branchCount = 0;
    private long nodeCount = 0;

    public PathInfo(long pPathSize) {
      super();
      pathSize = pPathSize;
      currentPathSize = pathSize;
      branchCount = 0;
      nodeCount = 0;
    }

    public long getPathSize() {
      return pathSize;
    }

    public long getCurrentPathSize() {
      return currentPathSize;
    }

    public long getBranchCount() {
      return branchCount;
    }

    public long getNodeCount() {
      return nodeCount;
    }

    protected long increaseNodeCount() {
      ++nodeCount;
      currentPathSize = pathSize - nodeCount;
      return nodeCount;
    }

    protected long increaseBranchCount() {
      return ++branchCount;
    }
  }


}
