// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import com.google.common.truth.Truth;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining.FunctionSCCGraph.FunctionSCC;
import org.sosy_lab.cpachecker.util.graph.TopologicalTraversal;

public class FunctionSCCGraphTest {

  @Test
  public void testSCCIsPartition() throws Exception {
    BlockGraph blockGraph = FunctionGraphTest.createBlockGraph(FunctionGraphTest.PROGRAM_RECURSIVE);

    FunctionGraph funcGraph = FunctionGraph.from(blockGraph);
    FunctionSCCGraph sccGraph = FunctionSCCGraph.from(funcGraph);

    Truth.assertWithMessage("SCC does not partition the functions")
        .that(FluentIterable.from(sccGraph.getSCCs()).transformAndConcat(scc -> scc.functions()))
        .containsExactlyElementsIn(funcGraph.getFunctions());
  }

  @Test
  public void testCallStacks() throws Exception {

    BlockGraph blockGraph = FunctionGraphTest.createBlockGraph(FunctionGraphTest.PROGRAM_RECURSIVE);

    FunctionGraph funcGraph = FunctionGraph.from(blockGraph);
    FunctionSCCGraph sccGraph = FunctionSCCGraph.from(funcGraph);

    Multimap<FunctionSCC, CallStack> stacks = sccGraph.findCallStacks();

    // Do not compare exact output to not be confused by different block names
    Truth.assertWithMessage("SCC call stacks wrong")
        .that(
            FluentIterable.from(stacks.asMap().entrySet())
                .transform(entry -> entry.getValue().size()))
        .containsExactly(
            1, // main (entry)
            2, // SCC[rec3, rec2, rec1] (2x main)
            4, // leaf1 (2x main, 1x [rec3, rec2, rec1]
            4 // leaf2 (2x [rec3, rec2, rec1])
            );
  }

  @Test
  public void testTraversalVisitsAll() throws Exception {

    BlockGraph blockGraph = FunctionGraphTest.createBlockGraph(FunctionGraphTest.PROGRAM_RECURSIVE);

    FunctionGraph funcGraph = FunctionGraph.from(blockGraph);
    FunctionSCCGraph sccGraph = FunctionSCCGraph.from(funcGraph);

    Truth.assertWithMessage("TopologicalTraversal does not visit all SCCS")
        .that(TopologicalTraversal.traverse(sccGraph.getRoot(), sccGraph::getSuccessors))
        .containsExactlyElementsIn(sccGraph.getSCCs());
  }
}
