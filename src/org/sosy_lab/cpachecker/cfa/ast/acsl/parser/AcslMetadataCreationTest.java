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
import java.util.Optional;
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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acslDeprecated.test.ACSLParserTest;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

@RunWith(Parameterized.class)
public class AcslMetadataCreationTest {
  private static final String TEST_DIR = "test/programs/acsl/";

  private final String programName;
  private final int expectedAnnotations;
  // private final int expectedNode;
  private final CFACreator cfaCreator;
  private final Optional<Integer> expectedLine;
  private final Optional<Integer> expectedCol;

  public AcslMetadataCreationTest(
      String pProgramName,
      int pExpetedAnnotions,
      Optional<Integer> pExpectedLine,
      Optional<Integer> pExpectedCol)
      throws InvalidConfigurationException {
    programName = pProgramName;
    expectedAnnotations = pExpetedAnnotions;
    expectedLine = pExpectedLine;
    expectedCol = pExpectedCol;
    // expectedNode = pExpectedNode;
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

    /*
    Regular ACSL statement annotation
     */
    b.add(task("even.c", 1, 21, 3));
    b.add(task("inv_for.c", 1, 13, 3));
    b.add(task("inv_short-for.c", 1, 13, 3));
    b.add(task("minimal_example.c", 1, 12, 5));

    /*
    Function contracts
     */
    b.add(task("abs2.c", 1, 21, 1));
    b.add(task("abs.c", 1, 21, 1));
    b.add(task("simple.c", 3, 18, 1));

    /*
    Special cases
     */
    b.add(task("no_annotations.c", 0));
    return b.build();
  }

  private static Object[] task(
      String program, int expectedAnnotations, int pExpectedLine, int pExpextedCol) {
    return new Object[] {
      program, expectedAnnotations, Optional.of(pExpectedLine), Optional.of(pExpextedCol)
    };
  }

  private static Object[] task(String program, int expectedAnnotations) {
    return new Object[] {program, expectedAnnotations, Optional.empty(), Optional.empty()};
  }

  @Test
  public void parseCorrectNumberOfAcslCommentsTest() throws Exception {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());
    CFA cfa = cfaCreator.parseFileAndCreateCFA(files);
    if (cfa.getMetadata().getAcslMetadata() != null) {
      ImmutableList<AcslComment> acslComments = cfa.getMetadata().getAcslMetadata().pAcslComments();
      assertThat(acslComments).hasSize(expectedAnnotations);
    }
  }

  @Test
  public void updateCfaNodseCorrectlyTest() throws Exception {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());
    CFA cfa = cfaCreator.parseFileAndCreateCFA(files);
    if (cfa.getMetadata().getAcslMetadata() != null) {
      ImmutableList<AcslComment> acslComments = cfa.getMetadata().getAcslMetadata().pAcslComments();
      if (!acslComments.isEmpty()) {
        CFANode actualNode = acslComments.getFirst().getCfaNode();
        FileLocation actualLocation = describeFileLocation(actualNode);
        assert actualLocation.isRealLocation();
        assertThat(Optional.of(actualLocation.getStartingLineNumber())).isEqualTo(expectedLine);
        assertThat(Optional.of(actualLocation.getStartColumnInLine())).isEqualTo(expectedCol);
      }
    }
  }

  public FileLocation describeFileLocation(CFANode node) {
    if (node instanceof FunctionEntryNode functionEntryNode) {
      return functionEntryNode.getFileLocation();
    }
    if (node.getNumLeavingEdges() > 0) {
      return node.getLeavingEdge(0).getFileLocation();
    }
    if (node.getNumEnteringEdges() > 0) {
      return node.getEnteringEdge(0).getFileLocation();
    }
    return FileLocation.DUMMY;
  }
}
