// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.junit.Test;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssAllWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class DssBlockMergingTest {

  private static final String DIAMONDS =
      """
      extern int __VERIFIER_nondet_int(void);
      int main() {
        int x;
        if (__VERIFIER_nondet_int()) { x = 1; } else { x = 2; }
        x = x + 1;
        if (__VERIFIER_nondet_int()) { x = x + 3; } else { x = x + 4; }
        x = x + 5;
        return x;
      }
      """;

  private static DssBlockAnalysis analysis(CFA pCfa, String pDomain, boolean pPreserve)
      throws Exception {
    return analysis(pCfa, pDomain, pPreserve, false);
  }

  private static DssBlockAnalysis analysis(
      CFA pCfa, String pDomain, boolean pPreserve, boolean pGhost) throws Exception {
    return analysis(pCfa, pDomain, pPreserve, pGhost, false);
  }

  private static DssBlockAnalysis analysis(
      CFA pCfa, String pDomain, boolean pPreserve, boolean pGhost, boolean pAggregate)
      throws Exception {
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromFile("config/distributed-summary-synthesis/dss-block-analysis.properties")
            .setOption(
                "CompositeCPA.cpas",
                "cpa.block.BlockCPA, cpa.location.LocationCPA, cpa.callstack.DssCallstackCPA,"
                    + " cpa.functionpointer.FunctionPointerCPA, cpa.predicate.PredicateCPA")
            .setOption("cpa.block.domain", pDomain)
            .setOption("cpa.arg.preservePaths", Boolean.toString(pPreserve))
            .setOption("analysis.algorithm.CEGAR", "false")
            .setOption("analysis.traversal.order", "bfs")
            .setOption("cpa.composite.aggregateBasicBlocks", Boolean.toString(pAggregate))
            .setOption("solver.solver", "SMTINTERPOL")
            .setOption("cpa.predicate.encodeBitvectorAs", "INTEGER")
            .setOption("cpa.predicate.encodeFloatAs", "RATIONAL")
            .build();
    CFANode end = pCfa.getMainFunction().getExitNode().orElseThrow();
    CFANode ghost = end;
    if (pGhost) {
      ghost = CFANode.newDummyCFANode("main");
      BlankEdge edge =
          new BlankEdge("", FileLocation.DUMMY, end, ghost, BlockGraph.GHOST_EDGE_DESCRIPTION);
      end.addLeavingEdge(edge);
      ghost.addEnteringEdge(edge);
    }
    BlockNode block =
        new BlockNode(
            "B",
            pCfa.getMainFunction(),
            end,
            ImmutableSet.<CFANode>builder().addAll(pCfa.nodes()).add(ghost).build(),
            CFAUtils.allEdges(pCfa).toSet(),
            ImmutableSet.of(),
            ImmutableSet.of(),
            ghost);
    DssAnalysisOptions options = new DssAnalysisOptions(config);
    return new DssBlockAnalysis(
        LogManager.createTestLogManager(),
        block,
        pCfa,
        Specification.alwaysSatisfied(),
        config,
        options,
        new DssMessageFactory(options),
        ShutdownManager.create(),
        new DssAllWorkerStatistics(false).createWorkerStats("test"));
  }

  private record Run(int states, int joins, Set<String> paths) {}

  private static Run run(CFA pCfa, String pDomain) throws Exception {
    return run(pCfa, pDomain, false);
  }

  private static Run run(CFA pCfa, String pDomain, boolean pAggregate) throws Exception {
    DssBlockAnalysis analysis = analysis(pCfa, pDomain, true, false, pAggregate);
    var result =
        analysis.runInitialBlockAnalysis(
            analysis.makeStartState(false), analysis.makeStartPrecision());
    Set<ArgPathAndCondition> paths = analysis.pathsFromOrigin(result.getFinalLocationStates());
    ImmutableSet.Builder<ARGState> states = ImmutableSet.builder();
    for (ArgPathAndCondition path : paths) {
      states.addAll(path.paths().iterator().next().getFirstState().getSubgraph());
    }
    var graph = states.build();
    return new Run(
        graph.size(),
        (int) graph.stream().filter(s -> s.getParents().size() > 1).count(),
        paths.stream()
            .flatMap(p -> com.google.common.collect.Streams.stream(p.paths()))
            .map(p -> p.getFullPath().toString())
            .collect(ImmutableSet.toImmutableSet()));
  }

  @Test
  public void predicateMergingSharesSuffixesAndPreservesAllDiamondPaths() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString(DIAMONDS);
    Run identity = run(cfa, "IDENTITY");
    Run value = run(cfa, "VALUE");
    assertThat(identity.paths()).hasSize(4);
    assertThat(value.paths()).containsExactlyElementsIn(identity.paths());
    assertThat(value.joins()).isGreaterThan(0);
    assertThat(value.states()).isLessThan(identity.states());
  }

  @Test
  public void aggregationPreservesTheExactDiamondEdges() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString(DIAMONDS);
    Run explicit = run(cfa, "VALUE", false);
    Run aggregated = run(cfa, "VALUE", true);
    assertThat(aggregated.paths()).containsExactlyElementsIn(explicit.paths());
    assertThat(aggregated.states()).isLessThan(explicit.states());
  }

  @Test
  public void repeatedBlockRunsHaveFreshProcessingAndReplayState() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString(DIAMONDS);
    DssBlockAnalysis analysis = analysis(cfa, "VALUE", true);
    var initial = analysis.makeStartState(false);
    var precision = analysis.makeStartPrecision();
    var first = analysis.runInitialBlockAnalysis(analysis.getDcpa().reset(initial), precision);
    var firstPaths = analysis.pathsFromOrigin(first.getFinalLocationStates());
    var second = analysis.runInitialBlockAnalysis(analysis.getDcpa().reset(initial), precision);
    assertThat(analysis.pathsFromOrigin(second.getFinalLocationStates()))
        .hasSize(firstPaths.size());
    assertThat(DssBlockAnalysis.blockStateOf(initial).getViolationConditions()).isEmpty();
  }

  @Test
  public void valueDomainRequiresPathPreservation() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString("int main() { return 0; }");
    assertThrows(InvalidConfigurationException.class, () -> analysis(cfa, "VALUE", false));
  }

  @Test
  public void everyViolationConditionKeepsItsOwnGhostObligation() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString(DIAMONDS);
    DssBlockAnalysis analysis = analysis(cfa, "VALUE", true, true);
    var initial = analysis.makeStartState(false);
    CFANode end = cfa.getMainFunction().getExitNode().orElseThrow();
    var firstCondition =
        analysis.getDcpa().getInitialState(end, StateSpacePartition.getDefaultPartition());
    var secondCondition =
        analysis.getDcpa().getInitialState(end, StateSpacePartition.getDefaultPartition());
    var conditions = ImmutableList.of(firstCondition, secondCondition);
    var result = analysis.runBlockAnalysis(initial, analysis.makeStartPrecision(), conditions);
    var ghosts = result.getViolationConditionViolations();
    assertThat(ghosts).hasSize(2);
    assertThat(
            ghosts.stream()
                .flatMap(g -> DssBlockAnalysis.blockStateOf(g).getViolationConditions().stream())
                .toList())
        .containsExactlyElementsIn(conditions);
    for (ARGState ghost : ghosts) {
      var predecessor = DssBlockAnalysis.blockStateOf(ghost).getPredecessor();
      assertThat(predecessor.getViolationConditions()).containsExactlyElementsIn(conditions);
      assertThat(predecessor.getPendingViolationConditions()).isEmpty();
    }
    assertThat(DssBlockAnalysis.blockStateOf(initial).getViolationConditions()).isEmpty();
  }
}
