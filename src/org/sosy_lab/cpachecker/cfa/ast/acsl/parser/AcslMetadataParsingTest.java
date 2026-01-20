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
import com.google.common.collect.ImmutableListMultimap;
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
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AAcslAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acslDeprecated.test.ACSLParserTest;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

@RunWith(Parameterized.class)
public class AcslMetadataParsingTest {
  private static final String TEST_DIR = "test/programs/acsl/";
  private final String programName;
  private final int expectedNumComments;
  private final ImmutableList<String> expectedComments;
  private final CFACreator cfaCreator;
  private final LogManager logManager;

  public AcslMetadataParsingTest(
      String pProgramName, int pExpectedNumComments, ImmutableList<String> pAnnotations)
      throws InvalidConfigurationException {
    programName = pProgramName;
    expectedNumComments = pExpectedNumComments;
    expectedComments = pAnnotations;
    Configuration config =
        TestDataTools.configurationForTest()
            .loadFromResource(ACSLParserTest.class, "acslToWitness.properties")
            .build();
    logManager = LogManager.createTestLogManager();
    cfaCreator = new CFACreator(config, logManager, ShutdownNotifier.createDummy());
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    ImmutableList.Builder<Object[]> b = ImmutableList.builder();
    b.add(task("after_else.c", 1, ImmutableList.of("assert a == 10 || a == 20;")));
    b.add(task("after_for_loop2.c", 1, ImmutableList.of("assert b == 20;")));
    b.add(task("after_if.c", 1, ImmutableList.of("assert a != 20;")));
    b.add(task("after_loop.c", 1, ImmutableList.of("assert a == 20;")));
    b.add(task("after_loop2.c", 1, ImmutableList.of("assert  a == 20;")));
    b.add(task("at_end.c", 1, ImmutableList.of("assert a == 20;")));
    b.add(task("badVariable.c", 0, ImmutableList.of()));
    b.add(task("empty.c", 1, ImmutableList.of("assert \true")));
    b.add(task("end_of_do_while.c", 1, ImmutableList.of("assert a <= 20")));
    b.add(task("even.c", 1, ImmutableList.of("loop invariant x % 2 == 0;")));
    b.add(task("even2.c", 1, ImmutableList.of("loop invariant  1 <= x <= 10 && x % 2 == 1;")));
    b.add(task("in_middle.c", 1, ImmutableList.of("assert a == 19;")));
    b.add(task("inv_for.c", 1, ImmutableList.of("loop invariant x + y == 20;")));
    b.add(task("inv_short-for.c", 1, ImmutableList.of("loop invariant x + y == 20;")));
    b.add(task("minimal_example.c", 1, ImmutableList.of("ensures x == 10;")));
    b.add(task("no_annotations.c", 0, ImmutableList.of()));
    b.add(
        task(
            "statements.c",
            4,
            ImmutableList.of(
                "ensures x == 0;",
                "ensures y == 0;",
                "ensures x == i;",
                "requires x == i; ensures y == i;")));
    b.add(task("traps.c", 2, ImmutableList.of("assert \false;", "ensures y > 0")));
    return b.build();
  }

  private static Object[] task(
      String program, int expectedNumComments, ImmutableList<String> annotations) {
    return new Object[] {program, expectedNumComments, annotations};
  }

  @Test
  public void parseCorrectNumberOfAcslCommentsTest() throws Exception {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());
    CFA cfa = cfaCreator.parseFileAndCreateCFA(files);
    if (cfa.getMetadata().getAcslMetadata() != null) {
      ImmutableList<AcslComment> acslComments = cfa.getMetadata().getAcslMetadata().pAcslComments();
      assertThat(acslComments).hasSize(expectedNumComments);
    }
  }

  @Test
  public void parseMetadataTest() throws Exception {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());
    CFA cfa = cfaCreator.parseFileAndCreateCFA(files);

    CProgramScope programScope = new CProgramScope(cfa, logManager);

    ImmutableList.Builder<AAcslAnnotation> expectedBuilder = ImmutableList.builder();
    for (String s : expectedComments) {
      expectedBuilder.add(
          AcslParser.parseAcslAnnotation(s, FileLocation.DUMMY, programScope, AcslScope.empty()));
    }
    ImmutableList<AAcslAnnotation> expectedAnnotations = expectedBuilder.build();

    AcslMetadata acslMetadata = cfa.getMetadata().getAcslMetadata();
    ImmutableListMultimap<AAcslAnnotation, CFANode> annotations =
        acslMetadata.genericAnnotations().inverse();
    for (AAcslAnnotation expectedAnnotation : expectedAnnotations) {
      assert annotations.containsKey(expectedAnnotation);
    }
  }

  public record CodeLoctation(int expectedLine, int expectedCol) {}
}
