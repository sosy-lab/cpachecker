// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * This class converts an ARG into an automaton (or several automata), that can be used as
 * specification for a second execution of CPAchecker. The automata represent disjoint partitions of
 * the state space, i.e., the analysis of each one of them returns a result, and merging all those
 * results is sound for the whole program. We currently provide different strategies for splitting
 * the state space.
 *
 * <p>The idea is based on the paper "Structurally Defined Conditional Data-Flow Static Analysis"
 * from Elena Sherman and Matthew B. Dwyer (2018).
 */
@Options(prefix = "cpa.arg.automaton")
public class ARGToAutomatonConverter {

  public enum SplitterStrategy {
    /** do not apply splitting, but return ONE automaton for the ARG */
    NONE,
    /** split at non-nested conditions only */
    GLOBAL_CONDITIONS,
    /** split different leaf states */
    LEAVES,
    /** split at target states */
    TARGETS,
  }

  public enum BranchExportStrategy {
    /** export no branches */
    NONE,
    /** export all branches, will contain redundant paths, mostly for debugging. */
    ALL,
    /** export all leaf nodes of the ARG, very precise, no redundant paths are exported. */
    LEAVES,
    /** export some intermediate nodes, sound due to export of siblings if needed. */
    WEIGHTED,
    /**
     * export top nodes according to BFS, but skip the first N internal nodes, e.g. export the
     * frontier nodes after analyzing N nodes.
     */
    FIRST_BFS
  }

  public enum DataExportStrategy {
    /** export locations, i.e., most precise information. */
    LOCATION,
    /** export call-stack information, i.e., very abstract representation. */
    CALLSTACK
  }

  @Option(
      secure = true,
      description = "which coarse strategy should be applied when analyzing the ARG?")
  private SplitterStrategy splitStrategy = SplitterStrategy.TARGETS;

  @Option(
      secure = true,
      description = "after determining branches, which one of them should be exported?")
  private BranchExportStrategy selectionStrategy = BranchExportStrategy.LEAVES;

  @Option(
      secure = true,
      description =
          "what data should be exported from the ARG nodes? "
              + "A different strategy might result in a smaller automaton.")
  private DataExportStrategy dataStrategy = DataExportStrategy.LOCATION;

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

  private final CBinaryExpressionBuilder cBinaryExpressionBuilder;

  public ARGToAutomatonConverter(
      @Nullable Configuration config, MachineModel machinemodel, LogManager logger)
      throws InvalidConfigurationException {
    config.inject(this);
    cBinaryExpressionBuilder = new CBinaryExpressionBuilder(machinemodel, logger);
  }

  /**
   * get a single (!) automaton for the whole program. If a splitting strategy is set, it is used to
   * determine the leaves of this automaton.
   *
   * @param targetsOnly Export all possible paths or only paths leading to target states.
   */
  public Automaton getAutomaton(ARGState root, boolean targetsOnly) {
    switch (dataStrategy) {
      case LOCATION:
        return getLocationAutomatonForStates(root, Predicates.alwaysFalse(), targetsOnly);
      case CALLSTACK:
        return getCallstackAutomatonForStates(root, getLeaves(root, targetsOnly), targetsOnly);
      default:
        throw new AssertionError("unexpected strategy");
    }
  }

  /** get several automata according to the splitter strategy. */
  public Iterable<Automaton> getAutomata(ARGState root) {
    switch (splitStrategy) {
      case NONE:
        return Collections.singleton(
            getLocationAutomatonForStates(root, Predicates.alwaysFalse(), false));
      case GLOBAL_CONDITIONS:
        return getGlobalConditionSplitAutomata(root, selectionStrategy);
      case LEAVES:
        return getLeaves(root, false).transform(l -> getAutomatonForLeaf(root, l));
      case TARGETS:
        return getLeaves(root, true).transform(l -> getAutomatonForLeaf(root, l));
      default:
        throw new AssertionError("unexpected strategy");
    }
  }

  /** generate an automaton that traverses the subgraph, but leaves out ignored states. */
  private static Automaton getLocationAutomatonForStates(
      ARGState pRoot, Predicate<ARGState> ignoreState, boolean withTargetStates) {
    try {
      return getLocationAutomatonForStates0(pRoot, ignoreState, withTargetStates);
    } catch (InvalidAutomatonException e) {
      throw new AssertionError("unexpected exception", e);
    }
  }

  /** generate an automaton that traverses the subgraph, but leaves out ignored states. */
  private static Automaton getLocationAutomatonForStates0(
      ARGState root, Predicate<ARGState> ignoreState, boolean withTargetStates)
      throws InvalidAutomatonException {

    Preconditions.checkArgument(!ignoreState.apply(root));
    Preconditions.checkArgument(!root.isCovered());

    Map<String, AutomatonVariable> variables = ImmutableMap.of();

    Deque<ARGState> waitlist = new ArrayDeque<>();
    Collection<ARGState> finished = new HashSet<>();
    waitlist.add(root);

    List<AutomatonInternalState> states = new ArrayList<>();
    if (withTargetStates) {
      states.add(AutomatonInternalState.ERROR);
    }
    while (!waitlist.isEmpty()) {
      ARGState s = uncover(waitlist.pop());
      if (!finished.add(s)) {
        continue;
      }
      List<AutomatonTransition> transitions = new ArrayList<>();
      List<AutomatonBoolExpr> locationQueries = new ArrayList<>();
      for (ARGState child : s.getChildren()) {
        child = uncover(child);
        if (ignoreState.apply(child)) {
          // ignore those states here, BOTTOM-state will be inserted afterwards automatically.
          // Note: if sibling with same location is not ignored, ignorance might be useless.
          continue;
        }

        CFANode location = AbstractStates.extractLocation(child);
        AutomatonBoolExpr locationQuery =
            new AutomatonBoolExpr.CPAQuery("location", "nodenumber==" + location.getNodeNumber());
        locationQueries.add(locationQuery);
        final String id;
        if (withTargetStates && child.isTarget()) {
          id = AutomatonInternalState.ERROR.getName();
        } else {
          id = id(child);
        }
        transitions.add(new AutomatonTransition.Builder(locationQuery, id).build());
        waitlist.add(child);
      }

      if (withTargetStates && s.isTarget()) {
        assert transitions.isEmpty();
        assert states.contains(AutomatonInternalState.ERROR);
      } else {
        transitions.add(
            new AutomatonTransition.Builder(
                    buildOtherwise(locationQueries), AutomatonInternalState.BOTTOM)
                .build());

        boolean hasSeveralChildren = transitions.size() > 1;
        states.add(
            new AutomatonInternalState(id(s), transitions, false, hasSeveralChildren, false));
      }
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

  @Deprecated // unmaintained?
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
        return ImmutableList.of();

      case ALL: // export all nodes, mainly for debugging.
        return from(pDependencies.entrySet())
            .transformAndConcat(entry -> entry.getValue().getIgnoreStates())
            .transform(
                ignores -> getLocationAutomatonForStates(root, s -> ignores.contains(s), false));

      case LEAVES: // ALL_PATHS, export all leaf-nodes, sub-graphs cover the whole graph.
        // no redundant paths expected, if leafs are reached via different paths.
        return from(pDependencies.entrySet())
            // end-states do not have outgoing edges, and thus no next states.
            .filter(entry -> entry.getValue().getNextStates().isEmpty())
            .transformAndConcat(entry -> entry.getValue().getIgnoreStates())
            .transform(
                ignores -> getLocationAutomatonForStates(root, s -> ignores.contains(s), false));

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

    return from(paths)
        .transform(
            ignores -> getLocationAutomatonForStates(pRoot, s -> ignores.contains(s), false));
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
          automata.add(
              getLocationAutomatonForStates(
                  root, as -> (ignores.contains(as) || finishedChildren.contains(as)), false));
        }
        completeExportOfState = true;
      } else {
        // no children are exported -> export current node or skip and export parent
        if (shouldExportAutomatonFor(root, s, pDependencies)) {
          for (PersistentSet<ARGState> ignores : bi.getIgnoreStates()) {
            automata.add(getLocationAutomatonForStates(root, as -> ignores.contains(as), false));
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
    if (s.equals(root)) { // if no other automaton is exported, then export the whole ARG via root
      return true;
    }
    int rootSize = sizeOfBranch(root);
    int branchSize = sizeOfBranch(s);
    if (branchSize > branchRatio * rootSize) {
      // export large branches and ignore small branches
      return true;
    }
    Collection<Integer> siblings =
        from(pDependencies.get(s).getParents())
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
      Preconditions.checkArgument(branch.current.equals(branchingPoint));

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
                from(branchingPoint.getChildren()).filter(s -> !s.equals(viaChild.getKey()))) {
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
      if (s.equals(pRoot)) {
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
      if (s.getChildren().size() > 1 || s.getChildren().isEmpty()) {
        // branching-points and end-states are important
        next.add(s);
      } else {
        waitlist.addAll(s.getChildren());
      }
    }
    return next;
  }

  private static FluentIterable<ARGState> getLeaves(ARGState pRoot, boolean targetsOnly) {
    FluentIterable<ARGState> leaves =
        ARGUtils.getNonCoveredStatesInSubgraph(pRoot).filter(s -> s.getChildren().isEmpty());
    return targetsOnly ? leaves.filter(ARGState::isTarget) : leaves;
  }

  /** generate an automaton that leads to the leaf state. */
  private Automaton getAutomatonForLeaf(ARGState pRoot, ARGState pLeaf) {

    Preconditions.checkArgument(!pRoot.equals(pLeaf));
    Preconditions.checkArgument(!pLeaf.isCovered());

    switch (dataStrategy) {
      case LOCATION:
        Collection<ARGState> allStatesOnPaths = getAllStatesOnPathsTo(pLeaf);
        Preconditions.checkArgument(allStatesOnPaths.contains(pRoot));
        Preconditions.checkArgument(allStatesOnPaths.contains(pLeaf));
        return getLocationAutomatonForStates(pRoot, s -> !allStatesOnPaths.contains(s), true);
      case CALLSTACK:
        return getCallstackAutomatonForStates(pRoot, Collections.singleton(pLeaf), true);
      default:
        throw new AssertionError("unhandled case");
    }
  }

  private Automaton getCallstackAutomatonForStates(
      ARGState pRoot, Iterable<ARGState> pLeaves, boolean withTargetStates) {
    try {
      return getCallstackAutomatonForStates0(pRoot, pLeaves, withTargetStates);
    } catch (InvalidAutomatonException e) {
      throw new AssertionError("unexpected exception", e);
    }
  }

  private Automaton getCallstackAutomatonForStates0(
      ARGState pRoot, Iterable<ARGState> pLeaves, boolean withTargetStates)
      throws InvalidAutomatonException {

    // build the call graph, i.e., a directed tree starting at main-entry
    final Multimap<CallstackState, CallstackState> callstacks = LinkedHashMultimap.create();
    final Map<CallstackState, CallstackState> inverseCallstacks = new LinkedHashMap<>();
    final Multimap<CallstackState, ARGState> callstackToLeaves = LinkedHashMultimap.create();
    final Multimap<CallstackState, ARGState> callstackToLeafWithParentAssumptions =
        LinkedHashMultimap.create();
    for (ARGState leaf : pLeaves) {
      CallstackState callstack = AbstractStates.extractStateByType(leaf, CallstackState.class);
      Preconditions.checkNotNull(callstack);
      Preconditions.checkArgument(leaf.getParents().size() == 1);
      Preconditions.checkArgument(leaf.getParents().iterator().next().getEdgeToChild(leaf) != null);
      // if an error occurs when entering the function, we need to remove the last entry from the
      // stack (at least for assumption handling, otherwise this probably does not happen any way):
      if (leaf.getParents().iterator().next().getEdgeToChild(leaf) instanceof FunctionCallEdge) {
        callstack = callstack.getPreviousState();
      }
      callstackToLeaves.put(callstack, leaf);
      for (ARGState parent : leaf.getParents()) {
        if (AbstractStates.asIterable(parent)
            .filter(AbstractStateWithAssumptions.class)
            .anyMatch(x -> !x.getAssumptions().isEmpty())) {
          callstackToLeafWithParentAssumptions.put(callstack, leaf);
        }
      }
      CallstackState prev = callstack.getPreviousState();
      while (prev != null) {
        callstacks.put(prev, callstack);
        inverseCallstacks.put(callstack, prev);
        callstack = prev;
        prev = callstack.getPreviousState();
      }
    }

    final List<AutomatonInternalState> states = new ArrayList<>();
    if (withTargetStates) {
      states.add(AutomatonInternalState.ERROR);
    }

    // build automaton from call-graph
    CallstackState root = AbstractStates.extractStateByType(pRoot, CallstackState.class);
    Deque<CallstackState> waitlist = new ArrayDeque<>();
    Set<CFANode> reached = new HashSet<>();
    Set<CallstackState> stacksWithAssumptions = new HashSet<>();
    waitlist.push(root);
    reached.add(root.getCallNode());
    while (!waitlist.isEmpty()) {
      final CallstackState elem = waitlist.removeFirst();
      boolean useAll = false;
      ImmutableSet.Builder<AExpression> assumptionsBuilder = ImmutableSet.builder();
      for (ARGState leaf : callstackToLeafWithParentAssumptions.get(elem)) {
        for (ARGState parent : leaf.getParents()) {
          for (AbstractStateWithAssumptions state :
              AbstractStates.asIterable(parent).filter(AbstractStateWithAssumptions.class)) {
            assumptionsBuilder.addAll(state.getAssumptions());
          }
        }
      }
      Set<AExpression> assumptions = assumptionsBuilder.build();
      final List<AutomatonTransition> transitions = new ArrayList<>();
      for (ARGState leaf : callstackToLeaves.get(elem)) {
        if (assumptions.isEmpty()) {
          // no assumptions, proceed normally:
          transitions.add(
              makeLocationTransition(
                  AbstractStates.extractLocation(leaf).getNodeNumber(),
                  withTargetStates ? AutomatonInternalState.ERROR.getName() : id(leaf)));
        } else {
          // assumptions present, bend transition to parent instead:
          ARGState parent = leaf.getParents().iterator().next();
          stacksWithAssumptions.add(elem);
          transitions.add(
              makeLocationTransition(
                  AbstractStates.extractLocation(parent).getNodeNumber(), id(parent), assumptions));
          try {
            transitions.add(
                makeLocationTransition(
                    AbstractStates.extractLocation(parent).getNodeNumber(),
                    id(elem),
                    transformedImmutableListCopy(
                        assumptions, x -> negateExpression((CExpression) x))));
          } catch (ClassCastException e) {
            throw new AssertionError(
                "Currently there is only support for negating CExpressions", e);
          }
          useAll = true;
        }
      }
      for (CallstackState called : callstacks.get(elem)) {
        if (!reached.contains(called.getCallNode())) {
          waitlist.add(called);
          reached.add(called.getCallNode());
        }
        // calling the next function on the stack corresponds to going to the next automaton state
        transitions.add(makeLocationTransition(called.getCallNode().getNodeNumber(), id(called)));
      }
      final CallstackState callee = inverseCallstacks.get(elem);
      if (callee != null) {
        // returning from the current function corresponds to returning to the previous automaton
        // state (except for the main function):
        transitions.add(
            makeLocationTransition(
                elem.getCallNode().getLeavingSummaryEdge().getSuccessor().getNodeNumber(),
                id(callee)));
      }
      states.add(new AutomatonInternalState(id(elem), transitions, false, useAll, false));
    }

    finishAssumptionHandling(states, callstackToLeaves, stacksWithAssumptions);
    return new Automaton("ARG", ImmutableMap.of(), states, id(root));
  }

  private static void finishAssumptionHandling(
      List<AutomatonInternalState> pStates,
      Multimap<CallstackState, ARGState> pCallstackToLeaves,
      Set<CallstackState> pStacksWithAssumptions) {

    for (CallstackState callstack : pStacksWithAssumptions) {
      final Set<ARGState> parents = new HashSet<>();
      final Map<ARGState, CFANode> parentsToLeafNode = new HashMap<>();
      for (ARGState leaf : pCallstackToLeaves.get(callstack)) {
        ARGState parent = leaf.getParents().iterator().next();
        if (parents.add(parent)) {
          CFANode leafNode = AbstractStates.extractLocation(leaf);
          if (parentsToLeafNode.containsKey(parent)) {
            assert parentsToLeafNode.get(parent).equals(leafNode)
                : "Expected to have only one CFANode for the children"
                    + "(this holds at least when considering overflows with OverflowCPA)";
          }
          parentsToLeafNode.put(parent, AbstractStates.extractLocation(leaf));
        }
      }
      for (ARGState parent : parents) {
        final CFANode leafNode = parentsToLeafNode.get(parent);
        final List<AutomatonTransition> transitions = new ArrayList<>();
        transitions.add(
            makeLocationTransition(
                leafNode.getNodeNumber(), AutomatonInternalState.ERROR.getName()));
        transitions.add(makeNegatedLocationTransition(leafNode.getNodeNumber(), id(callstack)));
        // Following one matching transition when in the following state should be ok:
        pStates.add(new AutomatonInternalState(id(parent), transitions));
      }
    }
  }

  private static AutomatonTransition makeLocationTransition(
      int nodeNumber, String followStateName) {
    return makeLocationTransition(nodeNumber, followStateName, ImmutableList.of());
  }

  private static AutomatonTransition makeLocationTransition(
      int nodeNumber, String followStateName, Collection<AExpression> assumptions) {
    return makeLocationTransition(nodeNumber, followStateName, assumptions, false);
  }

  private static AutomatonTransition makeNegatedLocationTransition(
      int nodeNumber, String followStateName, Collection<AExpression> assumptions) {
    return makeLocationTransition(nodeNumber, followStateName, assumptions, true);
  }

  private static AutomatonTransition makeNegatedLocationTransition(
      int nodeNumber, String followStateName) {
    return makeNegatedLocationTransition(nodeNumber, followStateName, ImmutableList.of());
  }

  private static AutomatonTransition makeLocationTransition(
      int nodeNumber, String followStateName, Collection<AExpression> assumptions, boolean negate) {
    AutomatonBoolExpr expr =
        new AutomatonBoolExpr.CPAQuery("location", "nodenumber==" + nodeNumber);
    return new AutomatonTransition.Builder(
            negate ? new AutomatonBoolExpr.Negation(expr) : expr, followStateName)
        .withAssumptions(ImmutableList.copyOf(assumptions))
        .build();
  }

  private CExpression negateExpression(CExpression expr) {
    try {
      return cBinaryExpressionBuilder.negateExpressionAndSimplify(expr);
    } catch (UnrecognizedCodeException e) {
      throw new AssertionError(e);
    }
  }

  /** the same as {@link ARGUtils#getAllStatesOnPathsTo}, but with coverage handling. */
  private static Collection<ARGState> getAllStatesOnPathsTo(ARGState pLeaf) {
    Collection<ARGState> finished = new LinkedHashSet<>();
    Deque<ARGState> waitlist = new ArrayDeque<>();
    waitlist.add(pLeaf);
    while (!waitlist.isEmpty()) {
      ARGState s = waitlist.pop();
      if (!finished.add(s)) {
        continue;
      }
      waitlist.addAll(s.getParents());
      waitlist.addAll(s.getCoveredByThis());
    }
    return finished;
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

  private static String id(CallstackState s) {
    return id(s, false);
  }

  private static String id(CallstackState s, boolean complete) {
    StringBuilder strBuilder = new StringBuilder().append(currentCallDescription(s));
    if (complete) {
      while (s.getPreviousState() != null) {
        s = s.getPreviousState();
        strBuilder.insert(0, currentCallDescription(s) + "__");
      }
    }
    return strBuilder.toString();
  }

  private static String currentCallDescription(final CallstackState s) {
    return s.getCurrentFunction()
        + "_N"
        + s.getCallNode().getNodeNumber()
        + "_L"
        + getCallLineNumber(s);
  }

  private static int getCallLineNumber(final CallstackState s) {
    final CFANode node = s.getCallNode();
    int position = 0;
    // we set a position even if there is no FunctionCallEdge here on purpose!(this is needed for
    // main, whose node from the CallstackState does not have a leaving FunctionCallEdge)
    for (int i = 0; i < node.getNumLeavingEdges(); i++) {
      final CFAEdge edge = node.getLeavingEdge(i);
      position = edge.getLineNumber();
      if (edge instanceof FunctionCallEdge) {
        break;
      }
    }
    return position;
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

    BranchingInfo(ARGState pCurrent, ImmutableSetMultimap<ARGState, ARGState> pChildren) {
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
