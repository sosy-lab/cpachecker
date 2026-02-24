// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

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
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.acslDeprecated.test.ACSLParserTest;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

@RunWith(Parameterized.class)
public class AcslMetadataParsingTest {
  private static final String TEST_DIR = "test/programs/acsl/";
  private final String programName;
  private final ImmutableList<String> expectedComments;
  private final CFACreator cfaCreator;
  private final LogManager logManager;

  public AcslMetadataParsingTest(String pProgramName, ImmutableList<String> pAnnotations)
      throws InvalidConfigurationException {
    programName = pProgramName;
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
    // Regular Annotations (assertions and loop loopAnnotations)
    b.add(
        task(
            "double_loop_invariant.c",
            ImmutableList.of("loop invariant  1 <= x <= 21; loop invariant  1 <= x <= 21")));
    b.add(task("after_else.c", ImmutableList.of("assert a == 10 || a == 20;")));
    b.add(task("after_for_loop2.c", ImmutableList.of("assert b == 20;")));
    b.add(task("after_if.c", ImmutableList.of("assert a != 20;")));
    b.add(task("after_loop.c", ImmutableList.of("assert a == 20;")));
    b.add(task("after_loop2.c", ImmutableList.of("assert  a == 20;")));
    b.add(task("at_end.c", ImmutableList.of("assert a != 20;")));
    b.add(task("badVariable.c", ImmutableList.of()));
    b.add(task("end_of_do_while.c", ImmutableList.of("assert a <= 20")));
    b.add(task("even_while.c", ImmutableList.of("loop invariant x % 2 == 0;")));
    b.add(task("even_while_nondet.c", ImmutableList.of("loop invariant x % 2 == 0;")));
    b.add(task("even_do_while.c", ImmutableList.of("loop invariant  1 <= x <= 10 && x % 2 == 1;")));
    b.add(task("in_middle.c", ImmutableList.of("assert a == 19;")));
    b.add(task("inv_for.c", ImmutableList.of("loop invariant x + y == 20;")));
    b.add(task("inv_short-for.c", ImmutableList.of("loop invariant x + y == 20;")));
    b.add(task("same_annotation_twice.c", ImmutableList.of("assert x == 10;", "assert x == 10;")));

    // function contracts
    b.add(task("square.c", ImmutableList.of("ensures b >= 0; ensures b == a * a;")));
    b.add(task("square_result.c", ImmutableList.of("ensures \\result == a * a;")));
    b.add(task("power.c", ImmutableList.of("requires a > 0; requires b>= 0; ensures c > 0;")));
    b.add(
        task(
            "power_result.c",
            ImmutableList.of("requires a > 0; requires b>= 0; ensures \\result > 0;")));
    return b.build();
  }

  private static Object[] task(String program, ImmutableList<String> annotations) {
    return new Object[] {program, annotations};
  }

  @Test
  public void parseCorrectNumberOfAcslCommentsTest() throws Exception {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());

    if (programName.equals("badVariable.c")) {
      RuntimeException expectedException =
          assertThrows(
              RuntimeException.class, () -> cfaCreator.parseFilesAndCreateAcslMetadata(files));
      assertThat(expectedException)
          .hasMessageThat()
          .isEqualTo("Variable y is not declared in neither the C program nor the ACSL scope.");
    } else {
      AcslMetadata acslMetadata = cfaCreator.parseFilesAndCreateAcslMetadata(files);
      assertThat(acslMetadata).isNotNull();
      assertThat(acslMetadata.numOfAnnotaniots()).isEqualTo(expectedComments.size());
    }
  }
}
