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

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.HistoryForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class PartialARGsCombiner implements Algorithm {

  private final Algorithm restartAlgorithm;
  private final LogManager logger;
  private final ShutdownNotifier shutdown;
  private final Configuration config;


  public PartialARGsCombiner(Algorithm pAlgorithm, Configuration pConfig, LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {
    restartAlgorithm = pAlgorithm;
    logger = pLogger;
    shutdown = pShutdownNotifier;
    config = pConfig;
  }

  @Override
  public boolean run(ReachedSet pReachedSet) throws CPAException, InterruptedException,
      PredicatedAnalysisPropertyViolationException {
    checkArgument(pReachedSet instanceof ForwardingReachedSet, "PartialARGsCombiner needs ForwardingReachedSet");

    HistoryForwardingReachedSet reached = new HistoryForwardingReachedSet(pReachedSet);

    logger.log(Level.INFO, "Start inner algorithm to analyze program(s)");
    if (restartAlgorithm.run(reached)) {
      shutdown.shutdownIfNecessary();

      logger.log(Level.INFO, "Program(s) soundly analyzed, start combining ARGs.");

      Collection<ReachedSet> usedReachedSets = reached.getAllReachedSetsUsedAsDelegates();

      if (usedReachedSets.size() <= 1) {
        logger.log(Level.INFO, "Only a single ARG is considered. Do not need to combine ARGs");
        ((ForwardingReachedSet) pReachedSet).setDelegate(reached.getDelegate());
        return true;
      }

      logger.log(Level.FINE, "Extract root nodes of ARGs");
      List<ARGState> rootNodes = new ArrayList<>(usedReachedSets.size());
      for (ReachedSet usedReached : usedReachedSets) {
        checkArgument(usedReached.getFirstState() instanceof ARGState,
            "Require that all restart configurations use ARGCPA as top level CPA.");
        checkArgument(AbstractStates.extractLocation(usedReached.getFirstState()) != null,
            "Require that all restart configurations consider a location aware state");

        if (from(reached).anyMatch(IS_TARGET_STATE)) {
          logger.log(Level.INFO, "Error found, do not combine ARGs.");
          ((ForwardingReachedSet) pReachedSet).setDelegate(usedReached);
        }

        rootNodes.add((ARGState) usedReached.getFirstState());
      }

      shutdown.shutdownIfNecessary();
      ARGState root = combineARGs(rootNodes);


      logger.log(Level.INFO, "Propagate the combined ARG as analysis result");
      try {
        ((ForwardingReachedSet) pReachedSet).setDelegate(new ReachedSetFactory(config, logger).create());
      } catch (InvalidConfigurationException e) {
        logger.log(Level.SEVERE, "Creating reached set which should contain combined ARG fails.");
        return false;
      }
      // TODO need to add all ARG states?, require different precision?
      // add to reached set and delete from waitlist to prevent UNKNOWN result
      reached.add(root, SingletonPrecision.getInstance());
      reached.removeOnlyFromWaitlist(root);

    } else {
      logger.log(Level.INFO, "Program analysis is already unsound.",
          "Do not continue with combination of unsound results");
      // set reached set to last used by restart algorithm
      ((ForwardingReachedSet) pReachedSet).setDelegate(reached.getDelegate());
      return false;
    }

    return true;
  }

  private ARGState combineARGs(List<ARGState> roots) throws InterruptedException {
    Pair<Map<String, Integer>, List<AbstractState>> initStates =
        identifyCompositeStateTypesAndTheirInitialInstances(roots);

    Map<String, Integer> stateToPos = initStates.getFirst();
    List<AbstractState> initialStates = initStates.getSecond();

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

      // identify possible successor edges
      locPred = AbstractStates.extractLocation(composedState);
      for (CFAEdge succEdge : CFAUtils.allLeavingEdges(locPred)) {
        shutdown.shutdownIfNecessary();

        successorsForEdge.clear();
        edgeSuccessorIdentifier.setCFAEdge(succEdge);

        for (ARGState component : components) {
          // get the successors of ARG state for this
          edgeSuccessorIdentifier.setPredecessor(component);
          successorsForEdge.add(Lists.newArrayList(Iterables.filter(component.getChildren(), edgeSuccessorIdentifier)));
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

    return combinedRoot;
  }

  private Pair<Map<String, Integer>, List<AbstractState>>
      identifyCompositeStateTypesAndTheirInitialInstances(Collection<ARGState> rootNodes) throws InterruptedException {
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
      assert (j < i && j > 0);
      if (automataStateNames.get(j).equals(automataStateNames.get(i))) {
        if (j + numRootStates - 1 == i) {
          // automaton states commonly used
          commonAutomataStates.add(automataStateNames.get(j));
        }
      } else {
        j = i;
      }
    }

    ARGState root = rootNodes.iterator().next();

    if (root.getWrappedState() instanceof AbstractWrapperState) {
      wrapped = ((AbstractWrapperState) root.getWrappedState()).getWrappedStates();
    } else {
      wrapped = Collections.singleton(root.getWrappedState());
    }

    for (AbstractState innerWrapped : wrapped) {
      shutdown.shutdownIfNecessary();

      name = getName(innerWrapped);
      if (automataStateNames.contains(name)) {
        assert (initialState.size() == nextId);

        stateToPos.put(name, nextId);
        initialState.add(innerWrapped);
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
    return pState.getClass().getName();
  }


  private Collection<Pair<List<AbstractState>, List<ARGState>>> computeCartesianProduct(
      final List<List<ARGState>> pSuccessorsForEdge, final Map<String, Integer> pStateToPos,
      final List<AbstractState> pInitialStates) throws InterruptedException {
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
    List<ARGState> argSuccessors = new ArrayList<>(pSuccessorsForEdge.size());
    int[] indices = new int[pSuccessorsForEdge.size()];
    int nextIndex=0;
    boolean restart;
    int lastSize = pSuccessorsForEdge.get(pSuccessorsForEdge.size()-1).size();

    while(indices[indices.length-1]<lastSize){
      shutdown.shutdownIfNecessary();

      argSuccessors.clear();

      // collect ARG successors
      for (int index : indices) {
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
      final Map<String, Integer> pStateToPos, final List<AbstractState> pInitialStates) throws InterruptedException {
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
          Preconditions.checkState(innerWrapped instanceof AutomatonState,
                  "Found state which is not considered in combined composite state and which is not due to the use of an assumption automaton");
          continue;
        }
        index = pStateToPos.get(getName(innerWrapped));
        if (pInitialStates.get(index)==result.get(index)) {
          result.set(index, innerWrapped);
        } else {
            logger.log(Level.WARNING,
                    "Cannot identify the inner state which is more precise, use the earliest found. Combination may be unsound.");
          }
        }
      }
    return result;
  }

  private static class EdgeSuccessor implements Predicate<ARGState> {

    private CFAEdge edge;
    private ARGState predecessor;


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

}
