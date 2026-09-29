// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;

public class DssDecompositionOptionsTest {
  @Test
  public void decompositionBoundariesCanDifferFromWitnessAbstractions() throws Exception {
    CFA cfa = TestCfaUtils.makeCfaFromString("int main(int x) { if (x) x++; else x--; return x; }");
    Configuration base =
        Configuration.builder()
            .loadFromFile("config/distributed-summary-synthesis/dss-base.properties")
            .setOption(
                "distributedSummaries.decomposition.decompositionType", "LINEAR_DECOMPOSITION")
            .build();
    Configuration config =
        Configuration.builder()
            .copyFrom(base)
            .setOption("cpa.predicate.blk.alwaysAtBranch", "false")
            .setOption(
                "distributedSummaries.decomposition.cpa.predicate.blk.alwaysAtBranch", "true")
            .build();
    BlockGraph expected =
        new DssDecompositionOptions(base, cfa).getConfiguredDecomposition().decompose(cfa);
    BlockGraph actual =
        new DssDecompositionOptions(config, cfa).getConfiguredDecomposition().decompose(cfa);
    assertThat(actual.getNodes().stream().map(b -> b.getEdges()).toList())
        .containsExactlyElementsIn(expected.getNodes().stream().map(b -> b.getEdges()).toList());
    BlockOperator replay = new BlockOperator();
    config.inject(replay);
    replay.setCFA(cfa);
    CFANode branch =
        cfa.nodes().stream().filter(n -> n.getNumLeavingEdges() == 2).findFirst().orElseThrow();
    assertThat(replay.isBlockEnd(branch, 0)).isFalse();
    assertThat(actual.getNodes().stream().anyMatch(b -> b.getFinalLocation().equals(branch)))
        .isTrue();
  }
}
