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
package org.sosy_lab.cpachecker.cpa.bam;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.toPercent;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.FileSystemPath;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGToDotWriter;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;

/**
 * Prints some BAM related statistics
 */
@Options(prefix="cpa.bam")
class BAMCPAStatistics implements Statistics {

  @Option(description="export blocked ARG as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path argFile = Paths.get("BlockedARG.dot");

  @Option(description="export single blocked ARG as .dot files, should contain '%d'")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path indexedArgFile = Paths.get("ARGs/ARG_%d.dot");

  @Option(description="export used parts of blocked ARG as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path simplifiedArgFile = Paths.get("BlockedARGSimplified.dot");

  private final Predicate<Pair<ARGState,ARGState>> highlightSummaryEdge = new Predicate<Pair<ARGState, ARGState>>() {
    @Override
    public boolean apply(Pair<ARGState, ARGState> input) {
      final CFAEdge edge = input.getFirst().getEdgeToChild(input.getSecond());
      return edge instanceof FunctionSummaryEdge;
    }
  };

  private final BAMCPA cpa;
  private final BAMCache cache;
  private AbstractBAMBasedRefiner refiner = null;

  public BAMCPAStatistics(BAMCPA cpa, BAMCache cache, Configuration config)
          throws InvalidConfigurationException {
    config.inject(this);

    this.cpa = cpa;
    this.cache = cache;
  }

  @Override
  public String getName() {
    return "BAMCPA";
  }

  public void addRefiner(AbstractBAMBasedRefiner pRefiner) {
    checkState(refiner == null);
    refiner = pRefiner;
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {

    BAMTransferRelation transferRelation = cpa.getTransferRelation();
    TimedReducer reducer = cpa.getReducer();

    int sumCalls = cache.cacheMisses + cache.partialCacheHits + cache.fullCacheHits;

    int sumARTElemets = 0;
    for (ReachedSet subreached : BAMARGUtils.gatherReachedSets(cpa, reached).values()) {
      sumARTElemets += subreached.size();
    }

    out.println("Total size of all ARGs:                                         " + sumARTElemets);
    out.println("Maximum block depth:                                            " + transferRelation.maxRecursiveDepth);
    out.println("Total number of recursive CPA calls:                            " + sumCalls);
    out.println("  Number of cache misses:                                       " + cache.cacheMisses + " (" + toPercent(cache.cacheMisses, sumCalls) + " of all calls)");
    out.println("  Number of partial cache hits:                                 " + cache.partialCacheHits + " (" + toPercent(cache.partialCacheHits, sumCalls) + " of all calls)");
    out.println("  Number of full cache hits:                                    " + cache.fullCacheHits + " (" + toPercent(cache.fullCacheHits, sumCalls) + " of all calls)");
    if (cache.gatherCacheMissStatistics) {
      out.println("Cause for cache misses:                                         ");
      out.println("  Number of abstraction caused misses:                          " + cache.abstractionCausedMisses + " (" + toPercent(cache.abstractionCausedMisses, cache.cacheMisses) + " of all misses)");
      out.println("  Number of precision caused misses:                            " + cache.precisionCausedMisses + " (" + toPercent(cache.precisionCausedMisses, cache.cacheMisses) + " of all misses)");
      out.println("  Number of misses with no similar elements:                    " + cache.noSimilarCausedMisses + " (" + toPercent(cache.noSimilarCausedMisses, cache.cacheMisses) + " of all misses)");
    }
    out.println("Time for reducing abstract states:                            " + reducer.reduceTime + " (Calls: " + reducer.reduceTime.getNumberOfIntervals() + ")");
    out.println("Time for expanding abstract states:                           " + reducer.expandTime + " (Calls: " + reducer.expandTime.getNumberOfIntervals() + ")");
    out.println("Time for checking equality of abstract states:                " + cache.equalsTimer + " (Calls: " + cache.equalsTimer.getNumberOfIntervals() + ")");
    out.println("Time for computing the hashCode of abstract states:           " + cache.hashingTimer + " (Calls: " + cache.hashingTimer.getNumberOfIntervals() + ")");
    out.println("Time for searching for similar cache entries:                   " + cache.searchingTimer + " (Calls: " + cache.searchingTimer.getNumberOfIntervals() + ")");
    out.println("Time for reducing precisions:                                   " + reducer.reducePrecisionTime + " (Calls: " + reducer.reducePrecisionTime.getNumberOfIntervals() + ")");
    out.println("Time for expanding precisions:                                  " + reducer.expandPrecisionTime + " (Calls: " + reducer.expandPrecisionTime.getNumberOfIntervals() + ")");

    out.println("Time for removing cached subtrees for refinement:               " + transferRelation.removeCachedSubtreeTimer);
    out.println("Time for recomputing ARGs during counterexample analysis:       " + transferRelation.recomputeARTTimer);
    if (refiner != null) {
      out.println("Compute path for refinement:                                    " + refiner.computePathTimer);
      out.println("  Constructing flat ARG:                                        " + refiner.computeSubtreeTimer);
      out.println("  Searching path to error location:                             " + refiner.computeCounterexampleTimer);
    }

    //Add to reached set all states from BAM cache
    Collection<ReachedSet> cachedStates = cache.getAllCachedReachedStates();
    for (ReachedSet set : cachedStates) {
      for (AbstractState state : set.asCollection()) {
        /* Method 'add' add state not only in list of reached states, but also in waitlist,
         * so we should delete it.
         */
        reached.add(state, set.getPrecision(state));
        reached.removeOnlyFromWaitlist(state);
      }
    }

    exportAllReachedSets(reached);
    exportLatestReachedSets(reached);
  }

  private void exportAllReachedSets(final ReachedSet mainReachedSet) {

    if (argFile != null) {

      final Set<ReachedSet> allReachedSets = new HashSet<>(cache.getAllCachedReachedStates());
      allReachedSets.add(mainReachedSet);

      final Set<ARGState> rootStates = new HashSet<>();
      final Multimap<ARGState, ARGState> connections = HashMultimap.create();

      for (final ReachedSet reachedSet : allReachedSets) {
        ARGState rootState = (ARGState) reachedSet.getFirstState();
        rootStates.add(rootState);
        Multimap<ARGState, ARGState> localConnections = HashMultimap.create();
        getConnections(rootState, localConnections);
        connections.putAll(localConnections);

        // dump small graph
        Path file = new FileSystemPath(String.format(
                indexedArgFile.getPath(), ((ARGState) reachedSet.getFirstState()).getStateId()));
        try (Writer w = Files.openOutputFile(file)) {
          ARGToDotWriter.write(w,
                  Collections.singleton((ARGState) reachedSet.getFirstState()),
                  localConnections,
                  ARGUtils.CHILDREN_OF_STATE,
                  Predicates.alwaysTrue(),
                  highlightSummaryEdge);
        } catch (IOException e) {
          // ignore, TODO write message for user
        }
      }

      // dump super-graph
      try (Writer w = Files.openOutputFile(argFile)) {
        ARGToDotWriter.write(w,
                rootStates,
                connections,
                ARGUtils.CHILDREN_OF_STATE,
                Predicates.alwaysTrue(),
                highlightSummaryEdge);
      } catch (IOException e) {
        // ignore, TODO write message for user
      }
    }
  }

  /** dump only those ReachedSets, that are reachable from mainReachedSet. */
  private void exportLatestReachedSets(final ReachedSet mainReachedSet) {

    if (simplifiedArgFile != null) {

      final Multimap<ARGState, ARGState> connections = HashMultimap.create();
      final Set<ReachedSet> finished = new HashSet<>();
      final Deque<ReachedSet> waitlist = new ArrayDeque<>();
      waitlist.add(mainReachedSet);
      while (!waitlist.isEmpty()){
        ReachedSet reachedSet = waitlist.pop();
        if (!finished.add(reachedSet)) {
          continue;
        }
        ARGState rootState = (ARGState) reachedSet.getFirstState();
        Set<ReachedSet> referencedReachedSets = getConnections(rootState, connections);
        waitlist.addAll(referencedReachedSets);
      }

      final Set<ARGState> rootStates = new HashSet<>();
      for (ReachedSet reachedSet : finished) {
        rootStates.add((ARGState)reachedSet.getFirstState());
      }

      try (Writer w = Files.openOutputFile(simplifiedArgFile)) {
        ARGToDotWriter.write(w,
                rootStates,
                connections,
                ARGUtils.CHILDREN_OF_STATE,
                Predicates.alwaysTrue(),
                highlightSummaryEdge);
      } catch (IOException e) {
        // ignore, TODO write message for user
      }
    }
  }

  private Set<ReachedSet> getConnections(final ARGState rootState, final Multimap<ARGState, ARGState> connections) {
    final Set<ReachedSet> referencedReachedSets = new HashSet<>();
    final Set<ARGState> finished = new HashSet<>();
    final Deque<ARGState> waitlist = new ArrayDeque<>();
    waitlist.add(rootState);
    while (!waitlist.isEmpty()) {
      ARGState state = waitlist.pop();
      if (!finished.add(state)) {
        continue;
      }
      if (cpa.getTransferRelation().abstractStateToReachedSet.containsKey(state)) {
        ReachedSet target = cpa.getTransferRelation().abstractStateToReachedSet.get(state);
        referencedReachedSets.add(target);
        ARGState targetState = (ARGState) target.getFirstState();
        connections.put(state, targetState);
      }
      if (cpa.getTransferRelation().expandedToReducedCache.containsKey(state)) {
        AbstractState sourceState = cpa.getTransferRelation().expandedToReducedCache.get(state);
        connections.put((ARGState) sourceState, state);
      }
      waitlist.addAll(state.getChildren());
    }
    return referencedReachedSets;
  }
}
