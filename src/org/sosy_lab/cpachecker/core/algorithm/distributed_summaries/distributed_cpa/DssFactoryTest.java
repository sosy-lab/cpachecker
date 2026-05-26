// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.BiMap;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class DssFactoryTest {
  private static final String PROGRAM = "doc/examples/example.c";

  @Test
  public void testCanResetCfaNodeIdMap() throws Exception {
    String programText = Files.readString(Path.of(PROGRAM), StandardCharsets.UTF_8);
    CFA originalCFA = TestDataTools.makeCFA(programText);
    CFA shiftedCFA = TestDataTools.makeCFA(programText);

    // If the CFAs have the same nodes, then they were not shifted and this test is not valid
    assertThat(originalCFA.nodes()).isNotEmpty();
    assertThat(originalCFA.nodes()).containsNoneIn(shiftedCFA.nodes());

    BiMap<Integer, CFANode> cfaNodeIdMapWithOriginalCFA =
        DssFactory.createCfaNodeIdMap(originalCFA);
    BiMap<Integer, CFANode> cfaNodeIdMapWithShiftedCFA = DssFactory.createCfaNodeIdMap(shiftedCFA);

    assertThat(cfaNodeIdMapWithOriginalCFA.keySet()).isEqualTo(cfaNodeIdMapWithShiftedCFA.keySet());
  }
}
