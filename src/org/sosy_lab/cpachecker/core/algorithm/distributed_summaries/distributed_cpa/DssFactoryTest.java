// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;

import com.google.common.collect.BiMap;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.TestUtil;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class DssFactoryTest {
  private static final String CONFIGURATION_FILE_GENERATE_CFA = "config/generateCFA.properties";
  private static final String PROGRAM = "doc/examples/example.c";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testCanResetCfaNodeIdMap() throws Exception {
    Path tempFolderPath = tempFolder.getRoot().toPath();
    CFA originalCFA = generateCfa(tempFolderPath);
    CFA shiftedCFA = generateCfa(tempFolderPath);

    assumeTrue(originalCFA.nodes() != shiftedCFA.nodes());

    BiMap<Integer, CFANode> cfaNodeIdMapWithOriginalCFA =
        DssFactory.createCfaNodeIdMap(originalCFA);
    BiMap<Integer, CFANode> cfaNodeIdMapWithShiftedCFA = DssFactory.createCfaNodeIdMap(shiftedCFA);

    assertThat(cfaNodeIdMapWithOriginalCFA.keySet()).isEqualTo(cfaNodeIdMapWithShiftedCFA.keySet());
  }

  private CFA generateCfa(Path tempFolderPath) throws Exception {
    Configuration configToGenerateCfa = TestUtil.generateConfig(CONFIGURATION_FILE_GENERATE_CFA, tempFolderPath);
    TestResults result = CPATestRunner.run(configToGenerateCfa, PROGRAM);
    return result.getCheckerResult().getCfa();
  }
}
