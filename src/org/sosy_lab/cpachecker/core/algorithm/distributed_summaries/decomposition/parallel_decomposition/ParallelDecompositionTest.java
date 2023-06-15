// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.parallel_decomposition;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class ParallelDecompositionTest {

  /**
   * Test that ParallelBlockNodeDecomposition decomposes a CFA into blocks that each represent a
   * single function
   */
  @Test
  public void testDecomposeMinimal()
      throws InvalidConfigurationException, ParserException, InterruptedException {
    final Configuration config =
        TestDataTools.configurationForTest().setOption("language", "C").build();
    final CFACreator creator = createTestingConfig(config);
    final CFA created = creator.parseSourceAndCreateCFA("void main() { return; }");
    final ParallelBlockNodeDecomposition decomposer = new ParallelBlockNodeDecomposition();
    final BlockGraph decomposed = decomposer.decompose(created);
    assertThat(decomposed.getNodes().size()).isEqualTo(1);
    for (BlockNode n : decomposed.getNodes()) {
      n.getEdges()
          .forEach(e -> assertThat(e.getEdgeType()).isNotEqualTo(CFAEdgeType.FunctionCallEdge));
    }
  }

  @Test
  public void testDecomposeMulti()
      throws InvalidConfigurationException, ParserException, InterruptedException {
    final Configuration config =
        TestDataTools.configurationForTest().setOption("language", "C").build();
    final CFACreator creator = createTestingConfig(config);
    final String program =
        "void main() { return; } int foo(int x) { if (x > 0) { return 1; } else { return 0; }} int"
            + " bar(){ return 1; }";
    final CFA created = creator.parseSourceAndCreateCFA(program);
    final ParallelBlockNodeDecomposition decomposer = new ParallelBlockNodeDecomposition();
    final BlockGraph decomposed = decomposer.decompose(created);
    assertThat(decomposed.getNodes().size()).isEqualTo(3);
    for (BlockNode n : decomposed.getNodes()) {
      n.getEdges()
          .forEach(e -> assertThat(e.getEdgeType()).isNotEqualTo(CFAEdgeType.FunctionCallEdge));
    }
  }

  @Test
  public void testDecomposeNested()
      throws InvalidConfigurationException, ParserException, InterruptedException {
    final Configuration config =
        TestDataTools.configurationForTest().setOption("language", "C").build();
    final CFACreator creator = createTestingConfig(config);
    final String program =
        "void main() { return; } int foo(int x) { if (x > 0) { return bar(); } else { return 0; }}"
            + " int bar(){ return 1; }";
    final CFA created = creator.parseSourceAndCreateCFA(program);
    final ParallelBlockNodeDecomposition decomposer = new ParallelBlockNodeDecomposition();
    final BlockGraph decomposed = decomposer.decompose(created);
    for (BlockNode n : decomposed.getNodes()) {
      n.getEdges()
          .forEach(e -> assertThat(e.getEdgeType()).isNotEqualTo(CFAEdgeType.FunctionCallEdge));
      if (n.getFirst().getFunctionName().equals("bar")) {
        assertThat(getReturnEdgeCount(n)).isEqualTo(1);
      } else {
        assertThat(getReturnEdgeCount(n)).isEqualTo(0);
      }
    }
  }

  private CFACreator createTestingConfig(Configuration config)
      throws InvalidConfigurationException {
    final LogManager logger = LogManager.createTestLogManager();
    final ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    return new CFACreator(config, logger, shutdownNotifier);
  }

  private int getReturnEdgeCount(BlockNode n) {
    int count = 0;
    for (CFAEdge e : n.getEdges()) {
      if (e.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
        count++;
      }
    }
    return count;
  }
}
