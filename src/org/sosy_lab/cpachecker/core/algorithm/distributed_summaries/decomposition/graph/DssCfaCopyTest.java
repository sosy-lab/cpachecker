// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMutableNetwork;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.DssDecompositionOptions;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class DssCfaCopyTest {

  private Configuration config;

  @Before
  public void setUp() throws InvalidConfigurationException {
    config = TestUtils.configurationForTest().build();
  }

  private static final LogManager LOGGER = LogManager.createTestLogManager();

  private MutableCFA original() throws Exception {
    return MutableCFA.copyOf(
        TestCfaUtils.makeCfaFromString("void f(void) {} int main(void) { f(); return 0; }"),
        config,
        LOGGER);
  }

  private static CFunctionCallEdge call(CFA pCfa) {
    return pCfa.nodes().stream()
        .flatMap(node -> node.getAllLeavingEdges().stream())
        .filter(CFunctionCallEdge.class::isInstance)
        .map(CFunctionCallEdge.class::cast)
        .findFirst()
        .orElseThrow();
  }

  private static CFunctionSummaryEdge addOrphan(MutableCFA pCfa, boolean pWithReturn) {
    CFunctionCallEdge call = call(pCfa);
    CFANode caller = new CFANode(pCfa.getMainFunction().getFunction());
    CFANode afterCall = new CFANode(caller.getFunction());
    pCfa.addNode(caller);
    pCfa.addNode(afterCall);
    CFunctionSummaryEdge summary =
        new CFunctionSummaryEdge(
            call.getRawStatement(),
            FileLocation.DUMMY,
            caller,
            afterCall,
            call.getFunctionCall(),
            call.getSuccessor());
    caller.addLeavingSummaryEdge(summary);
    afterCall.addEnteringSummaryEdge(summary);
    if (pWithReturn) {
      CFunctionReturnEdge returning =
          new CFunctionReturnEdge(
              FileLocation.DUMMY,
              call.getSuccessor().getExitNode().orElseThrow(),
              afterCall,
              summary);
      returning.getPredecessor().addLeavingEdge(returning);
      afterCall.addEnteringEdge(returning);
    }
    return summary;
  }

  private static Set<CFANode> reachable(CFA pCfa) {
    return reachable(pCfa, true);
  }

  private static Set<CFANode> reachable(CFA pCfa, boolean pFollowReturns) {
    CfaMutableNetwork graph = CfaMutableNetwork.of(pCfa);
    Set<CFANode> reached = new HashSet<>();
    Deque<CFANode> waiting = new ArrayDeque<>();
    reached.add(pCfa.getMainFunction());
    waiting.add(pCfa.getMainFunction());
    while (!waiting.isEmpty()) {
      for (CFAEdge edge : graph.outEdges(waiting.removeFirst())) {
        if (!pFollowReturns && edge instanceof CFunctionReturnEdge) {
          continue;
        }
        CFANode next = graph.incidentNodes(edge).nodeV();
        if (reached.add(next)) {
          waiting.add(next);
        }
      }
    }
    return reached;
  }

  @Test
  public void wellFormedCopyPreservesEdges() throws Exception {
    CFA original = original().immutableCopy();
    CFA copy = DssCfaCopy.copyOf(original, config, LOGGER);
    assertThat(CfaMutableNetwork.of(copy).edges())
        .hasSize(CfaMutableNetwork.of(original).edges().size());
    assertThat(copy.nodes()).containsNoneIn(original.nodes());
    assertThat(reachable(copy)).hasSize(reachable(original).size());
  }

  @Test
  public void repairsOnlyThePrivateCopyOfAnUnreachableCallSite() throws Exception {
    MutableCFA mutable = original();
    CFunctionSummaryEdge orphan = addOrphan(mutable, true);
    CFA original = mutable.immutableCopy();
    ImmutableSet<CFAEdge> originalEdges =
        ImmutableSet.copyOf(CfaMutableNetwork.of(original).edges());
    assertThat(reachable(original)).doesNotContain(orphan.getPredecessor());
    assertThrows(IllegalStateException.class, () -> MutableCFA.copyOf(original, config, LOGGER));

    CFA copy = DssCfaCopy.copyOf(original, config, LOGGER);
    Set<CFANode> copiedReachable = reachable(copy);
    assertThat(copiedReachable).hasSize(reachable(original).size());
    assertThat(
            copy.nodes().stream()
                .filter(node -> !copiedReachable.contains(node))
                .flatMap(node -> node.getAllLeavingEdges().stream())
                .filter(CFunctionCallEdge.class::isInstance)
                .count())
        .isEqualTo(1);
    assertThat(CfaMutableNetwork.of(copy).edges()).hasSize(originalEdges.size() + 1);
    assertThat(CfaMutableNetwork.of(original).edges()).containsExactlyElementsIn(originalEdges);
    assertThat(orphan.getPredecessor().getNumLeavingEdges()).isEqualTo(0);
    assertThrows(IllegalStateException.class, () -> MutableCFA.copyOf(original, config, LOGGER));

    Configuration dssConfig =
        Configuration.builder().loadFromFile("config/dss-plain.properties").build();
    BlockGraph blocks =
        new DssDecompositionOptions(dssConfig, original)
            .getConfiguredDecomposition()
            .decompose(original);
    assertThat(blocks.getNodes().size()).isGreaterThan(1);
    BlockGraphModification.Modification instrumented =
        BlockGraphModification.instrumentCFA(original, blocks, dssConfig, LOGGER);
    assertThat(instrumented.metadata().originalCfa()).isSameInstanceAs(original);
    assertThat(instrumented.blockGraph().getNodes()).hasSize(blocks.getNodes().size());
  }

  @Test
  public void unmatchedReturnsDoNotMakeAnotherOrphanReachable() throws Exception {
    MutableCFA mutable = original();
    CFunctionSummaryEdge first = addOrphan(mutable, true);
    CFunctionSummaryEdge second = addOrphan(mutable, true);
    BlankEdge continuation =
        new BlankEdge(
            "",
            first.getFileLocation(),
            first.getSuccessor(),
            second.getPredecessor(),
            "continuation");
    first.getSuccessor().addLeavingEdge(continuation);
    second.getPredecessor().addEnteringEdge(continuation);
    CFA original = mutable.immutableCopy();
    assertThat(reachable(original)).contains(second.getPredecessor());
    assertThat(reachable(original, false)).doesNotContain(second.getPredecessor());
    CFA copy = DssCfaCopy.copyOf(original, config, LOGGER);
    assertThat(reachable(copy, false)).hasSize(reachable(original, false).size());
    assertThat(CfaMutableNetwork.of(copy).edges())
        .hasSize(CfaMutableNetwork.of(original).edges().size() + 2);
    assertThat(first.getPredecessor().getNumLeavingEdges()).isEqualTo(0);
    assertThat(second.getPredecessor().getNumLeavingEdges()).isEqualTo(0);
  }

  @Test
  public void handlesCallsAfterANonReturningFunction() throws Exception {
    CFA original =
        TestCfaUtils.makeCfaFromString(
            "extern void abort(void); void f(void) {} void stop(void) { abort(); } "
                + "int main(void) { stop(); f(); return 0; }");
    // The parser removes the call following stop(), but retains its summary edge.
    assertThrows(IllegalStateException.class, () -> MutableCFA.copyOf(original, config, LOGGER));
    CFA copy = DssCfaCopy.copyOf(original, config, LOGGER);
    assertThat(copy.getMainFunction().getFunctionName()).isEqualTo("main");
    assertThat(copy.nodes()).containsNoneIn(original.nodes());
  }

  @Test
  public void rejectsAMissingCallAtAReachableLocation() throws Exception {
    MutableCFA mutable = original();
    CFunctionCallEdge call = call(mutable);
    call.getPredecessor().removeLeavingEdge(call);
    call.getSuccessor().removeEnteringEdge(call);
    CFA original = mutable.immutableCopy();
    IllegalStateException failure =
        assertThrows(
            IllegalStateException.class, () -> DssCfaCopy.copyOf(original, config, LOGGER));
    assertThat(failure).hasMessageThat().contains("reachable call site");
  }

  @Test
  public void handlesAnUnreachableSummaryWithoutAReturn() throws Exception {
    MutableCFA mutable = original();
    CFunctionSummaryEdge orphan = addOrphan(mutable, false);
    CFA original = mutable.immutableCopy();
    CFA copy = DssCfaCopy.copyOf(original, config, LOGGER);
    assertThat(reachable(copy, false)).hasSize(reachable(original, false).size());
    assertThat(orphan.getPredecessor().getNumLeavingEdges()).isEqualTo(0);
  }

  @Test
  public void laterCallsToAnAlreadyReachedCalleeStillReturn() throws Exception {
    MutableCFA mutable =
        MutableCFA.copyOf(
            TestCfaUtils.makeCfaFromString(
                "void f(void) {}\nint main(void) {\nf();\nf();\nf();\nreturn 0; }"),
            config,
            LOGGER);
    CFunctionCallEdge last =
        mutable.nodes().stream()
            .flatMap(node -> node.getAllLeavingEdges().stream())
            .filter(CFunctionCallEdge.class::isInstance)
            .map(CFunctionCallEdge.class::cast)
            .filter(edge -> edge.getFileLocation().getStartingLineNumber() == 5)
            .findFirst()
            .orElseThrow();
    last.getPredecessor().removeLeavingEdge(last);
    last.getSuccessor().removeEnteringEdge(last);
    CFA original = mutable.immutableCopy();
    IllegalStateException failure =
        assertThrows(
            IllegalStateException.class, () -> DssCfaCopy.copyOf(original, config, LOGGER));
    assertThat(failure).hasMessageThat().contains("reachable call site");
  }
}
