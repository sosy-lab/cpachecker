/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.ImmutableList.of;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

public class ARGToAutomatonConverter {

  public enum SplitterStrategy {
    /** do not apply splitting, but return ONE automaton for the ARG */
    NONE,
    /** split at non-nested conditions only */
    GLOBAL_CONDITIONS,
    /** split at all conditions */
    LOCAL_CONDITIONS,
    /** unroll loops // TODO just use a LoopCPA? */
    LOOPS,
  }

  private final ARGState root;
  private final SplitterStrategy strategy;

  public ARGToAutomatonConverter(ARGState pRoot, SplitterStrategy pStrategy) {
    root = pRoot;
    strategy = pStrategy;
  }

  public Iterable<Automaton> getAutomata() throws InvalidAutomatonException {
    switch (strategy) {
      case NONE:
        return Collections.singleton(getAutomatonForStates(root, of()));
      case GLOBAL_CONDITIONS:
        return getGlobalConditionSplitAutomata();
      default:
        throw new AssertionError("unexpected strategy");
    }
  }

  /** generate an automaton that traverses the subgraph, but leaves out ignoresd states. */
  private static Automaton getAutomatonForStates(ARGState pRoot, Collection<ARGState> ignoredStates)
      throws InvalidAutomatonException {

    Preconditions.checkArgument(!ignoredStates.contains(pRoot));
    Preconditions.checkArgument(!pRoot.isCovered());
    Preconditions.checkArgument(Iterables.all(ignoredStates, s -> !s.isCovered()));

    Map<String, AutomatonVariable> variables = Collections.emptyMap();

    Deque<ARGState> waitlist = new ArrayDeque<>();
    Collection<ARGState> finished = new HashSet<>();
    waitlist.add(pRoot);

    List<AutomatonInternalState> states = new ArrayList<>();
    while (!waitlist.isEmpty()) {
      ARGState s = uncover(waitlist.pop());
      if (!finished.add(s)) {
        continue;
      }
      List<AutomatonTransition> transitions = new ArrayList<>();
      List<AutomatonBoolExpr> locationQueries = new ArrayList<>();
      for (ARGState child : s.getChildren()) {
        child = uncover(child);
        if (ignoredStates.contains(child)) {
          // ignore those states here, BOTTOM-state will be inserted afterwards automatically.
          // Note: if sibling with same location is not ignored, ignorance might be useless.
          continue;
        }

        CFANode location = AbstractStates.extractLocation(child);
        AutomatonBoolExpr locationQuery =
            new AutomatonBoolExpr.CPAQuery("location", "nodenumber==" + location.getNodeNumber());
        locationQueries.add(locationQuery);
        transitions.add(new AutomatonTransition(locationQuery, of(), of(), of(), id(child)));
        waitlist.add(child);
      }

      transitions.add(
          new AutomatonTransition(
              buildOtherwise(locationQueries), of(), of(), AutomatonInternalState.BOTTOM));

      boolean hasSeveralChildren = s.getChildren().size() > 1;
      states.add(new AutomatonInternalState(id(s), transitions, false, hasSeveralChildren, false));
    }

    return new Automaton("ARG", variables, states, id(pRoot));
  }

  /** unwrap covered state if needed. */
  private static ARGState uncover(ARGState s) {
    if (s.isCovered()) {
      Preconditions.checkArgument(s.getChildren().isEmpty(), "covered state has children:", s);
      s = s.getCoveringState();
      Preconditions.checkArgument(!s.isCovered(), "covering state is covered:", s);
    }
    return s;
  }

  /**
   * build the negated conjunction of all given queries, i.e., if none of the given queries matches,
   * the returned query succeeds.
   */
  private static AutomatonBoolExpr buildOtherwise(List<AutomatonBoolExpr> locationQueries) {
    if (locationQueries.isEmpty()) {
      return AutomatonBoolExpr.TRUE;
    }
    AutomatonBoolExpr otherwise = AutomatonBoolExpr.TRUE;
    for (AutomatonBoolExpr expr : locationQueries) {
      otherwise = new AutomatonBoolExpr.And(otherwise, expr);
    }
    return new AutomatonBoolExpr.Negation(otherwise);
  }

  private static String id(ARGState s) {
    return "S" + s.getStateId() + "_N" + AbstractStates.extractLocation(s).getNodeNumber();
  }

  private Iterable<Automaton> getGlobalConditionSplitAutomata() throws InvalidAutomatonException {
    Multimap<ARGState, ARGState> dependencies = getGlobalBranchingTree(root);
    Preconditions.checkState(dependencies.isEmpty() || dependencies.containsKey(root));
    ImmutableSet<ARGState> loopStates = ImmutableSet.copyOf(getLoopStates(dependencies));

    return collectAutomaton(loopStates);
  }

  /**
   * collect all automata that start at root and ignore some branches. Split automata at the given
   * branching point if possible.
   */
  private Collection<Automaton> collectAutomaton(ImmutableSet<ARGState> loopStates)
      throws InvalidAutomatonException {
    Map<ImmutableSet<ARGState>, Automaton> automata = new LinkedHashMap<>();

    Deque<Pair<ARGState, ImmutableSet<ARGState>>> waitlist = new ArrayDeque<>();
    waitlist.add(Pair.of(root, ImmutableSet.of()));
    Multimap<ARGState, ImmutableSet<ARGState>> finished = HashMultimap.create();

    while (!waitlist.isEmpty()) {
      Pair<ARGState, ImmutableSet<ARGState>> p = waitlist.pop();
      ARGState branchingPoint = p.getFirst();
      ImmutableSet<ARGState> ignoreBranches = p.getSecond();

      if (!finished.put(branchingPoint, p.getSecond())) {
        continue;
      }

      for (ARGState child : branchingPoint.getChildren()) {
        ImmutableSet<ARGState> newIgnoreBranches;
        if (loopStates.contains(branchingPoint)) {
          // for loop-states, we cannot split the automaton.
          // for some other states we also do not apply splitting,
          newIgnoreBranches = ignoreBranches;
        } else {
          // for non-loop-states, we split the automaton
          newIgnoreBranches =
              ImmutableSet.<ARGState>builder()
                  .addAll(ignoreBranches)
                  .addAll(from(branchingPoint.getChildren()).filter(s -> s != child))
                  .build();
        }
        if (getNext(child, true).isEmpty()) {
          // no more dependencies, simply export the automaton
          if (!automata.containsKey(newIgnoreBranches)) {
            // do not export duplicates
            automata.put(newIgnoreBranches, getAutomatonForStates(root, newIgnoreBranches));
          }
        } else {
          // more dependencies for splitting subgraphs, call recursively for subgraph of child
          waitlist.add(Pair.of(child, newIgnoreBranches));
        }
      }
    }
    return automata.values();
  }

  private Multimap<ARGState, ARGState> getGlobalBranchingTree(ARGState pRoot) {
    Preconditions.checkArgument(!pRoot.isCovered());
    Multimap<ARGState, ARGState> branchingTree = HashMultimap.create();
    Deque<ARGState> waitlist = new ArrayDeque<>();
    Collection<ARGState> finished = new HashSet<>();
    waitlist.add(pRoot);
    while (!waitlist.isEmpty()) {
      ARGState s = uncover(waitlist.pop());
      if (!finished.add(s)) {
        continue;
      }
      Collection<ARGState> nextStates = getNext(s, false);
      branchingTree.putAll(s, nextStates);
      waitlist.addAll(nextStates);
    }
    return branchingTree;
  }

  /** get all branching states that part of a cycle. */
  private Iterable<ARGState> getLoopStates(Multimap<ARGState, ARGState> dependencies) {
    return from(dependencies.keySet()).filter(s -> isPartOfCycle(s, dependencies));
  }

  /**
   * check whether there is a loop containing the given state.
   *
   * <p>Actually BFS with additional check for initial element.
   */
  private boolean isPartOfCycle(ARGState pRoot, Multimap<ARGState, ARGState> dependencies) {
    Deque<ARGState> waitlist = new ArrayDeque<>();
    Collection<ARGState> finished = new HashSet<>();
    waitlist.addAll(dependencies.get(pRoot));
    while (!waitlist.isEmpty()) {
      ARGState s = waitlist.pop();
      if (!finished.add(s)) {
        continue;
      }
      if (s == pRoot) {
        return true;
      }
      waitlist.addAll(dependencies.get(s));
    }
    return false;
  }

  /**
   * return all branching nodes directly reachable from the current node, including nodes reachable
   * via branches of covered states.
   */
  private Collection<ARGState> getNext(ARGState base, boolean includeSelf) {
    base = uncover(base);
    Collection<ARGState> next = new ArrayList<>();
    Deque<ARGState> waitlist = new ArrayDeque<>();
    Collection<ARGState> finished = new HashSet<>();
    if (includeSelf) {
      waitlist.add(base);
    } else {
      waitlist.addAll(base.getChildren());
    }
    while (!waitlist.isEmpty()) {
      ARGState s = uncover(waitlist.pop());
      if (!finished.add(s)) {
        continue;
      }
      if (s.getChildren().size() > 1) {
        next.add(s);
      } else {
        waitlist.addAll(s.getChildren());
      }
    }
    return next;
  }
}
