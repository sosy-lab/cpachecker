/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit.refiner;

import static com.google.common.collect.FluentIterable.from;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitCPA;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.ExplicitInterpolationBasedExplicitRefiner.ExplicitValueInterpolant;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ExplictFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.io.Files;

@Options(prefix="cpa.explicit.refiner")
public class ExplicitGlobalRefiner implements Refiner, StatisticsProvider {

  ExplicitInterpolationBasedExplicitRefiner interpolatingRefiner;
  ExplictFeasibilityChecker checker;

  private final LogManager logger;

  private final ARGCPA argCpa;

  private final ShutdownNotifier shutdownNotifier;

  // statistics
  private int refinementCalls = 0;

  private final Timer totalTime = new Timer();

  @Option(description="whether or not to do lazy-abstraction")
  private boolean doLazyAbstraction = true;

  @Option(description="checkForRepeatedRefinements")
  private boolean checkForRepeatedRefinements = true;

  @Option(description="whether or not to stop interpolation, when no increment was obtained from the last error trace that was interpolated")
  private boolean checkOnEmptyIncrement = false;
  private int skipOnEmptyIncrement = 0;

  @Option(description="perform check on relevance of error path")
  private boolean checkOnRelevance = false;
  private int skipOnRelevance = 0;

  @Option(description="perform check, and potential cut-off if incremental precision is found to be enough")
  private boolean checkOnIncrementalPrec = false;
  private int skipOnIncrementalPrec = 0;

  @Option(description="the order in which error paths should be refined", toUppercase=true, values={"DEFAULT", "ZIGZAG"})
  private String errorPathOrder = "DEFAULT";

  private int totalOfTargetsFound = 0;
  private int totalOfPathsFound = 0;
  private int totalOfPathsItped = 0;

  public static ExplicitGlobalRefiner create(final ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    final ExplicitCPA explicitCpa = CPAs.retrieveCPA(pCpa, ExplicitCPA.class);
    if (explicitCpa == null) {
      throw new InvalidConfigurationException(ExplicitGlobalRefiner.class.getSimpleName() + " needs a ExplicitCPA");
    }

    ExplicitGlobalRefiner refiner = new ExplicitGlobalRefiner(explicitCpa.getConfiguration(),
                                    explicitCpa.getLogger(),
                                    (ARGCPA)pCpa,
                                    explicitCpa.getShutdownNotifier(),
                                    explicitCpa.getCFA());

    explicitCpa.getStats().addRefiner(refiner);


    return refiner;
  }

  private ExplicitGlobalRefiner(final Configuration pConfig, final LogManager pLogger,
      final ARGCPA pArgCpa, final ShutdownNotifier pShutdownNotifier, final CFA pCfa)
          throws InvalidConfigurationException {

    logger = pLogger;
    argCpa = pArgCpa;
    shutdownNotifier = pShutdownNotifier;

    pConfig.inject(this);

    interpolatingRefiner  = new ExplicitInterpolationBasedExplicitRefiner(pConfig, pLogger, pShutdownNotifier, pCfa);
    checker = new ExplictFeasibilityChecker(pLogger, pCfa);
    lowestCommonAncestor = null;
  }

  private class InterpolationTree {
    private Map<ARGState, ARGState> predecessorRelation = Maps.newHashMap();

    private SetMultimap<ARGState, ARGState> successorRelation = LinkedHashMultimap.create();

    private Deque<ARGState> itpRoots = new ArrayDeque<>();

    private InterpolationTree(Collection<ARGState> targetStates) {
      Deque<ARGState> todo = new ArrayDeque<>(targetStates);

      while (!todo.isEmpty()) {
        final ARGState currentState = todo.removeFirst();
        assert currentState.mayCover();

        if(currentState.getParents().iterator().hasNext()) {
          ARGState parentState = currentState.getParents().iterator().next();
          todo.add(parentState);
          predecessorRelation.put(currentState, parentState);
          successorRelation.put(parentState, currentState);
        }

        else if (itpRoots.size() == 0) {
          logger.log(Level.FINEST, "setting initial itp-root to " + currentState.getStateId());
          itpRoots.add(currentState);
        }
      }
    }

    private void exportItpTree(int refinementCnt, int iteration) {
      StringBuilder result = new StringBuilder().append("digraph tree {" + "\n");
      for(Map.Entry<ARGState, ARGState> current : successorRelation.entries()) {
        if(glblItps.containsKey(current.getKey())) {
          StringBuilder sb = new StringBuilder();

          sb.append("itp is " + glblItps.get(current.getKey()));

          result.append(current.getKey().getStateId() + " [label=\"" + current.getKey().getStateId() + " has itp " + (sb.toString()) + "\"]" + "\n");
          result.append(current.getKey().getStateId() + " -> " + current.getValue().getStateId() + "\n");// + " [label=\"" + current.getKey().getEdgeToChild(current.getValue()).getRawStatement().replace("\n", "") + "\"]\n");
        }

        else {
          result.append(current.getKey().getStateId() + " [label=\"" + current.getKey().getStateId() + " has itp NA\"]" + "\n");
          result.append(current.getKey().getStateId() + " -> " + current.getValue().getStateId() + "\n");// + " [label=\"" + current.getKey().getEdgeToChild(current.getValue()).getRawStatement().replace("\n", "") + "\"]\n");
        }

        if(current.getValue().isTarget()) {
          result.append(current.getValue().getStateId() + " [style=filled, fillcolor=\"red\"]" + "\n");
        }

        assert(!current.getKey().isTarget());
      }
      result.append("}");

      try {
        Files.write(result.toString(), new File("itpTree_" + refinementCnt + "_" + iteration + ".dot"), Charset.defaultCharset());
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    private boolean isDone() {
      return itpRoots.isEmpty();
    }

    private ARGPath getNextErrorPath() {
      if(isDone()) {
        return null;
      }

      ARGPath errorPath = new ARGPath();
      ARGState current = itpRoots.pop();
      logger.log(Level.FINEST, "taking new root ", current.getStateId(), " from stack");

      if(glblItps.containsKey(predecessorRelation.get(current)) && glblItps.get(predecessorRelation.get(current)).isFalse()) {
        logger.log(Level.FINEST, "itp of predecessor ", predecessorRelation.get(current).getStateId(), " is already false ... go ahead");
        return getNextErrorPath();
      }

      while(successorRelation.get(current).iterator().hasNext()) {
        Iterator<ARGState> children = successorRelation.get(current).iterator();
        ARGState child = children.next();
        errorPath.add(Pair.of(current, current.getEdgeToChild(child)));

        if(children.hasNext()) {
          if(lowestCommonAncestor == null) {
            lowestCommonAncestor = current;
          }

          ARGState sibling = children.next();
          logger.log(Level.FINEST, "\tpush new root ", sibling.getStateId(), " onto stack for parent ", predecessorRelation.get(sibling).getStateId());
          itpRoots.push(sibling);
        }

        current = child;
      }

      return errorPath;
    }

    public CFAEdge getEdgeFor(ARGState state) {
      ARGState child = successorRelation.get(state).iterator().next();

      return state.getEdgeToChild(child);
    }

    private ExplicitValueInterpolant getInitialItp(ARGState root) {
      return glblItps.get(predecessorRelation.get(root));
    }
  }


  @Override
  public boolean performRefinement(final ReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "performing global refinement ...");
    totalTime.start();
    refinementCalls++;

    lowestCommonAncestor = null;

    if(isAnyPathFeasible(new ARGReachedSet(pReached), getErrorPaths(getErrorStates(pReached)))) {
      return false;
    }
    InterpolationTree itpTree = new InterpolationTree(getErrorStates(pReached));

    glblItps = new HashMap<>();
    int i = 0;
    while(!itpTree.isDone()) {
      i++;
      ARGPath errorPath = itpTree.getNextErrorPath();

      if(errorPath == null) {
        break;
      }

      ExplicitValueInterpolant initialItp = itpTree.getInitialItp(errorPath.getFirst().getFirst());
      if(initialItp == null) {
        initialItp = ExplicitValueInterpolant.createInitial();
        assert i == 1 : "think, stupid!";
      }

      logger.log(Level.FINEST, "perform itp, starting at ", errorPath.getFirst().getFirst().getStateId(), ", using itp ", initialItp);

      if(initialItp.isFalse()) {
        logger.log(Level.FINEST, "itp is false, skipping");
      }

      else {
        Map<ARGState, ExplicitValueInterpolant> itps = interpolatingRefiner.performInterpolation(errorPath, initialItp);

        // TODO: obtain the refinement-root ... the hacky way
        if(lowestCommonAncestor == null) {
          for(Map.Entry<ARGState, ExplicitValueInterpolant> itp : itps.entrySet()) {
            if(!itp.getValue().isFalse() && !itp.getValue().isTrue()) {
              lowestCommonAncestor = itp.getKey();
              break;
            }
          }
        }
        else {
          for(Map.Entry<ARGState, ExplicitValueInterpolant> itp : itps.entrySet()) {
            if(!itp.getValue().isFalse() && !itp.getValue().isTrue() && itp.getKey().isOlderThan(lowestCommonAncestor)) {
              lowestCommonAncestor = itp.getKey();
              break;
            }
          }
        }

        glblItps.putAll(itps);

        //itpTree.exportItpTree(refinementCalls, i);
        logger.log(Level.FINEST, "itp done");
      }

      i++;
    }

    final Multimap<CFANode, MemoryLocation> globalIncrement = HashMultimap.create();
    for(Map.Entry<ARGState, ExplicitValueInterpolant> itp : glblItps.entrySet()) {

      for(MemoryLocation memloc : itp.getValue().getMemoryLocations()) {
        globalIncrement.put(itpTree.getEdgeFor(itp.getKey()).getSuccessor(), memloc);
      }
    }

    // TODO: join precisions for all target states (for all target states "below" refinement root), and refine that prec.
    final Precision precision                 = pReached.getPrecision(pReached.getLastState());
    final ExplicitPrecision originalPrecision = Precisions.extractPrecisionByType(precision, ExplicitPrecision.class);
    final ExplicitPrecision refinedPrecision  = new ExplicitPrecision(originalPrecision, globalIncrement);

    ARGReachedSet reached = new ARGReachedSet(pReached);
    if(doLazyAbstraction) {
      // TODO: non-lazy lazy-abstraction
      if(lowestCommonAncestor.getParents().isEmpty()) {
        reached.removeSubtree(((ARGState)pReached.getFirstState()).getChildren().iterator().next(), refinedPrecision, ExplicitPrecision.class);
      }
      else {
        reached.removeSubtree(lowestCommonAncestor, refinedPrecision, ExplicitPrecision.class);
      }
    } else {
      reached.removeSubtree(((ARGState)pReached.getFirstState()).getChildren().iterator().next(), refinedPrecision, ExplicitPrecision.class);
    }

    totalTime.stop();
    return true;
  }

  private boolean isAnyPathFeasible(final ARGReachedSet pReached, final Collection<ARGPath> errorPaths)
      throws CPAException, InterruptedException {

    ARGPath feasiblePath = null;
    for(ARGPath currentPath : errorPaths) {
      if(isErrorPathFeasible(currentPath)) {
        feasiblePath = currentPath;
      }
    }

    // remove all other target states, so that only one is left (for CEX-checker)
    if(feasiblePath != null) {
      for(ARGPath others : errorPaths) {
        if(others != feasiblePath) {
          pReached.removeSubtree(others.getLast().getFirst());
        }
      }
      return true;
    }

    return false;
  }

  private boolean isErrorPathFeasible(final ARGPath errorPath)
      throws CPAException, InterruptedException {
    if(checker.isFeasible(errorPath)) {
      logger.log(Level.FINEST, "found a feasible cex - returning from refinement");

      return true;
    }

    return false;
  }

  private Collection<ARGPath> getErrorPaths(final Collection<ARGState> targetStates) {
    Set<ARGPath> errorPaths = new TreeSet<>(new Comparator<ARGPath>() {
      @Override
      public int compare(ARGPath path1, ARGPath path2) {
        if(path1.size() == path2.size()) {
          return 1;
        }

        else {
          return (path1.size() < path2.size()) ? -1 : 1;
        }
      }
    });

    for(ARGState target : targetStates) {
      ARGPath p = ARGUtils.getOnePathTo(target);
      errorPaths.add(p);
    }

    return errorPaths;
  }

  private Iterator<ARGPath> getErrorPathIterator(Collection<ARGPath> errorPaths) {
    if(errorPathOrder.equals("DEFAULT")) {
      return errorPaths.iterator();
    } else {
      return new ZigZagIterator<>(Lists.newArrayList(errorPaths));
    }
  }

  private List<ARGState> getErrorStates(final ReachedSet pReached) {
    List<ARGState> targets = from(pReached)
        .transform(AbstractStates.toState(ARGState.class))
        .filter(AbstractStates.IS_TARGET_STATE)
        .toList();

    assert !targets.isEmpty();
    logger.log(Level.FINEST, "number of targets found: " + targets.size());

    return targets;
  }

  private int previousRefinementId = -1;
  private Map<ARGState, ExplicitValueInterpolant> glblItps;
  private ARGState lowestCommonAncestor;
  private boolean isRepeatedRefinementRoot(final ARGState root) {
    final int currentRefinementId = AbstractStates.extractLocation(root).getLineNumber();
    final boolean result          = (previousRefinementId == currentRefinementId);
    previousRefinementId          = currentRefinementId;

    return result && checkForRepeatedRefinements;
  }


  private ARGState getCommonRoot(Collection<ARGPath> pErrorPaths, Set<ARGState> roots, int highestItpPoint) {
    List<ARGPath> errorPaths = Lists.newArrayList(pErrorPaths);

    ARGPath shortestPath = errorPaths.get(0);

    for(int i = 0; i < shortestPath.size(); i++) {
      ARGState currentState = shortestPath.get(i).getFirst();
      if(i == highestItpPoint) {
        return shortestPath.get(highestItpPoint).getFirst();
      }
      for(int j = 0; j < errorPaths.size(); j++) {
        ARGPath currentPath = errorPaths.get(j);
        if(!currentState.equals(currentPath.get(i).getFirst())) {
          return shortestPath.get(i - 1).getFirst();
        }
      }
    }

    assert(false);
    return null;
  }

  /**
   * Do refinement for a set of target states.
   *
   * The strategy is to first build the predecessor/successor relations for all
   * abstraction states on the paths to the target states, and then call
   * {@link #performRefinementOnSubgraph(ARGState, List, SetMultimap, Map, ReachedSet, List)}
   * on the root state of the ARG.
   * @throws InterruptedException
   * @throws CPAException
   */
  private Multimap<CFANode, MemoryLocation> getIncrementForSinglePath(final ARGPath errorPath) throws CPAException, InterruptedException {
    Multimap<CFANode, MemoryLocation> increment = interpolatingRefiner.determinePrecisionIncrement(errorPath);

    return increment;
  }

  @Override
  public void collectStatistics(final Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public String getName() {
        return "ExplicitGlobalRefiner";
      }

      @Override
      public void printStatistics(final PrintStream pOut, final Result pResult, final ReachedSet pReached) {
        ExplicitGlobalRefiner.this.printStatistics(pOut, pResult, pReached);
      }
    });
  }

  private void printStatistics(final PrintStream out, final Result pResult, final ReachedSet pReached) {
    if (refinementCalls > 0) {
      out.println("Total time for global refinement:  " + totalTime);
      out.println("totalOfTargetsFound:  " + totalOfTargetsFound);
      out.println("totalOfPathsFound:  " + totalOfPathsFound);
      out.println("totalOfPathsItped:  " + totalOfPathsItped);
      out.println("skipOnIncrementalPrec:  " + skipOnIncrementalPrec);
      out.println("skipOnEmptyIncrement:  " + skipOnEmptyIncrement);
      out.println("skipOnRelevance:  " + skipOnRelevance);

      interpolatingRefiner.printStatistics(out, pResult, pReached);
    }
  }

  private class ZigZagIterator<T> implements Iterator<T> {

    private final List<T> errorPath;

    private int headIndex = 0;
    private int tailIndex = 0;
    private int currentIndex = 0;

    private boolean pop = false;

    private ZigZagIterator(List<T> pErrorPath) {
      errorPath = pErrorPath;
      tailIndex = errorPath.size() - 1;
    }

    @Override
    public boolean hasNext() {
      return headIndex <= tailIndex;
    }

    @Override
    public T next() {
      if(pop) {
        currentIndex = tailIndex;
        tailIndex--;
      }

      else {
        currentIndex = headIndex;
        headIndex++;
      }

      pop = !pop;

      return errorPath.get(currentIndex);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Removing is not supported");
    }
  }
}
