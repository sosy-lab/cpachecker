/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.pcc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.HistoryForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonStateARGCombiningHelper;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.annotation.Nullable;


public class PartialARGsCombiner implements Algorithm, StatisticsProvider {

  private final Algorithm restartAlgorithm;
  private final LogManagerWithoutDuplicates logger;
  private final ShutdownNotifier shutdown;
  private final Configuration config;
  private final AutomatonStateARGCombiningHelper automatonARGBuilderSupport;

  private final ARGCombinerStatistics stats = new ARGCombinerStatistics();


  public PartialARGsCombiner(Algorithm pAlgorithm, Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {
    restartAlgorithm = pAlgorithm;
    logger = new LogManagerWithoutDuplicates(pLogger);
    shutdown = pShutdownNotifier;
    config = pConfig;

    automatonARGBuilderSupport = new AutomatonStateARGCombiningHelper();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      CPAEnabledAnalysisPropertyViolationException {
    checkArgument(pReachedSet instanceof ForwardingReachedSet, "PartialARGsCombiner needs ForwardingReachedSet");

    HistoryForwardingReachedSet reached = new HistoryForwardingReachedSet(pReachedSet);

    logger.log(Level.INFO, "Start inner algorithm to analyze program(s)");
    AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_PRECISE;

    stats.analysisTime.start();
    try {
      status = restartAlgorithm.run(reached);
    } finally {
      stats.analysisTime.stop();
    }

    if (status.isSound()) {
      shutdown.shutdownIfNecessary();

      logger.log(Level.INFO, "Program(s) soundly analyzed, start combining ARGs.");

      stats.argCombineTime.start();
      try {
        Collection<ReachedSet> usedReachedSets = reached.getAllReachedSetsUsedAsDelegates();

        if (usedReachedSets.size() <= 1) {
          logger.log(Level.INFO, "Only a single ARG is considered. Do not need to combine ARGs");
          if (usedReachedSets.size() == 1) {
            ((ForwardingReachedSet) pReachedSet).setDelegate(reached.getDelegate());
          }
          return status;
        }

        if (from(reached.getDelegate()).anyMatch((IS_TARGET_STATE))) {
          logger.log(Level.INFO, "Error found, do not combine ARGs.");
          ((ForwardingReachedSet) pReachedSet).setDelegate(reached.getDelegate());
          return status;
        }

        logger.log(Level.FINE, "Extract root nodes of ARGs");
        List<ARGState> rootNodes = new ArrayList<>(usedReachedSets.size());
        for (ReachedSet usedReached : usedReachedSets) {
          checkArgument(usedReached.getFirstState() instanceof ARGState,
              "Require that all restart configurations use ARGCPA as top level CPA.");
          checkArgument(AbstractStates.extractLocation(usedReached.getFirstState()) != null,
              "Require that all restart configurations consider a location aware state");

          for (AbstractState errorState : from(usedReached).filter((IS_TARGET_STATE))) {
            logger.log(Level.INFO, "Error state found in reached set ", usedReached,
                "but not by last configuration. Error state must be infeasible.");
            logger.log(Level.FINE, "Remove infeasible error state", errorState);
            ((ARGState) errorState).removeFromARG();
          }

          rootNodes.add((ARGState) usedReached.getFirstState());
        }

        shutdown.shutdownIfNecessary();

        if (!combineARGs(rootNodes, (ForwardingReachedSet) pReachedSet, reached)) {
          logger.log(Level.SEVERE, "Combination of ARGs failed.");
          return status.withSound(false);
        }
      } finally {
        stats.argCombineTime.stop();
      }

      logger.log(Level.INFO, "Finished combination of ARGS");

    } else {
      logger.log(Level.INFO, "Program analysis is already unsound.",
          "Do not continue with combination of unsound results");
      // set reached set to last used by restart algorithm
      if (reached.getDelegate() != pReachedSet) {
        ((ForwardingReachedSet) pReachedSet).setDelegate(reached.getDelegate());
      }
      return status.withSound(false);
    }

    return status.withSound(true);
  }

  private boolean combineARGs(List<ARGState> roots, ForwardingReachedSet pReceivedReachedSet,
      HistoryForwardingReachedSet pForwaredReachedSet)
      throws InterruptedException, CPAException {
  Pair<Map<String, Integer>, List<AbstractState>> initStates =
      identifyCompositeStateTypesAndTheirInitialInstances(roots);

    Map<String, Integer> stateToPos = initStates.getFirst();
    List<AbstractState> initialStates = initStates.getSecond();

    try {
      pReceivedReachedSet.setDelegate(new ReachedSetFactory(config).create());
    } catch (InvalidConfigurationException e) {
      logger.log(Level.SEVERE, "Creating reached set which should contain combined ARG fails.");
      return false;
    }

    shutdown.shutdownIfNecessary();

    // combined root
    ARGState combinedRoot = new ARGState(new CompositeState(initialStates), null);

    CFANode locPred;
    ARGState composedState, composedSuccessor;
    Collection<ARGState> components;

    List<List<ARGState>> successorsForEdge = new ArrayList<>(initialStates.size());
    EdgeSuccessor edgeSuccessorIdentifier = new EdgeSuccessor();

    Map<Pair<List<AbstractState>, List<ARGState>>, ARGState> constructedCombinedStates = Maps.newHashMap();
    Deque<Pair<List<ARGState>, ARGState>> toVisit = new ArrayDeque<>();
    toVisit.add(Pair.of(roots, combinedRoot));


    // traverse through ARGs and construct combined ARG
    // assume that states in initial states are most general, represent top state (except for automaton CPAs)
    while (!toVisit.isEmpty()) {
      shutdown.shutdownIfNecessary();

      components = toVisit.peek().getFirst();
      composedState = toVisit.poll().getSecond();

      // add composed state to reached set
      pReceivedReachedSet.add(composedState, SingletonPrecision.getInstance());
      pReceivedReachedSet.removeOnlyFromWaitlist(composedState);

      // identify possible successor edges
      locPred = AbstractStates.extractLocation(composedState);
      nextEdge: for (CFAEdge succEdge : CFAUtils.allLeavingEdges(locPred)) {
        shutdown.shutdownIfNecessary();

        successorsForEdge.clear();
        edgeSuccessorIdentifier.setCFAEdge(succEdge);

        for (ARGState component : components) {
          // get the successors of ARG state for this edge succEdge
          edgeSuccessorIdentifier.setPredecessor(component);
          successorsForEdge.add(Lists.newArrayList(Iterables.filter(component.getChildren(), edgeSuccessorIdentifier)));
          // check if stopped because no concrete successors exists, then do not
          if (successorsForEdge.get(successorsForEdge.size() - 1).isEmpty()
              && noConcreteSuccessorExist(component, succEdge, pForwaredReachedSet)) {
            continue nextEdge;
          }
        }

        // construct successors for each identified combination
        for (Pair<List<AbstractState>, List<ARGState>> combinedSuccessor
                                            : computeCartesianProduct(successorsForEdge, stateToPos, initialStates)) {
          if (constructedCombinedStates.containsKey(combinedSuccessor)) {
            // handle coverage
            constructedCombinedStates.get(combinedSuccessor).addParent(composedState);
          } else {
            // construct and register composed successor
            composedSuccessor = new ARGState(new CompositeState(combinedSuccessor.getFirst()), composedState);
            constructedCombinedStates.put(combinedSuccessor, composedSuccessor);

            // add successor for further exploration
            toVisit.add(Pair.of(combinedSuccessor.getSecond(), composedSuccessor));
          }
        }
      }
    }
    return true;
  }

  private boolean noConcreteSuccessorExist(final ARGState pPredecessor, final CFAEdge pSuccEdge,
      HistoryForwardingReachedSet pForwaredReachedSet) {
    // check if analysis stopped exploration due e.g. time limit
    for(ReachedSet reached :pForwaredReachedSet.getAllReachedSetsUsedAsDelegates()) {
      if(reached.getWaitlist().contains(pPredecessor)) {
        return false;
      }
    }
  // check if analysis stopped exploration due to true state in automaton --> concrete successors may exist
    for (AbstractState state : AbstractStates.asIterable(pPredecessor)) {
      if (state instanceof AutomatonState
          && ((AutomatonState) state).getOwningAutomatonName().equals("AssumptionAutomaton")) {
        if (AutomatonStateARGCombiningHelper.endsInAssumptionTrueState((AutomatonState) state, pSuccEdge)) {
          return false;
        }
      }
    }
    return true;
  }

  private Pair<Map<String, Integer>, List<AbstractState>>
      identifyCompositeStateTypesAndTheirInitialInstances(Collection<ARGState> rootNodes)
  throws InterruptedException, CPAException {
   logger.log(Level.FINE, "Derive composite state structure of combined ARG");

    List<AbstractState> initialState = new ArrayList<>();
    Map<String, Integer> stateToPos = new HashMap<>();
    List<String> automataStateNames = new ArrayList<>();

    String name;
    int nextId = 0;
    Iterable<AbstractState> wrapped;

    logger.log(Level.FINE, "Add non-automaton states");
    for (ARGState root : rootNodes) {
      shutdown.shutdownIfNecessary();

      wrapped = getWrappedStates(root);

      for (AbstractState innerWrapped : wrapped) {
        shutdown.shutdownIfNecessary();

        if(innerWrapped instanceof AssumptionStorageState) {
          continue;
        }

        name = getName(innerWrapped);

        if (stateToPos.containsKey(name)) {
          if (!initialState.get(stateToPos.get(name)).equals(innerWrapped)) {
            logger.log(Level.WARNING, "Abstract state ", innerWrapped.getClass(),
                    " is used by multiple configurations, but cannot check that always start in the same initial state as it is assumed");
          }
        } else {
          assert (initialState.size() == nextId);

          if (innerWrapped instanceof AutomatonState) {
            automataStateNames.add(name);
          } else {
            stateToPos.put(name, nextId);
            initialState.add(innerWrapped);
            nextId++;
          }
        }
      }
    }

    logger.log(Level.FINE, "Add automaton states related to specification");
    Collections.sort(automataStateNames);

    int numRootStates = rootNodes.size();
    Set<String> commonAutomataStates = new TreeSet<>();
    for (int i = 1, j = 0; i < automataStateNames.size(); i++) {
      assert (j < i && j >= 0);
      if (automataStateNames.get(j).equals(automataStateNames.get(i))) {
        if (j + numRootStates - 1 == i ) {
          // automaton states commonly used
          commonAutomataStates.add(automataStateNames.get(j));
        }
      } else {
        j = i;
      }
    }

    // assume root is the root node of the first ARG constructed
    ARGState root = rootNodes.iterator().next();

    if (root.getWrappedState() instanceof AbstractWrapperState) {
      wrapped = ((AbstractWrapperState) root.getWrappedState()).getWrappedStates();
    } else {
      wrapped = Collections.singleton(root.getWrappedState());
    }

    for (AbstractState innerWrapped : wrapped) {
      shutdown.shutdownIfNecessary();

      name = getName(innerWrapped);
      if (commonAutomataStates.contains(name)) {
        assert (initialState.size() == nextId);

        stateToPos.put(name, nextId);
        if (!automatonARGBuilderSupport.registerAutomaton((AutomatonState) innerWrapped)) {
          logger.log(Level.SEVERE, "Property specification, given by automata specification, is ambigous.");
          throw new CPAException(
              "Ambigious property specification,  automata specification contains automata with same name or same state names");
        }
        initialState.add(automatonARGBuilderSupport.replaceStateByStateInAutomatonOfSameInstance((AutomatonState) innerWrapped));
        nextId++;
      }
    }

    return Pair.of(stateToPos, initialState);
  }

  private Iterable<AbstractState> getWrappedStates(ARGState wrapper) {
    if (wrapper.getWrappedState() instanceof AbstractWrapperState) {
      return ((AbstractWrapperState) wrapper.getWrappedState()).getWrappedStates();
    } else {
      return Collections.singleton(wrapper.getWrappedState());
    }
  }

  private String getName(AbstractState pState) {
    if (pState instanceof AutomatonState) { return ((AutomatonState) pState).getOwningAutomatonName(); }
    if (pState instanceof PredicateAbstractState) { return PredicateAbstractState.class.getName(); }
    return pState.getClass().getName();
  }


  private Collection<Pair<List<AbstractState>, List<ARGState>>> computeCartesianProduct(
      final List<List<ARGState>> pSuccessorsForEdge, final Map<String, Integer> pStateToPos,
      final List<AbstractState> pInitialStates) throws InterruptedException, CPAException {
    // compute number of successors
    int count = 0;
    for (List<ARGState> successor : pSuccessorsForEdge) {
      if (successor.size() > 0) {
        count = count == 0 ? successor.size() : count * successor.size();
      }
    }

    // no successor in every of the ARGs
    if (count == 0) {
      return Collections.emptySet();
    }

    Collection<Pair<List<AbstractState>, List<ARGState>>> result = new ArrayList<>(count);

    // compute cartesian product
    int[] indices = new int[pSuccessorsForEdge.size()];
    int nextIndex=0;
    boolean restart;
    int lastSize = pSuccessorsForEdge.get(pSuccessorsForEdge.size()-1).size();

    if (lastSize == 0) {
      lastSize = 1;
    }

    while(indices[indices.length-1]<lastSize){
      shutdown.shutdownIfNecessary();

      final List<ARGState> argSuccessors = new ArrayList<>(pSuccessorsForEdge.size());

      // collect ARG successors
      for (int index = 0; index < indices.length; index++) {
        if (pSuccessorsForEdge.get(index).size() > 0) {
          argSuccessors.add(getUncoveredSuccessor(pSuccessorsForEdge.get(index).get(indices[index])));
        }
      }

      // combine ARG states to get one cartesian product element, assume top state if no explicit state information available
      result.add(Pair.of(combineARGStates(argSuccessors, pStateToPos, pInitialStates), argSuccessors));

      // compute indices for elements of next cartesian element
      indices[nextIndex]++;
      restart = false;
      while (indices[nextIndex] >= pSuccessorsForEdge.get(nextIndex).size() && nextIndex < indices.length - 1) {
        nextIndex++;
        indices[nextIndex]++;
        restart = true;
      }

      while(restart && nextIndex>0){
        indices[--nextIndex]=0;
      }
    }

    return result;
  }

  private ARGState getUncoveredSuccessor(ARGState pMaybeCovered) {
    while (pMaybeCovered.isCovered()) {
      pMaybeCovered = pMaybeCovered.getCoveringState();
    }
    return pMaybeCovered;
  }

  private List<AbstractState> combineARGStates(final List<ARGState> combiningStates,
      final Map<String, Integer> pStateToPos, final List<AbstractState> pInitialStates)
      throws InterruptedException, CPAException {
    // set every state to the top state (except for automaton states) in case we have no concrete information
    List<AbstractState> result = new ArrayList<>(pInitialStates);

    Iterable<AbstractState> wrapped;
    int index;

    // replace top by more concrete information found by exploration (saved in ARGStates)
    for (ARGState combiner : combiningStates) {
      shutdown.shutdownIfNecessary();

      wrapped = getWrappedStates(combiner);
      for (AbstractState innerWrapped : wrapped) {
        shutdown.shutdownIfNecessary();

        if (!pStateToPos.containsKey(getName(innerWrapped))) {
          Preconditions.checkState(innerWrapped instanceof AutomatonState
              || innerWrapped instanceof AssumptionStorageState,
                  "Found state which is not considered in combined composite state and which is not due to the use of an assumption automaton");
          continue;
        }
        index = pStateToPos.get(getName(innerWrapped));
        if (pInitialStates.get(index)==result.get(index)) {
          if (result.get(index) instanceof AutomatonState) {
            result.set(index,
                   automatonARGBuilderSupport.replaceStateByStateInAutomatonOfSameInstance((AutomatonState) innerWrapped));
          } else {
            result.set(index, innerWrapped);
          }
        } else {
            logger.logOnce(Level.WARNING,
                    "Cannot identify the inner state which is more precise, use the earliest found. Combination may be unsound.");
          }
        }
      }
    return result;
  }

  private static class EdgeSuccessor implements Predicate<ARGState> {

    private @Nullable CFAEdge edge;
    private @Nullable ARGState predecessor;


    @Override
    public boolean apply(ARGState pChild) {
      return pChild != null && predecessor.getEdgeToChild(pChild) == edge;
    }

    private void setCFAEdge(final CFAEdge pEdge) {
      edge = pEdge;
    }

    private void setPredecessor(ARGState pPred) {
      predecessor = pPred;
    }
  }

  private static class ARGCombinerStatistics implements Statistics {

    private final Timer argCombineTime = new Timer();
    private final Timer analysisTime = new Timer();


    @Override
    public void printStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
      out.println("Time for program analyis: " + analysisTime);
      out.println("Time to combine ARGs:     " + argCombineTime);

    }

    @Override
    public @Nullable
    String getName() {
      return "ARG Combiner Statistics";
    }

  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (restartAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)restartAlgorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }

}
