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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.testgen.TestGenAlgorithm.AnalysisStrategySelector;
import org.sosy_lab.cpachecker.core.algorithm.testgen.TestGenStatistics;
import org.sosy_lab.cpachecker.core.algorithm.testgen.iteration.PredicatePathAnalysisResult;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.CFAUtils2;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @Deprecated replaced by {@link BasicPathSelector} in conjunction with {@link CUTEPathValidator}. Use {@link AnalysisStrategySelector#CUTE_PATH_SELECTOR}
 */
@Deprecated
public class CUTEBasicPathSelector implements PathSelector {

  private TestGenStatistics stats;
  private LogManager logger;
  private BranchingHistory branchingHistory;
  private PathChecker pathChecker;


  public CUTEBasicPathSelector(PathChecker pPathChecker, StartupConfig config, TestGenStatistics pStats) {
    super();
    this.pathChecker = pPathChecker;
    this.logger = config.getLog();
    stats = pStats;
    branchingHistory = new BranchingHistory();
  }


  @Override
  public PredicatePathAnalysisResult findNewFeasiblePathUsingPredicates(final ARGPath pExecutedPath,
      ReachedSet reachedStates)
      throws CPATransferException, InterruptedException {
    /*
     * create copy of the given path, because it will be modified with this algorithm.
     * represents the current new valid path.
     */
    ARGPath newARGPath = new ARGPath();
    newARGPath.addAll(pExecutedPath);
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

    while (descendingPathElements.hasNext())
    {
      pathInfo.increaseNodeCount();
      currentElement = descendingPathElements.next();
      CFAEdge edge = currentElement.getSecond();
      Pair<CFAEdge, Boolean> oldElement = null;
      //handle last node of the given path. (should never be a decision node, so we skip it)
      if (edge == null) {
        lastElement = currentElement;
        continue;
      }
      oldElement = handleNextNode(pathInfo.getCurrentPathSize(), edge, oldElement);
      CFANode node = edge.getPredecessor();
      //num of leaving edges does not include a summary edge, so the check is valid.
      if (node.getNumLeavingEdges() != 2) {
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
      //      if(branchingHistory.isMatch(otherEdge, oldEdge))
      //no edge found should not happen because we filtered nodes such that only those with more than one leaving edge encounter this.; If it does make it visible.
      assert otherEdge.isPresent();
      logger.logf(Level.FINEST, "StackState: %d %d (%d)", pathInfo.getCurrentPathSize(),
          branchingHistory.getCurrentDepths(),
          branchingHistory.getPathDepths());
      /*
       * (DART: the j = -1 case)
       */
      //      if(pathValidator.isVisitedBranching(newARGPath, currentElement, node, otherEdge))
      if (isVisited(currentElement, oldElement, otherEdge.get()))
      {
        logger.log(Level.FINER, "Branch on path was handled in an earlier iteration -> skipping branching.");
        lastElement = currentElement;
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
      CounterexampleTraceInfo traceInfo = pathChecker.checkPath(newPath);
      stats.afterPathCheck();
      /*
       * check if path is feasible. If it's not continue to identify another decision node
       * If path is feasible, add the ARGState belonging to the decision node and the new edge to the ARGPath. Exit and Return result.
       */
      if (!traceInfo.isSpurious())
      {
        newARGPath.add(Pair.of(currentElement.getFirst(), otherEdge.get()));
        logger.logf(Level.FINEST, "selected new path %s", newPath.toString());
        //        pathValidator.handleValidPath(newARGPath, traceInfo);
        branchingHistory.resetTo(newARGPath);
        return new PredicatePathAnalysisResult(traceInfo, currentElement.getFirst(), lastElement.getFirst(), newARGPath);
      }
      else {
        lastElement = currentElement;
        logger.logf(Level.FINER, "path candidate is infeasible");
        //        pathValidator.handleSpuriousPath(newPath);
        continue;
      }

    }
    //all possible paths explored. (DART: the j = -1 case)
    logger.logf(Level.FINER, "No possible path left to explore");
    return PredicatePathAnalysisResult.INVALID;
  }


  private boolean isVisited(Pair<ARGState, CFAEdge> currentElement, Pair<CFAEdge, Boolean> oldElement, CFAEdge otherEdge) {
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


  private Pair<CFAEdge, Boolean> handleNextNode(long currentPathSize, CFAEdge edge,
      Pair<CFAEdge, Boolean> oldElement) {
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



  @Override
  public CounterexampleTraceInfo computePredicateCheck(ARGPath pExecutedPath) throws CPATransferException,
      InterruptedException {
    return pathChecker.checkPath(pExecutedPath.asEdgesList()
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

    //    public void setPathDepths(int pPathDepths) {
    //      pathDepths = pPathDepths;
    //      if(currentDepths >= pathDepths) {
    //        currentDepths = pathDepths;
    //      }
    //    }

  }

}
