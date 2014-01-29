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

import java.io.PrintStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
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
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ExplictFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

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
  }

  public static ARGState explicitTarget = null;

  @Override
  public boolean performRefinement(final ReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "performing global refinement ...");

    totalTime.start();
    refinementCalls++;

    final Collection<ARGState> errorStates  = getErrorStates(pReached);
    final Collection<ARGPath> errorPaths    = getErrorPaths(errorStates);

    totalOfTargetsFound += errorStates.size();
    totalOfPathsFound += errorPaths.size();

    try {
      final Multimap<CFANode, MemoryLocation> globalIncrement = HashMultimap.create();
      ARGState globalRoot = null;
      Set<ARGState> roots = new HashSet<>();
      Set<CFAEdge> interpolatedEdges = new HashSet<>();

      // perform refinement, potentially for each error path
      Iterator<ARGPath> erroPathIterator = getErrorPathIterator(errorPaths);
      while(erroPathIterator.hasNext()) {
        shutdownNotifier.shutdownIfNecessary();
        final ARGPath errorPath = erroPathIterator.next();

        if(isErrorPathFeasible(errorPath)) {
          explicitTarget = errorPath.getLast().getFirst();
          return false;
        }

        if(checkOnIncrementalPrec) {
          boolean isFeasible = checker.isFeasible(errorPath);
          final Precision tempPrecision                 = pReached.getPrecision(pReached.getLastState());
          final ExplicitPrecision tempOriginalPrecision = Precisions.extractPrecisionByType(tempPrecision, ExplicitPrecision.class);
          isFeasible = checker.isFeasible(errorPath, new ExplicitPrecision(tempOriginalPrecision, globalIncrement));
          if(!isFeasible) {
            logger.log(Level.FINEST, "checking path with running, refined precision revealed that is is already infeasible");
            skipOnIncrementalPrec++;
            continue;
          }
        }


        if(checkOnRelevance) {
          // todo: already filter out "old"/interpolated ones here
          Set<CFAEdge> edgesOfPath = Sets.newHashSet(from(errorPath).transform(Pair.<CFAEdge>getProjectionToSecond()));
          double size1 = edgesOfPath.size();

          edgesOfPath.removeAll(interpolatedEdges);
          double size2 = edgesOfPath.size();

          double relevance = Math.round((size2 / size1) * 100);

          interpolatedEdges.addAll(edgesOfPath);

          if(relevance < 10) {
            logger.log(Level.FINEST, "error path found to be not relevant based on already-interpolated-CFA-edge heuristic [relevance < " + relevance + "%]");
            skipOnRelevance++;
            continue;
          }
        }

        totalOfPathsItped++;
        final Multimap<CFANode, MemoryLocation> increment = getIncrementForSinglePath(errorPath);
        final ARGState currentRoot = interpolatingRefiner.determineRefinementRoot(errorPath, increment, false).getFirst();
        roots.add(currentRoot);

        globalRoot = getRoot(globalRoot, currentRoot, errorPath);
        boolean globalIncrementIncreased = globalIncrement.putAll(increment);

        if(checkOnEmptyIncrement && !globalIncrementIncreased) {
          logger.log(Level.FINEST, "no increment obtained during this interpolation - stoping futher interpolations");
          skipOnEmptyIncrement++;
          break;
        }
      }
      logger.log(Level.FINEST, "obtained the following increment: ", new TreeSet<>(globalIncrement.values()));

      if(globalIncrement.isEmpty()) {
        logger.log(Level.FINEST, "no global increment obtained - returning from refinement");
        return false;
      }

      final Precision precision                 = pReached.getPrecision(pReached.getLastState());
      final ExplicitPrecision originalPrecision = Precisions.extractPrecisionByType(precision, ExplicitPrecision.class);
      final ExplicitPrecision refinedPrecision  = new ExplicitPrecision(originalPrecision, globalIncrement);

      final ARGReachedSet reached               = new ARGReachedSet(pReached, argCpa);

      globalRoot = getCommonRoot(errorPaths, roots);

      /* selection of refinement root for lazy abstraction needs some more thinking */
      if(isRepeatedRefinementRoot(globalRoot)) {
        //logger.log(Level.FINEST, "!!! REPEATED -> NON-LAZY !!! ");
      }

      if(doLazyAbstraction && !isRepeatedRefinementRoot(globalRoot)) {
        reached.removeSubtree(globalRoot, refinedPrecision, ExplicitPrecision.class);

        /* works for lazy abstraction
        for(ARGState refinementRoot : roots) {
          if(!refinementRoot.isDestroyed()) {
            reached.removeSubtree(refinementRoot, refinedPrecision, ExplicitPrecision.class);
          }
        }
         */
      } else {
        // remove the first child of the root node
        reached.removeSubtree(Iterables.getFirst(errorPaths, null).get(1).getFirst(), refinedPrecision, ExplicitPrecision.class);
      }

      logger.log(Level.FINEST, (from(pReached).filter(AbstractStates.IS_TARGET_STATE).size()), " target states remain in reached set");

      return true;

    } finally {
      totalTime.stop();
    }
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
          return 0;
        }

        else {
          return (path1.size() < path2.size()) ? -1 : 1;
        }
      }
    });

    for(ARGState target : targetStates) {
      errorPaths.add(ARGUtils.getOnePathTo(target));
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
        .toList()
      // sorting the collection might be a good idea, just to guarantee,
      // that the same program shows the same behavior on reverification
      // this should already be sorted, but just to be safe
      /*
      .toSortedList(new Comparator<AbstractState>() {
        @Override
        public int compare(final AbstractState o1, final AbstractState o2) {
          return ((ARGState)o1).compareTo(((ARGState)o2));
        }
      })*/;

    assert !targets.isEmpty();
    logger.log(Level.FINEST, "number of targets found: " + targets.size());

    return targets;
  }

  private int previousRefinementId = -1;
  private boolean isRepeatedRefinementRoot(final ARGState root) {
    final int currentRefinementId = AbstractStates.extractLocation(root).getLineNumber();
    final boolean result          = (previousRefinementId == currentRefinementId);
    previousRefinementId          = currentRefinementId;

    return result && checkForRepeatedRefinements;
  }


  private ARGState getCommonRoot(Collection<ARGPath> pErrorPaths, Set<ARGState> roots) {
    boolean allMatch = true;
    int offset = 0;

    List<ARGPath> errorPaths = Lists.newArrayList(pErrorPaths);

    while(true) {

      if(offset >= errorPaths.get(0).size()) {
        logger.log(Level.FINEST, "PICKED INITIAL ELEMENT - maybe we screwed up badly !?!?!");
        return errorPaths.get(0).get(1).getFirst();
      }

      ARGState toBeMatched = errorPaths.get(0).get(offset).getFirst();
      for(int i = 1; i < errorPaths.size(); i++) {

        if(offset >= errorPaths.get(i).size()) {
          logger.log(Level.FINEST, "PICKED INITIAL ELEMENT - maybe we screwed up worse than badly !?!?!");
          return errorPaths.get(0).get(1).getFirst();
        }

        ARGState matchable = errorPaths.get(i).get(offset).getFirst();
        allMatch = allMatch && toBeMatched.equals(matchable);

        if(roots.contains(matchable)) {
          return matchable;
        }
      }

      if(!allMatch) {
        break;
      }

      offset++;
    }

    logger.log(Level.FINEST, "using common root at index " + (offset - 1));
    return errorPaths.get(0).get(offset - 1).getFirst();
  }

  private ARGState getRoot(final ARGState globalRoot, final ARGState currentRoot, final ARGPath currentPath) {
    // initial case, if no root was set yet
    if(globalRoot == null) {
      return currentRoot;
    }

    if(currentRoot.isOlderThan(globalRoot)) {
      return globalRoot;
    } else {
      return currentRoot;
    }
/*
    // otherwise, pick the higher node, i.e., either previous global root, or the current root
    for(final Pair<ARGState, CFAEdge> currentElement : currentPath) {
      final ARGState currentState = currentElement.getFirst();

      if(currentState == globalRoot) {
        return globalRoot;
      } else if (currentState == currentRoot) {
        return currentRoot;
      }
    }

    assert(false);

    return null;*/
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
