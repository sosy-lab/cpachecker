// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class SccFinderTest {

  @Test
  public void emptyGraph_producesNoSCCs() {
    ImmutableSet<StronglyConnectedComponent<Object>> sccs =
        SccFinder.findSCCs(ImmutableList.of(), ImmutableListMultimap.of()::get);
    assertThat(sccs).isEmpty();
  }

  @Test
  public void singleNodeNoSelfLoop_isOwnSCC() {
    ImmutableSet<StronglyConnectedComponent<Integer>> sccs =
        SccFinder.findSCCs(ImmutableList.of(1), ImmutableListMultimap.<Integer, Integer>of()::get);

    assertThat(sccs).hasSize(1);
    assertThat(getOnlyNodes(sccs)).containsExactly(1);
  }

  @Test
  public void acyclicChain_everyNodeIsItsOwnSCC() {
    // 1 -> 2 -> 3, no cycles
    ImmutableListMultimap<Integer, Integer> adj = ImmutableListMultimap.of(1, 2, 2, 3);

    ImmutableSet<StronglyConnectedComponent<Integer>> sccs =
        SccFinder.findSCCs(ImmutableList.of(1, 2, 3), adj::get);

    assertThat(sccs).hasSize(3);
    assertThat(getAllNodeSets(sccs)).containsExactly(Set.of(1), Set.of(2), Set.of(3));
  }

  @Test
  public void selfLoop_isSingleNodeSCC() {
    ImmutableListMultimap<Integer, Integer> adj = ImmutableListMultimap.of(1, 1);

    ImmutableSet<StronglyConnectedComponent<Integer>> sccs =
        SccFinder.findSCCs(ImmutableList.of(1), adj::get);

    assertThat(sccs).hasSize(1);
    assertThat(getOnlyNodes(sccs)).containsExactly(1);
  }

  @Test
  public void simpleTwoCycle_isOneSCC() {
    // 1 <-> 2
    ImmutableListMultimap<Integer, Integer> adj = ImmutableListMultimap.of(1, 2, 2, 1);

    ImmutableSet<StronglyConnectedComponent<Integer>> sccs =
        SccFinder.findSCCs(ImmutableList.of(1, 2), adj::get);

    assertThat(sccs).hasSize(1);
    assertThat(getOnlyNodes(sccs)).containsExactly(1, 2);
  }

  @Test
  public void twoDisjointCycles_areTwoSCCs() {
    // 1 <-> 2   and   3 <-> 4
    ImmutableListMultimap<Integer, Integer> adj = ImmutableListMultimap.of(1, 2, 2, 1, 3, 4, 4, 3);

    ImmutableSet<StronglyConnectedComponent<Integer>> sccs =
        SccFinder.findSCCs(ImmutableList.of(1, 2, 3, 4), adj::get);

    assertThat(sccs).hasSize(2);
    assertThat(getAllNodeSets(sccs)).containsExactly(Set.of(1, 2), Set.of(3, 4));
  }

  @Test
  public void excludedNodes_areIgnoredEntirely() {
    // 1 <-> 2, but 2 is excluded -> only {1} should remain (and be its own trivial SCC)
    ImmutableListMultimap<Integer, Integer> adj = ImmutableListMultimap.of(1, 2, 2, 1);

    ImmutableSet<StronglyConnectedComponent<Integer>> sccs =
        SccFinder.findSCCs(ImmutableList.of(1, 2), adj::get, ImmutableList.of(2));

    assertThat(sccs).hasSize(1);
    assertThat(getOnlyNodes(sccs)).containsExactly(1);
  }

  // ---------------------------------------------------------------------
  // Regression test for the index-collision bug
  // ---------------------------------------------------------------------

  /**
   * Reproduces the bug in the original implementation: {@code pIndex} was passed by value into the
   * recursion and never propagated back to the caller's frame, so the same index value could be
   * handed out to two different nodes if the DFS explored a second branch after returning from a
   * deep recursive branch.
   *
   * <p>Graph: A -> B, B -> A, A -> C, C -> D, D -> B
   *
   * <p>Every node can reach every other node (A->B->A is a cycle; A->C->D->B->A closes the loop
   * through the rest), so the whole graph is exactly one SCC of size 4.
   *
   * <p>With the buggy by-value index counter: after returning from the A->B branch, A's local
   * {@code pIndex} is stale (it never saw B's internal increments). Recursing into C then reuses
   * that stale value, which happens to collide with B's real index. This causes C's lowlink
   * computation (via D's back-edge to B) to falsely satisfy the "is a root" check, incorrectly
   * splitting off {C, D} as their own SCC instead of merging everything into one component.
   *
   * <p>Successor order matters to reproduce the bug: A's successors must be visited as [B, C], i.e.
   * the cyclic-but-shallow branch (B) before the deeper branch (C -> D -> B). An {@link
   * ImmutableListMultimap} preserves insertion order per key, which is what makes this reliable.
   */
  @Test
  public void bug_crossBranchIndexCollision_wholeGraphIsOneSCC() {
    final int a = 1;
    final int b = 2;
    final int c = 3;
    final int d = 4;

    ImmutableListMultimap<Integer, Integer> adj =
        ImmutableListMultimap.<Integer, Integer>builder()
            .putAll(a, b, c)
            .put(b, a)
            .put(c, d)
            .put(d, b)
            .build();

    ImmutableSet<StronglyConnectedComponent<Integer>> sccs =
        SccFinder.findSCCs(ImmutableList.of(a, b, c, d), adj::get);

    // The buggy implementation would produce two SCCs here: {C, D} and {A, B}.
    assertThat(sccs).hasSize(1);
    assertThat(getOnlyNodes(sccs)).containsExactly(a, b, c, d);
  }

  /**
   * A slightly larger variant of the same collision pattern, with an extra branch fanning out of A
   * before the deep C->D->B branch, to make sure the fix holds up with more siblings consuming
   * indices in between (not just a single sibling).
   */
  @Test
  public void bug_crossBranchIndexCollision_withExtraSiblingBranch() {
    final int a = 1;
    final int b = 2;
    final int c = 3;
    final int d = 4;
    final int e = 5; // extra acyclic sibling branch off A

    ImmutableListMultimap<Integer, Integer> adj =
        ImmutableListMultimap.<Integer, Integer>builder()
            .putAll(a, b, e, c)
            .put(b, a)
            .put(c, d)
            .put(d, b)
            .build();

    ImmutableSet<StronglyConnectedComponent<Integer>> sccs =
        SccFinder.findSCCs(ImmutableList.of(a, b, c, d, e), adj::get);

    assertThat(sccs).hasSize(2);
    assertThat(getAllNodeSets(sccs)).containsExactly(Set.of(e), Set.of(a, b, c, d));
  }

  // ---------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------

  private static Set<Integer> getOnlyNodes(ImmutableSet<StronglyConnectedComponent<Integer>> sccs) {
    assertThat(sccs).hasSize(1);
    return sccs.iterator().next().getNodes();
  }

  private static Set<Set<Integer>> getAllNodeSets(
      ImmutableSet<StronglyConnectedComponent<Integer>> sccs) {
    Set<Set<Integer>> result = new HashSet<>();
    for (StronglyConnectedComponent<Integer> scc : sccs) {
      result.add(scc.getNodes());
    }
    return result;
  }
}
