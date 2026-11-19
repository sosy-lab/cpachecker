// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pathrestriction;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.pathrestriction.DecisionGraph.PathPosition;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

public final class DecisionGraphTest {

  /** The two edges leaving the branching node of {@code if (condition)}. */
  private record Branch(CFAEdge taken, CFAEdge other) {}

  private CFA cfa;
  private Branch a;
  private Branch b;
  private Branch c;

  /** An edge where no other edge could have been taken. */
  private CFAEdge straight;

  @Before
  public void setUp() throws Exception {
    cfa =
        TestCfaUtils.makeCfaFromString(
            "int main(int a, int b, int c) { int x = 0; if (a) x = 1; else x = 2;"
                + " if (b) x = 3; else x = 4; if (c) x = 5; else x = 6; return x; }");
    a = branch("a");
    b = branch("b");
    c = branch("c");
    straight = edge("x = 1;");
  }

  private CFAEdge edge(String rawStatement) {
    return Iterables.getOnlyElement(
        CFAUtils.allEdges(cfa).filter(e -> e.getRawStatement().equals(rawStatement)));
  }

  private Branch branch(String condition) {
    return new Branch(edge("[" + condition + "]"), edge("[!(" + condition + ")]"));
  }

  /** Whether the graph permits taking {@code edges} in this order. */
  private static boolean permits(DecisionGraph graph, CFAEdge... edges) {
    PathPosition position = graph.start();
    for (CFAEdge edge : edges) {
      position = position.advance(edge);
      if (position.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private static DecisionGraph path(CFAEdge... edges) {
    return DecisionGraph.UNRESTRICTED.prepend(ImmutableList.copyOf(edges));
  }

  @Test
  public void unrestricted_permitsEverything() {
    assertThat(permits(DecisionGraph.UNRESTRICTED, a.taken(), b.other())).isTrue();
    assertThat(permits(DecisionGraph.UNRESTRICTED, a.other(), b.taken())).isTrue();
    assertThat(DecisionGraph.UNRESTRICTED.maxDecisions()).isEqualTo(0);
  }

  @Test
  public void prepend_restrictsDecisionEdges() {
    DecisionGraph graph = path(a.taken(), straight);

    assertThat(graph.maxDecisions()).isEqualTo(1);
    assertThat(permits(graph, a.taken())).isTrue();
    assertThat(permits(graph, a.other())).isFalse();
  }

  @Test
  public void prepend_ignoresEdgesWithoutAlternative() {
    DecisionGraph graph = path(straight);

    assertThat(graph.maxDecisions()).isEqualTo(0);
    assertThat(permits(graph, a.taken())).isTrue();
    assertThat(permits(graph, a.other())).isTrue();
  }

  @Test
  public void prepend_putsNewDecisionsBeforeExistingOnes() {
    DecisionGraph graph = path(b.taken()).prepend(ImmutableList.of(a.taken()));

    assertThat(graph.maxDecisions()).isEqualTo(2);
    assertThat(permits(graph, a.taken(), b.taken())).isTrue();
    assertThat(permits(graph, a.taken(), b.other())).isFalse();
    assertThat(permits(graph, b.taken())).isFalse();
  }

  @Test
  public void advance_ignoresEdgesWithoutAlternative() {
    assertThat(permits(path(a.taken()), straight, a.taken())).isTrue();
  }

  @Test
  public void advance_permitsEverythingAfterThePathEnds() {
    DecisionGraph graph = path(a.taken());

    assertThat(permits(graph, a.taken(), b.taken())).isTrue();
    assertThat(permits(graph, a.taken(), b.other())).isTrue();
  }

  @Test
  public void union_permitsThePathsOfEveryChoice() {
    // Different continuations after the first decision, e.g. from different successor blocks.
    DecisionGraph viaTaken = path(a.taken(), b.taken());
    DecisionGraph viaOther = path(a.other(), b.other());

    DecisionGraph union = DecisionGraph.union(ImmutableList.of(viaTaken, viaOther));

    assertThat(permits(union, a.taken(), b.taken())).isTrue();
    assertThat(permits(union, a.other(), b.other())).isTrue();
    assertThat(permits(union, a.taken(), b.other())).isFalse();
    assertThat(permits(union, a.other(), b.taken())).isFalse();
    assertThat(permits(union, c.taken())).isFalse();
  }

  @Test
  public void union_ofBranchesBeforeACommonContinuation_permitsEveryBranch() {
    // A block that branches before continuing with the witness of the previous condition.
    DecisionGraph previous = path(b.taken());

    DecisionGraph graph =
        DecisionGraph.union(
            ImmutableList.of(
                previous.prepend(ImmutableList.of(a.taken())),
                previous.prepend(ImmutableList.of(a.other()))));

    assertThat(graph.maxDecisions()).isEqualTo(2);
    assertThat(permits(graph, a.taken(), b.taken())).isTrue();
    assertThat(permits(graph, a.other(), b.taken())).isTrue();
    assertThat(permits(graph, a.taken(), b.other())).isFalse();
    assertThat(permits(graph, a.other(), b.other())).isFalse();
  }

  @Test
  public void union_ofSingleGraph_permitsTheSamePaths() {
    DecisionGraph union = DecisionGraph.union(ImmutableList.of(path(a.taken())));

    assertThat(union.maxDecisions()).isEqualTo(1);
    assertThat(permits(union, a.taken())).isTrue();
    assertThat(permits(union, a.other())).isFalse();
  }

  @Test
  public void union_withEmptyCollection_throws() {
    assertThrows(IllegalArgumentException.class, () -> DecisionGraph.union(ImmutableList.of()));
  }

  @Test
  public void equals_forGraphsBuiltFromTheSameEdges() {
    // Equal witnesses can be produced by different code paths, e.g. once via prepend and once
    // after being deserialized from a message.
    DecisionGraph first = path(a.taken(), straight, b.other());
    DecisionGraph second = path(a.taken(), straight, b.other());

    assertThat(first).isEqualTo(second);
    assertThat(first.hashCode()).isEqualTo(second.hashCode());
  }

  @Test
  public void equals_distinguishesDifferentDecisions() {
    assertThat(path(a.taken())).isNotEqualTo(path(a.other()));
  }

  @Test
  public void serialize_roundTrips() {
    DecisionGraph continuation = path(b.taken());
    List<DecisionGraph> graphs =
        ImmutableList.of(
            DecisionGraph.UNRESTRICTED,
            path(a.taken(), b.other()),
            DecisionGraph.union(
                ImmutableList.of(
                    continuation.prepend(ImmutableList.of(a.taken())),
                    continuation.prepend(ImmutableList.of(a.other())),
                    path(c.taken()))));

    for (DecisionGraph graph : graphs) {
      DecisionGraph deserialized = DecisionGraph.deserialize(graph.serialize());

      assertThat(deserialized).isEqualTo(graph);
      assertThat(deserialized.maxDecisions()).isEqualTo(graph.maxDecisions());
      for (CFAEdge first : ImmutableList.of(a.taken(), a.other(), c.taken())) {
        for (CFAEdge second : ImmutableList.of(b.taken(), b.other())) {
          assertThat(permits(deserialized, first, second)).isEqualTo(permits(graph, first, second));
        }
      }
    }
  }

  @Test
  public void transformEdges_replacesMappedEdgesAndLeavesOthersUnchanged() {
    DecisionGraph original = path(a.taken(), b.taken());

    DecisionGraph transformed = original.transformEdges(ImmutableMap.of(a.taken(), c.taken()));

    assertThat(permits(transformed, c.taken(), b.taken())).isTrue();
    assertThat(permits(transformed, c.taken(), b.other())).isFalse();
    assertThat(permits(transformed, a.taken())).isFalse();
  }

  @Test
  public void pathRestrictionCPA_followsTheGraph() throws Exception {
    PathRestrictionCPA cpa = (PathRestrictionCPA) PathRestrictionCPA.factory().createInstance();
    cpa.init(path(a.taken(), b.taken()));
    TransferRelation transfer = cpa.getTransferRelation();
    AbstractState initial =
        cpa.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());

    assertThat(successors(transfer, initial, a.other())).isEmpty();
    AbstractState next = Iterables.getOnlyElement(successors(transfer, initial, a.taken()));
    assertThat(successors(transfer, next, straight)).hasSize(1);
    assertThat(successors(transfer, next, b.taken())).hasSize(1);
    assertThat(successors(transfer, next, b.other())).isEmpty();
  }

  private static Collection<? extends AbstractState> successors(
      TransferRelation transfer, AbstractState state, CFAEdge edge) throws Exception {
    return transfer.getAbstractSuccessorsForEdge(state, SingletonPrecision.getInstance(), edge);
  }
}
