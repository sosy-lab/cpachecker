// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.truth.Truth;
import java.io.IOException;
import java.util.function.Predicate;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.TestUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class FunctionGraphTest {

  static final String PROGRAM_RECURSIVE = "test/programs/dss/function_graph_recursive.c";
  static final String PROGRAM_DAG = "test/programs/dss/function_graph_DAG.c";

  @Test
  public void testGraphIsPartitionDAG() throws Exception {

    BlockGraph blockGraph = createBlockGraph(PROGRAM_DAG);

    FunctionGraph funcGraph = FunctionGraph.from(blockGraph);

    Truth.assertWithMessage("Function Graph is not a partition")
        .that(
            FluentIterable.from(funcGraph.getFunctions())
                .transformAndConcat(f -> f.blockNodes())
                .toMultiset())
        .containsExactlyElementsIn(blockGraph.getNodes());
  }

  @Test
  public void testGraphIsPartitionRecursive() throws Exception {

    BlockGraph blockGraph = createBlockGraph(PROGRAM_RECURSIVE);

    FunctionGraph funcGraph = FunctionGraph.from(blockGraph);

    Truth.assertWithMessage("Function Graph is not a partition")
        .that(
            FluentIterable.from(funcGraph.getFunctions())
                .transformAndConcat(f -> f.blockNodes())
                .toMultiset())
        .containsExactlyElementsIn(blockGraph.getNodes());
  }

  @Test
  public void testGraphCorrectRecursive() throws Exception {

    BlockGraph blockGraph = createBlockGraph(PROGRAM_RECURSIVE);

    FunctionGraph funcGraph = FunctionGraph.from(blockGraph);

    ImmutableList<String> edges =
        FluentIterable.from(funcGraph.getEdges().entrySet())
            .transformAndConcat(
                e ->
                    FluentIterable.from(e.getValue())
                        .transform(c -> e.getKey().name() + " -> " + c.target().name()))
            .toList();

    Truth.assertThat(edges)
        .containsExactly(
            "main -> leaf1",
            "main -> leaf1",
            "main -> rec1",
            "main -> rec2",
            "rec1 -> leaf1",
            "rec1 -> rec1",
            "rec1 -> rec2",
            "rec2 -> leaf2",
            "rec2 -> rec3",
            "rec3 -> leaf2",
            "rec3 -> rec1");
  }

  @Test
  public void testGraphCorrectDAG() throws Exception {

    BlockGraph blockGraph = createBlockGraph(PROGRAM_DAG);

    FunctionGraph funcGraph = FunctionGraph.from(blockGraph);

    Truth.assertWithMessage("root not main, but %s instead", funcGraph.getRoot())
        .that(funcGraph.getRoot().name())
        .isEqualTo("main");

    Truth.assertThat(
            FluentIterable.from(funcGraph.getSuccessors(funcGraph.getRoot()))
                .transform(f -> f.name()))
        .containsExactly("f", "g");

    for (Function fun : funcGraph.getSuccessors(funcGraph.getRoot())) {
      if (fun.name().equals("f")) {
        Truth.assertThat(FluentIterable.from(funcGraph.getSuccessors(fun)).transform(f -> f.name()))
            .containsExactly("g");
      } else if (fun.name().equals("g")) {
        Function h = Iterables.getOnlyElement(FluentIterable.from(funcGraph.getSuccessors(fun)));

        Truth.assertThat(h.name()).isEqualTo("h");
        Truth.assertThat(funcGraph.getSuccessors(h)).isEmpty();
      }
    }
  }

  static BlockGraph createBlockGraph(String program)
      throws Exception, InvalidConfigurationException, IOException, InterruptedException {
    CFA cfa = TestUtil.buildTestCFA(program);

    BlockOperator blockOperator = new BlockOperator();
    Configuration config =
        TestDataTools.configurationForTest().loadFromFile(TestUtil.DSS_CONFIGURATION_FILE).build();
    config.inject(blockOperator);
    try {
      blockOperator.setCFA(cfa);
    } catch (CPAException e) {
      throw new InvalidConfigurationException("Initialization of block operator failed", e);
    }

    Predicate<CFANode> isBlockEnd = n -> blockOperator.isBlockEnd(n, -1);

    LinearBlockNodeDecomposition decomp = new LinearBlockNodeDecomposition(isBlockEnd);
    BlockGraph blockGraph = decomp.decompose(cfa);
    return blockGraph;
  }
}
