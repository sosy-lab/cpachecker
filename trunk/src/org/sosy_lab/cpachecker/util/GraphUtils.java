// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.StronglyConnectedComponent;

/** Some utilities for generic graphs. */
public class GraphUtils {

  private GraphUtils() {}

  /**
   * Project a graph to a subset of "relevant" nodes. The result is a SetMultimap containing the
   * successor relationships between all relevant nodes. A pair of nodes (a, b) is in the
   * SetMultimap, if there is a path through the graph from a to b which does not pass through any
   * other relevant node.
   *
   * <p>To get the predecessor relationship, you can use {@link
   * Multimaps#invertFrom(com.google.common.collect.Multimap, com.google.common.collect.Multimap)}.
   *
   * @param root The start of the graph to project (always considered relevant).
   * @param isRelevant The predicate determining which nodes are in the resulting relationship.
   * @param successorFunction A function giving the direct successors of any node.
   * @param <N> The node type of the graph.
   */
  public static <N> SetMultimap<N, N> projectARG(
      final N root,
      final Function<? super N, ? extends Iterable<N>> successorFunction,
      Predicate<? super N> isRelevant) {
    checkNotNull(root);

    isRelevant = Predicates.or(Predicates.equalTo(root), isRelevant);

    SetMultimap<N, N> successors = LinkedHashMultimap.create();

    // Our state is a stack of pairs of todo items.
    // The first item of each pair is a relevant state,
    // for which we are looking for relevant successor states.
    // The second item is a state,
    // whose children should be handled next.
    Deque<Pair<N, N>> todo = new ArrayDeque<>();
    Set<N> visited = new HashSet<>();
    todo.push(Pair.of(root, root));

    while (!todo.isEmpty()) {
      final Pair<N, N> currentPair = todo.pop();
      final N currentPredecessor = currentPair.getFirst();
      final N currentState = currentPair.getSecond();

      if (!visited.add(currentState)) {
        continue;
      }

      for (N child : successorFunction.apply(currentState)) {
        if (isRelevant.apply(child)) {
          successors.put(currentPredecessor, child);

          todo.push(Pair.of(child, child));

        } else {
          todo.push(Pair.of(currentPredecessor, child));
        }
      }
    }

    return successors;
  }

  /**
   * Find and retrieve all cycles from an exclusive list of {@link ARGState}s. I.e., only the states
   * within the list are considered, while every other ARGState from the {@link ReachedSet} is
   * explicitly ignored.
   *
   * <p>For more information, see {@link GraphUtils#retrieveSimpleCycles(List, Set)}
   *
   * @param pStates list of {@link ARGState}s to be looked for cycles
   * @param pReached the {@link ReachedSet} to retrieve all other states which are ignored later on
   * @return An adjacency list containing all cycles, given as a list of {@link ARGState}
   */
  public static List<List<ARGState>> retrieveSimpleCycles(
      List<ARGState> pStates, ReachedSet pReached) {
    Set<ARGState> filteredStates =
        new HashSet<>(Collections2.transform(pReached.asCollection(), s -> (ARGState) s));
    filteredStates.removeAll(pStates);
    return retrieveSimpleCycles(pStates, filteredStates);
  }

  /**
   * Find and retrieve all cycles from a list of {@link ARGState}s using Donald B. Johnson's
   * algorithm.
   *
   * <p>The algorithm finds all elementary circuits in time bounded by O((n + e)(c + 1)), with n
   * being the nodes, e the edges, and c the number of cycles found.
   *
   * @param pStates the ARGStates to be looked for cycles
   * @return An adjacency list containing all cycles, given as a list of {@link ARGState}
   * @see <a
   *     href="https://github.com/jgrapht/jgrapht/blob/master/jgrapht-core/src/main/java/org/jgrapht/alg/cycle/JohnsonSimpleCycles.java">code-references</a>
   * @see <a
   *     href="https://github.com/mission-peace/interview/blob/master/src/com/interview/graph/AllCyclesInDirectedGraphJohnson.java">code-references
   *     2</a>
   */
  public static List<List<ARGState>> retrieveSimpleCycles(
      List<ARGState> pStates, Set<ARGState> pExcludeStates) {
    Set<ARGState> blockedSet = new HashSet<>();
    SetMultimap<ARGState, ARGState> blockedMap = HashMultimap.create();
    Deque<ARGState> stack = new ArrayDeque<>();
    List<List<ARGState>> allCycles = new ArrayList<>();

    int startIndex = 0;
    while (startIndex < pStates.size() - 1) {
      // Find SCCs in the subgraph induced by pStates starting with startIndex and beyond.
      // This is done by using Tarjan's algorithm and pretending that nodes with an index
      // smaller than startIndex do not exist (these are stored in the excludeSet)
      List<ARGState> subList = pStates.subList(startIndex, pStates.size());
      Set<ARGState> excludeSet = new HashSet<>(pStates);
      excludeSet.addAll(pExcludeStates);
      excludeSet.removeAll(subList);
      ImmutableSet<StronglyConnectedComponent> SCCs =
          retrieveSCCs(subList, excludeSet).stream()
              .filter(x -> x.getNodes().size() > 1)
              .collect(ImmutableSet.toImmutableSet());

      if (!SCCs.isEmpty()) {
        // find the SCC with the minimum index with respect to pStates
        ARGState s =
            SCCs.stream()
                .map(x -> pStates.indexOf(x.getRootNode()))
                .reduce((x, y) -> x.compareTo(y) <= 0 ? x : y)
                .map(pStates::get)
                .orElseThrow();

        blockedSet.clear();
        blockedMap.clear();

        findCyclesInSCC(s, s, blockedSet, blockedMap, stack, allCycles, excludeSet);

        // TODO: the next line only works if pStates has a deterministic order
        startIndex = pStates.indexOf(s) + 1;

      } else {
        break;
      }
    }

    return allCycles;
  }

  /** Find cycles in a strongly connected graph per Johnson. */
  private static boolean findCyclesInSCC(
      ARGState pStartState,
      ARGState pCurrentState,
      Set<ARGState> pBlockedSet,
      SetMultimap<ARGState, ARGState> pBlockedMap,
      Deque<ARGState> pStack,
      List<List<ARGState>> pAllCycles,
      Set<ARGState> pExcludeSet) {

    if (pExcludeSet.contains(pCurrentState)) {
      // Do not regard nodes which were deliberately put into a set of excluded states
      return false;
    }

    boolean foundCycle = false;
    pStack.push(pCurrentState);
    pBlockedSet.add(pCurrentState);

    for (ARGState successor : pCurrentState.getChildren()) {
      // If the successor is equal to the startState, a cycle has been found.
      // Store contents of stack in the final result.
      if (successor.equals(pStartState)) {
        List<ARGState> cycle = new ArrayList<>();
        pStack.push(pStartState);
        cycle.addAll(pStack);
        Collections.reverse(cycle);
        pStack.pop();
        pAllCycles.add(cycle);
        foundCycle = true;
      } else if (!pBlockedSet.contains(successor)) {
        // Explore this successor only if it is not already in the blocked set.
        boolean gotCycle =
            findCyclesInSCC(
                pStartState, successor, pBlockedSet, pBlockedMap, pStack, pAllCycles, pExcludeSet);
        foundCycle = foundCycle || gotCycle;
      }
    }

    if (foundCycle) {
      unblock(pCurrentState, pBlockedSet, pBlockedMap);
    } else {
      for (ARGState s : pCurrentState.getChildren()) {
        pBlockedMap.put(s, pCurrentState);
      }
    }
    pStack.pop();

    return foundCycle;
  }

  private static void unblock(
      ARGState pCurrentState,
      Set<ARGState> pBlockedSet,
      SetMultimap<ARGState, ARGState> pBlockedMap) {
    pBlockedSet.remove(pCurrentState);
    pBlockedMap
        .get(pCurrentState)
        .forEach(
            state -> {
              if (pBlockedSet.contains(state)) {
                unblock(state, pBlockedSet, pBlockedMap);
              }
            });
  }

  /**
   * Find all strongly connected components recursively within the reached set using Tarjan's
   * algorithm.
   *
   * <p>The algorithm is based on a depth-first-search, so the respective procedure is called only
   * once for each node. The time complexity is thus linear in the number of nodes and edges, i.e.
   * O(|N| + |E|)
   *
   * @param pReached the reached set.
   * @return A set containing all {@link StronglyConnectedComponent}s. This set also includes SCCs
   *     that only consists of a single ARGState.
   */
  public static ImmutableSet<StronglyConnectedComponent> retrieveSCCs(ReachedSet pReached) {
    checkNotNull(pReached);

    ImmutableList<ARGState> argStates =
        transformedImmutableListCopy(pReached.asCollection(), x -> (ARGState) x);

    return retrieveSCCs(argStates, ImmutableSet.of());
  }

  private static ImmutableSet<StronglyConnectedComponent> retrieveSCCs(
      List<ARGState> pARGStates, Collection<ARGState> pExcludeStates) {
    checkNotNull(pARGStates);

    List<StronglyConnectedComponent> SCCs = new ArrayList<>();

    int index = 0;

    Deque<ARGState> dfsStack = new ArrayDeque<>();
    Map<ARGState, Integer> stateIndex = new HashMap<>();

    // Map to store the topmost reachable ancestor with the minimum possible index value
    Map<ARGState, Integer> stateLowLink = new HashMap<>();

    for (ARGState state : pARGStates) {
      if (pExcludeStates.contains(state)) {
        continue;
      }
      if (!stateIndex.containsKey(state)) {
        strongConnect(state, index, stateIndex, stateLowLink, dfsStack, SCCs, pExcludeStates);
      }
    }
    Collections.reverse(SCCs);
    return ImmutableSet.copyOf(SCCs);
  }

  /** Recursively find {@link StronglyConnectedComponent}s using DFS traversal */
  private static void strongConnect(
      ARGState pState,
      int pIndex,
      Map<ARGState, Integer> pStateIndex,
      Map<ARGState, Integer> pStateLowLink,
      Deque<ARGState> pDfsStack,
      List<StronglyConnectedComponent> pSCCs,
      Collection<ARGState> pExcludeStates) {

    pStateIndex.put(pState, pIndex);
    pStateLowLink.put(pState, pIndex);
    pIndex++;
    pDfsStack.push(pState);

    for (ARGState sucessorState : pState.getChildren()) {
      if (pExcludeStates.contains(sucessorState)) {
        continue;
      }
      if (!pStateIndex.containsKey(sucessorState)) {
        // Successor has not yet been visited; recurse on it
        strongConnect(
            sucessorState, pIndex, pStateIndex, pStateLowLink, pDfsStack, pSCCs, pExcludeStates);
        pStateLowLink.put(
            pState, Math.min(pStateLowLink.get(pState), pStateLowLink.get(sucessorState)));
      } else if (pDfsStack.contains(sucessorState)) {
        // Successor is in the stack ('dfsNodeStack') and hence in the current SCC
        // Otherwise, if the sucessorState is not on the stack, then (pState, sucessorState) is a
        // cross-edge (not a back edge) in the DFS tree and thus it must be ignored
        pStateLowLink.put(
            pState, Math.min(pStateLowLink.get(pState), pStateIndex.get(sucessorState)));
      }
    }

    // If pState is a root node, pop the stack and generate an SCC
    if (pStateIndex.get(pState).intValue() == pStateLowLink.get(pState).intValue()) {
      ARGState s;
      StronglyConnectedComponent scc = new StronglyConnectedComponent(pState);
      do {
        s = pDfsStack.pop();
        scc.addNode(s);
      } while (!Objects.equals(pState, s));
      pSCCs.add(scc);
    }
  }
}
