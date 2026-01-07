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
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.acslDeprecated.test.ACSLParserTest;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

@RunWith(Parameterized.class)
public class AcslMetadataCreationTest {
  private static final String TEST_DIR = "test/programs/acsl/";

  private final String programName;
  private final int expectedOffset;
  private final CFACreator cfaCreator;

  public AcslMetadataCreationTest(String pProgramName, int pExpectedOffset)
      throws InvalidConfigurationException {
    programName = pProgramName;
    expectedOffset = pExpectedOffset;
    Configuration config =
        TestDataTools.configurationForTest()
            .loadFromResource(ACSLParserTest.class, "acslToWitness.properties")
            .build();
    cfaCreator =
        new CFACreator(config, LogManager.createTestLogManager(), ShutdownNotifier.createDummy());
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    ImmutableList.Builder<Object[]> b = ImmutableList.builder();
    b.add(task("minimal_example.c", 304));
    return b.build();
  }

  private static Object[] task(String proram, int expectedOffset) {
    return new Object[] {proram, expectedOffset};
  }

  @Test
  public void updateCfaNodseCorrectlyTest() throws Exception {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());
    CFA cfa = cfaCreator.parseFileAndCreateCFA(files);
    if (cfa.getMetadata().getAcslMetadata() != null) {
      ImmutableList<AcslComment> acslComments = cfa.getMetadata().getAcslMetadata().pAcslComments();
      CFANode actualNode = acslComments.getFirst().getCfaNode();
      ImmutableList<CFAEdge> outgoingEdge =
          actualNode
              .getLeavingEdges()
              .filter(n -> n.getFileLocation().getNodeOffset() == expectedOffset)
              .toList();
      assertThat(outgoingEdge).hasSize(1);
    }
  }
}
