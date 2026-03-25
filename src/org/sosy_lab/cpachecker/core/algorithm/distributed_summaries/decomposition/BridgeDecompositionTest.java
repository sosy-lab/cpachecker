// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.TestUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;

public class BridgeDecompositionTest {

  @Test
  public void testSimple() throws Exception {

    CFA cfa =
        TestUtil.buildTestCFA(
            DssBlockDecompositionTestUtil.PROGRAMM_PATH_SIMPLE);

    DssBlockDecomposition decomposition = new BridgeDecomposition();

    BlockGraph graph = decomposition.decompose(cfa);

    DssBlockDecompositionTestUtil.checkBlockGraph(graph, cfa);
  }

  @Ignore // This currently creates an endless loop
  @Test
  public void testLarge() throws Exception {

    CFA cfa =
        TestUtil.buildTestCFA(
            DssBlockDecompositionTestUtil.PROGRAMM_PATH_LARGE);

    DssBlockDecomposition decomposition = new BridgeDecomposition();

    BlockGraph graph = decomposition.decompose(cfa);

    DssBlockDecompositionTestUtil.checkBlockGraph(graph, cfa);
  }
}
