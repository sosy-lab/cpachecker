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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * This class converts an ARG into an automaton (or several automata), that can be used as
 * specification for a second execution of CPAchecker. The automata represent disjoint partitions of
 * the state space, i.e., the analysis of each one of them returns a result, and merging all those
 * results is sound for the whole program. We currently provide different strategies for splitting
 * the state space.
 *
 * <p>The idea is based on the paper "Structurally Defined Conditional Data-Flow Static Analysis"
 * from Elena Sherman and Matthew B. Dwyer.
 */
@Options(prefix = "cpa.arg.automaton")
public class ARGToAutomatonConverter {

  public enum SplitterStrategy {
    /** do not apply splitting, but return ONE automaton for the ARG */
    NONE,
    /** split at non-nested conditions only */
    GLOBAL_CONDITIONS,
  }

  public enum BranchExportStrategy {
    /** export no branches */
    NONE,
    /** export all branches, will contain redundant paths, mostly for debugging. */
    ALL,
    /** export all leaf nodes of the ARG, very precise, no redundant paths are exported. */
    LEAFS,
    /** export some intermediate nodes, sound due to export of siblings if needed. */
    WEIGHTED,
    /**
     * export top nodes according to BFS, but skip the first N internal nodes, e.g. export the
     * frontier nodes after analyzing N nodes.
     */
    FIRST_BFS
  }

  @Option(
      secure = true,
      description = "which coarse strategy should be applied when analyzing the ARG?")
  private SplitterStrategy strategy = SplitterStrategy.GLOBAL_CONDITIONS;

  @Option(
      secure = true,
      description = "after determining branches, which one of them should be exported?")
  private BranchExportStrategy selectionStrategy = BranchExportStrategy.LEAFS;

  @Option(
      secure = true,
      description = "minimum ratio of branch compared to whole program to be exported")
  private double branchRatio = 0.5;

  @Option(
      secure = true,
      description = "minimum ratio of siblings such that one of them will be exported")
  private double siblingRatio = 0.4;

  @Option(
      secure = true,
      description =
          "when using FIRST_BFS, how many nodes should be skipped? "
              + "ZERO will only export the root itself, MAX_INT will export only LEAFS.")
  private int skipFirstNum = 10;

  public ARGToAutomatonConverter(@Nullable Configuration config)
      throws InvalidConfigurationException {
    if (config == null) {
      strategy = SplitterStrategy.NONE;
      selectionStrategy = BranchExportStrategy.NONE;
    } else {
      config.inject(this);
    }
  }

  public Iterable<Automaton> getAutomata(ARGState root) {
    switch (strategy) {
      case NONE:
        return Collections.singleton(getAutomatonForStates(root, of()));
      case GLOBAL_CONDITIONS:
        return getGlobalConditionSplitAutomata(root, selectionStrategy);
      default:
        throw new AssertionError("unexpected strategy");
    }
  }

  /** generate an automaton that traverses the subgraph, but leaves out ignored states. */
  private static Automaton getAutomatonForStates(
      ARGState pRoot, Collection<ARGState> ignoredStates) {
    try {
      return getAutomatonForStates0(pRoot, ignoredStates);
    } catch (InvalidAutomatonException e) {
      throw new AssertionError("unexpected exception", e);
    }
  }

  /** generate an automaton that traverses the subgraph, but leaves out ignored states. */
  private static Automaton getAutomatonForStates0(ARGState root, Collection<ARGState> ignoredStates)
      throws InvalidAutomatonException {

    Preconditions.checkArgument(!ignoredStates.contains(root));
    Preconditions.checkArgument(!root.isCovered());
    Preconditions.checkArgument(Iterables.all(ignoredStates, s -> !s.isCovered()));

    Map<String, AutomatonVariable> variables = Collections.emptyMap();

    Deque<ARGState> waitlist = new ArrayDeque<>();
    Collection<ARGState> finished = new HashSet<>();
    waitlist.add(root);

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

    return new Automaton("ARG", variables, states, id(root));
  }

  /** unwrap covered state if needed. */
  private static ARGState uncover(ARGState s) {
    while (s.isCovered()) {
      s = s.getCoveringState();
    }
    return s;
  }

  private static Iterable<ARGState> uncover(Iterable<ARGState> states) {
    return Iterables.transform(states, ARGToAutomatonConverter::uncover);
  }

  /**
   * build the negated conjunction of all given queries, i.e., if none of the given queries matches,
   * the returned query succeeds.
   */
  private static AutomatonBoolExpr buildOtherwise(List<AutomatonBoolExpr> locationQueries) {
    if (locationQueries.isEmpty()) {
      return AutomatonBoolExpr.TRUE;
    }
    AutomatonBoolExpr otherwise = locationQueries.get(0);
    for (AutomatonBoolExpr expr : Iterables.skip(locationQueries, 1)) {
      otherwise = new AutomatonBoolExpr.And(otherwise, expr);
    }
    return new AutomatonBoolExpr.Negation(otherwise);
  }

  private Iterable<Automaton> getGlobalConditionSplitAutomata(
      ARGState root, BranchExportStrategy branchExportStrategy) {
    Map<ARGState, BranchingInfo> dependencies = getGlobalBranchingTree(root);
    Preconditions.checkState(dependencies.isEmpty() || dependencies.containsKey(root));

    // logger.log(Level.INFO, toDot(dependencies));

    Map<ARGState, BranchingInfo> loopFreeDependencies = getFilteredDependencies(root, dependencies);
    Preconditions.checkState(
        loopFreeDependencies.isEmpty() || loopFreeDependencies.containsKey(root));

    // logger.log(Level.INFO, toDot(loopFreeDependencies));

    // add info about cut-off branches to each node
    addBranchInformation(root, loopFreeDependencies);

    // having parent relation is nice to have
    addParents(loopFreeDependencies);

    return collectAutomata(root, loopFreeDependencies, branchExportStrategy);
  }

  /**
   * Given a tree (DAG) of dependencies, we can create several automata, such that (starting from
   * root-node)
   *
   * <ul>
   *   <li>either a node is exported or
   *   <li>if there are children, the same rule applies for all of its children.
   * </ul>
   */
  private Iterable<Automaton> collectAutomata(
      ARGState root, Map<ARGState, BranchingInfo> pDependencies, BranchExportStrategy export) {

    switch (export) {
      case NONE:
        return Collections.emptyList();

      case ALL: // export all nodes, mainly for debugging.
        return FluentIterable.from(pDependencies.entrySet())
            .transformAndConcat(entry -> entry.getValue().getIgnoreStates())
            .transform(ignores -> getAutomatonForStates(root, ignores.asSet()));

      case LEAFS: // ALL_PATHS, export all leaf-nodes, sub-graphs cover the whole graph.
        // no redundant paths expected, if leafs are reached via different paths.
        return FluentIterable.from(pDependencies.entrySet())
            // end-states do not have outgoing edges, and thus no next states.
            .filter(entry -> entry.getValue().getNextStates().isEmpty())
            .transformAndConcat(entry -> entry.getValue().getIgnoreStates())
            .transform(ignores -> getAutomatonForStates(root, ignores.asSet()));

      case WEIGHTED: // export all nodes, where children are heavier than a given limit
        return getWeightedAutomata(root, pDependencies);

      case FIRST_BFS:
        return getFirstBFSAutomata(root, pDependencies);

      default:
        throw new AssertionError("unexpected export strategy");
    }
  }

  private Iterable<Automaton> getFirstBFSAutomata(
      ARGState pRoot, Map<ARGState, BranchingInfo> pDependencies) {
    Set<ARGState> statesForExport = getTopStatesForAutomata(pRoot, pDependencies);

    // remove redundant paths, e.g. do not export paths already covered by other paths
    Multimap<Integer, PersistentSet<ARGState>> sortedPaths = HashMultimap.create();
    for (ARGState state : statesForExport) {
      for (PersistentSet<ARGState> ignores : pDependencies.get(state).getIgnoreStates()) {
        sortedPaths.put(ignores.size(), ignores);
      }
    }
    List<PersistentSet<ARGState>> paths = new ArrayList<>();
    for (PersistentSet<ARGState> path : sortedPaths.values()) {
      if (!isCovered(path, sortedPaths)) {
        paths.add(path);
      }
    }

    return FluentIterable.from(paths)
        .transform(ignores -> getAutomatonForStates(pRoot, ignores.asSet()));
  }

  /**
   * returns whether another path already includes the current path, i.e., the ignored states of the
   * current path are a super-set of any other set.
   */
  private boolean isCovered(
      PersistentSet<ARGState> path, Multimap<Integer, PersistentSet<ARGState>> sortedPaths) {
    for (int size : sortedPaths.keySet()) {
      if (size < path.size()) {
        for (PersistentSet<ARGState> shorterPath : sortedPaths.get(size)) {
          if (path.asSet().containsAll(shorterPath.asSet())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /** return the frontier states after skipping N states */
  private Set<ARGState> getTopStatesForAutomata(
      ARGState root, Map<ARGState, BranchingInfo> pDependencies) {
    Set<ARGState> alwaysExport = new LinkedHashSet<>();
    Collection<ARGState> finished = new LinkedHashSet<>();
    Deque<ARGState> waitlist = new ArrayDeque<>();
    waitlist.add(root);
    while (!waitlist.isEmpty()) {
      if (finished.size() > skipFirstNum) {
        // we have skipped the first N states, return the frontier.
        // TODO exclude exit-states when counting skipN?
        // This might avoid to export simple paths at program-start.
        alwaysExport.addAll(waitlist);
        break;
      }
      ARGState s = waitlist.pop();
      if (!finished.add(s)) {
        continue;
      }
      BranchingInfo bi = pDependencies.get(s);
      Set<ARGState> children = bi.getNextStates();
      if (children.isEmpty()) {
        // end of ARG reached, export paths to state directly
        alwaysExport.add(s);
      }
      for (ARGState child : children) {
         if (!finished.contains(child)) {
           waitlist.add(child);
         }
       }
    }
    return alwaysExport;
  }

  private Iterable<Automaton> getWeightedAutomata(
      ARGState root, Map<ARGState, BranchingInfo> pDependencies) {

    // collect all end-states, i.e., states that have no succeeding next-state.
    Set<ARGState> endStates = new HashSet<>();
    for (Entry<ARGState, BranchingInfo> entry : pDependencies.entrySet()) {
      if (entry.getValue().getNextStates().isEmpty()) {
        endStates.add(entry.getKey());
      }
    }

    // tracks visited states with a flag whether all of their children are handled completely.
    // If not all children are handled completely (but finished),
    // the parent must be exported including paths to the non-exported children.
    Map<ARGState, Boolean> finished = new HashMap<>();

    List<Automaton> automata = new ArrayList<>();
    Deque<ARGState> waitlist = new ArrayDeque<>(endStates);
    while (!waitlist.isEmpty()) {
      ARGState s = waitlist.pop();
      if (finished.containsKey(s)) {
        continue;
      }
      BranchingInfo bi = pDependencies.get(s);
      Set<ARGState> children = bi.getNextStates();
      if (!finished.keySet().containsAll(children)) {
        // re-schedule until all children are finished.
        // does not imply that all children are exported!
        waitlist.add(s);
        continue;
      }
      final boolean completeExportOfState;
      Set<ARGState> finishedChildren = from(children).filter(c -> finished.get(c)).toSet();
      if (!children.isEmpty() && children.equals(finishedChildren)) {
        // all children exported, finished
        completeExportOfState = true;
      } else if (!finishedChildren.isEmpty()) {
        // only some children are exported -> export all siblings as one automaton.
        // TODO we currently only support look-ahead for one step.
        // This might lead to redundant traces in the exported automata.
        // Should we add a deeper analysis that also looks for (non-)exported grand-children?
        for (PersistentSet<ARGState> ignores : bi.getIgnoreStates()) {
          automata.add(getAutomatonForStates(root, Sets.union(ignores.asSet(), finishedChildren)));
        }
        completeExportOfState = true;
      } else {
        // no children are exported -> export current node or skip and export parent
        if (shouldExportAutomatonFor(root, s, pDependencies)) {
          for (PersistentSet<ARGState> ignores : bi.getIgnoreStates()) {
            automata.add(getAutomatonForStates(root, ignores.asSet()));
          }
          completeExportOfState = true;
        } else {
          // we do not export this automaton, but export the parent.
          completeExportOfState = false;
        }
      }
      finished.put(s, completeExportOfState);
      if (!completeExportOfState) {
        waitlist.addAll(bi.getParents());
      }
    }
    return automata;
  }

  /**
   * determine whether the automaton starting at root going via state s should be exported.
   *
   * <p>We can check whether the branch below s is large enough compared to the overall
   */
  private boolean shouldExportAutomatonFor(
      ARGState root, ARGState s, Map<ARGState, BranchingInfo> pDependencies) {
    if (s == root) { // if no other automaton is exported, then export the whole ARG via root
      return true;
    }
    int rootSize = sizeOfBranch(root);
    int branchSize = sizeOfBranch(s);
    if (branchSize > branchRatio * rootSize) {
      // export large branches and ignore small branches
      return true;
    }
    Collection<Integer> siblings =
        FluentIterable.from(pDependencies.get(s).getParents())
            .transformAndConcat(p -> pDependencies.get(p).getNextStates())
            .transform(n -> sizeOfBranch(n))
            .toSet();
    if (Collections.max(siblings) - Collections.min(siblings) > siblingRatio * rootSize) {
      // export states where siblings are very different in size
      return true;
    }
    return false;
  }

  /** cache to speedup the computation of subtree-sizes. */
  private final Map<ARGState, Integer> sizeCache = new HashMap<>();

  /** simple caching for {@link #sizeOfBranch0}. */
  private int sizeOfBranch(ARGState state) {
    return sizeCache.computeIfAbsent(state, this::sizeOfBranch0);
  }

  /** returns the number of states in the current branch until the end-states. */
  private int sizeOfBranch0(ARGState state) {
    Set<ARGState> reachable = new HashSet<>();
    Deque<ARGState> waitlist = new ArrayDeque<>();
    waitlist.add(state);
    while (!waitlist.isEmpty()) {
      ARGState s = uncover(waitlist.pop());
      if (reachable.add(s)) {
        waitlist.addAll(s.getChildren());
      }
    }
    return reachable.size();
  }

  /**
   * add the list of ignoredNodes to each BranchingInfo in the tree.
   *
   * <p>For loop-states, we just add the ignoredNodes of their parent. Maybe we should even expect
   * no loop.states at all.
   */
  private void addBranchInformation(ARGState root, Map<ARGState, BranchingInfo> dependencies) {
    dependencies.get(root).addIgnoreStates(PersistentSet.of());

    Deque<ARGState> waitlist = new ArrayDeque<>();
    waitlist.add(root);
    Set<ARGState> finished = new HashSet<>();

    while (!waitlist.isEmpty()) {
      ARGState branchingPoint = waitlist.pop();
      assert dependencies.containsKey(branchingPoint);
      if (!finished.add(branchingPoint)) {
        continue;
      }

      BranchingInfo branch = dependencies.get(branchingPoint);
      Preconditions.checkArgument(branch.current == branchingPoint);

      for (Entry<ARGState, ARGState> viaChild : branch.children.entries()) {
        ARGState nextState = viaChild.getValue();
        waitlist.add(nextState);
        BranchingInfo nextBi = dependencies.get(nextState);
        for (PersistentSet<ARGState> ignoreBranch : branch.getIgnoreStates()) {
          PersistentSet<ARGState> newIgnoreBranch = ignoreBranch;
          if (!branch.isPartOfLoop()) {
            // for loop-states, we cannot split the automaton -> ignore this case
            // for non-loop-states, we split the automaton
            for (ARGState sibling :
                from(branchingPoint.getChildren()).filter(s -> s != viaChild.getKey())) {
              newIgnoreBranch = newIgnoreBranch.addAndCopy(uncover(sibling));
            }
          }
          nextBi.addIgnoreStates(newIgnoreBranch);
        }
      }
    }
  }

  /** add backwards parent relation. */
  private void addParents(Map<ARGState, BranchingInfo> pDependencies) {
    for (Entry<ARGState, BranchingInfo> entry : pDependencies.entrySet()) {
      for (ARGState nextState : entry.getValue().getNextStates()) {
        pDependencies.get(nextState).addParent(entry.getKey());
      }
    }
  }

  private Map<ARGState, BranchingInfo> getGlobalBranchingTree(ARGState pRoot) {
    Preconditions.checkArgument(!pRoot.isCovered());
    Map<ARGState, BranchingInfo> branchingTree = new LinkedHashMap<>();
    Deque<ARGState> waitlist = new ArrayDeque<>();
    Collection<ARGState> finished = new HashSet<>();
    waitlist.add(pRoot);
    while (!waitlist.isEmpty()) {
      ARGState s = uncover(waitlist.pop());
      if (!finished.add(s)) {
        continue;
      }
      ImmutableSetMultimap.Builder<ARGState, ARGState> childToNext = ImmutableSetMultimap.builder();
      for (ARGState child : uncover(s.getChildren())) {
        Collection<ARGState> nextStates = getNext(child);
        waitlist.addAll(nextStates);
        childToNext.putAll(child, nextStates);
      }
      branchingTree.put(s, new BranchingInfo(s, childToNext.build()));
    }
    return branchingTree;
  }

  /**
   * get dependencies without loops, i.e., remove all loops.
   *
   * <p>Runtime-info: We do not analyze the whole ARG, but only the states in pDependencies.
   */
  private Map<ARGState, BranchingInfo> getFilteredDependencies(
      ARGState root, final Map<ARGState, BranchingInfo> pDependencies) {
    // pre-compute loop-info once
    markLoopStates(pDependencies);

    // then build new dependencies without loops
    Map<ARGState, BranchingInfo> branchingTree = new LinkedHashMap<>();
    Deque<ARGState> waitlist = new ArrayDeque<>();
    Collection<ARGState> finished = new HashSet<>();
    waitlist.add(root);
    while (!waitlist.isEmpty()) {
      final ARGState s = uncover(waitlist.pop());
      Preconditions.checkState(pDependencies.containsKey(s));
      if (!finished.add(s)) {
        continue;
      }
      ImmutableSetMultimap.Builder<ARGState, ARGState> childToNext = ImmutableSetMultimap.builder();
      for (ARGState child : uncover(s.getChildren())) {
        Collection<ARGState> nextStates = getNextWithoutLoops(child, s, pDependencies);
        waitlist.addAll(nextStates);
        childToNext.putAll(child, nextStates);
      }
      branchingTree.put(s, new BranchingInfo(s, childToNext.build()));
    }

    markLoopStates(branchingTree);
    assert Iterables.all(branchingTree.values(), bi -> !bi.isPartOfLoop())
        : "we should have removed all cyclic dependencies";

    return branchingTree;
  }

  /** get all branching states that part of a cycle. */
  private void markLoopStates(Map<ARGState, BranchingInfo> pDependencies) {
    for (Entry<ARGState, BranchingInfo> entry : pDependencies.entrySet()) {
      if (isPartOfCycle(entry.getKey(), pDependencies)) {
        entry.getValue().setPartOfLoop();
      }
    }
  }

  /**
   * return next states for given child, with ignoring loops in the ARG.
   *
   * <p>Runtime-info: We do not analyze the whole ARG, but only the states in pDependencies.
   */
  private Collection<ARGState> getNextWithoutLoops(
      final ARGState pChild,
      final ARGState pState,
      final Map<ARGState, BranchingInfo> pDependencies) {
    final Collection<ARGState> nextStates = new LinkedHashSet<>();
    final Deque<ARGState> waitlist = new ArrayDeque<>();
    final Collection<ARGState> finished = new HashSet<>();
    waitlist.addAll(pDependencies.get(pState).children.get(pChild));
    while (!waitlist.isEmpty()) {
      final ARGState s = uncover(waitlist.pop());
      Preconditions.checkState(pDependencies.containsKey(s));
      if (!finished.add(s)) {
        continue;
      }
      final BranchingInfo bi = pDependencies.get(s);
      if (bi.isPartOfLoop()) {
        // go deeper into branch
        waitlist.addAll(bi.getNextStates());
      } else {
        // terminate current branch-visitation
        nextStates.add(s);
      }
    }
    return nextStates;
  }

  /**
   * check whether there is a loop containing the given state.
   *
   * <p>Actually BFS with additional check for initial element.
   *
   * <p>Runtime-info: We do not analyze the whole ARG, but only the states in pDependencies.
   */
  private boolean isPartOfCycle(
      final ARGState pRoot, final Map<ARGState, BranchingInfo> pDependencies) {
    final Deque<ARGState> waitlist = new ArrayDeque<>();
    final Collection<ARGState> finished = new HashSet<>();
    waitlist.addAll(pDependencies.get(pRoot).getNextStates());
    while (!waitlist.isEmpty()) {
      final ARGState s = waitlist.pop();
      assert pDependencies.containsKey(s);
      if (!finished.add(s)) {
        continue;
      }
      if (s == pRoot) {
        return true;
      }
      waitlist.addAll(pDependencies.get(s).getNextStates());
    }
    return false;
  }

  /**
   * return all branching nodes directly reachable from the current node, including nodes reachable
   * via branches of covered states.
   */
  private Collection<ARGState> getNext(final ARGState base) {
    final Collection<ARGState> next = new ArrayList<>();
    final Deque<ARGState> waitlist = new ArrayDeque<>();
    final Collection<ARGState> finished = new HashSet<>();
    waitlist.add(base);
    while (!waitlist.isEmpty()) {
      final ARGState s = uncover(waitlist.pop());
      if (!finished.add(s)) {
        continue;
      }
      if (s.getChildren().size() > 1 || s.getChildren().size() == 0) {
        // branching-points and end-states are important
        next.add(s);
      } else {
        waitlist.addAll(s.getChildren());
      }
    }
    return next;
  }

  /** create simple dot-graph for dependencies, marking loop-states. useful for debugging. */
  @SuppressWarnings("unused")
  private static String toDot(Map<ARGState, BranchingInfo> pDependencies) {
    StringBuilder str = new StringBuilder("digraph BRANCHING_NODES {\n");
    str.append("  node [style=\"filled\" color=\"white\"];\n");
    for (Entry<ARGState, BranchingInfo> entry : pDependencies.entrySet()) {
      String color = entry.getValue().isPartOfLoop() ? "green" : "white";
      String label =
          id(entry.getKey())
              + "\\n{"
              + Joiner.on(",\\n")
                  .join(
                      Iterables.transform(
                          entry.getValue().ignoreStates, ARGToAutomatonConverter::id))
              + "}";
      str.append(
          String.format(
              "  %s [fillcolor=\"%s\", label=\"%s\"];%n", id(entry.getKey()), color, label));
    }
    for (Entry<ARGState, BranchingInfo> entry : pDependencies.entrySet()) {
      for (Entry<ARGState, ARGState> viaChild : entry.getValue().children.entries()) {
        str.append(
            String.format(
                "  %s -> %s [label=\"%s\"];%n",
                id(entry.getKey()), id(viaChild.getValue()), id(viaChild.getKey())));
      }
    }
    return str.append("}").toString();
  }

  private static String id(final ARGState s) {
    return "S" + s.getStateId() + "_N" + AbstractStates.extractLocation(s).getNodeNumber();
  }

  private static Iterable<String> id(final Iterable<ARGState> states) {
    return Iterables.transform(states, ARGToAutomatonConverter::id);
  }

  private static Iterable<String> ids(
      final Iterable<? extends Iterable<ARGState>> iterableOfStates) {
    return Iterables.transform(iterableOfStates, states -> id(states).toString());
  }

  private static String id(ImmutableMultimap<ARGState, ARGState> children) {
    return "{"
        + Joiner.on(", ")
            .join(Iterables.transform(children.keys(), s -> id(s) + "->" + id(children.get(s))))
        + "}";
  }

  private static final class BranchingInfo {
    private final ARGState current;

    /** mapping of direct child-states towards nextStates. */
    private final ImmutableSetMultimap<ARGState, ARGState> children;

    private final Set<ARGState> parents = new LinkedHashSet<>(); // lazily filled

    private boolean isPartOfLoop = false; // lazy

    /**
     * current state can be reached via several paths. Ignored states cut off all other branches for
     * each of those paths. Each set represents a single path with its cut-off-states.
     */
    private Set<PersistentSet<ARGState>> ignoreStates = new LinkedHashSet<>();

    BranchingInfo(
        ARGState pCurrent,
        ImmutableSetMultimap<ARGState, ARGState> pChildren) {
      current = pCurrent;
      children = pChildren;
    }

    private Set<ARGState> getNextStates() {
      return ImmutableSet.copyOf(children.values());
    }

    private Set<PersistentSet<ARGState>> getIgnoreStates() {
      return ignoreStates;
    }

    private void addIgnoreStates(PersistentSet<ARGState> pIgnoreStates) {
      Preconditions.checkNotNull(pIgnoreStates);
      ignoreStates.add(pIgnoreStates);
    }

    private boolean isPartOfLoop() {
      return isPartOfLoop;
    }

    private void setPartOfLoop() {
      isPartOfLoop = true;
    }

    private void addParent(ARGState parent) {
      parents.add(parent);
    }

    private Set<ARGState> getParents() {
      return parents;
    }

    @Override
    public String toString() {
      return String.format(
          "BranchingInfo {parents=%s, children=%s, ignore=%s, loop=%s}",
          id(parents), id(children), ids(ignoreStates), isPartOfLoop);
    }
  }
}
