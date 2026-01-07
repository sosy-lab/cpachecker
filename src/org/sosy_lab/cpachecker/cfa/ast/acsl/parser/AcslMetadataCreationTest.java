// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.acslDeprecated.test.ACSLParserTest;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class AcslMetadataCreationTest {
  private static final String TEST_DIR = "test/programs/acsl/";
  private final CFACreator cfaCreator;

  public AcslMetadataCreationTest() throws InvalidConfigurationException {
    Configuration config =
        TestDataTools.configurationForTest()
            .loadFromResource(ACSLParserTest.class, "acslToWitness.properties")
            .build();
    cfaCreator =
        new CFACreator(config, LogManager.createTestLogManager(), ShutdownNotifier.createDummy());
  }

  @Test
  public void updateCfaNodseCorrectlyTest() throws Exception {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, "minimal_example.c").toString());
    CFA cfa = cfaCreator.parseFileAndCreateCFA(files);
    if (cfa.getMetadata().getAcslMetadata() != null) {
      ImmutableList<AcslComment> acslComments = cfa.getMetadata().getAcslMetadata().pAcslComments();
      /*
      Compares the node number of the Cfa Node for the Acsl Comment with an expected value.
      This has two problems:
      1. We need to know the node numbers of the Cfa before we write a test
      2. The numbering of the Cfa Nodes might change at some point
       */
      int actualNodeId = acslComments.getFirst().getCfaNode().getNodeNumber();
      int expectedNodeId = 3;
      assertThat(acslComments).hasSize(1);
      assert acslComments.getFirst().hasCfaNode() && actualNodeId == expectedNodeId;
    }
  }
}
