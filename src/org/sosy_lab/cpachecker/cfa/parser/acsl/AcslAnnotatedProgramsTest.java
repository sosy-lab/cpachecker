// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/**
 * Checks the number of ACSL annotations in programs, including statement contracts and behaviors.
 */
@RunWith(Parameterized.class)
public class AcslAnnotatedProgramsTest {

  private static final String TEST_DIR = "test/programs/acsl/";

  /** Marks programs with invalid annotations, for which the creation of the CFA fails. */
  private static final int INVALID = -1;

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    ImmutableList.Builder<Object[]> b = ImmutableList.builder();
    // Function contracts that refer to local variables of the function
    b.add(task("abs.c", INVALID));
    b.add(task("simple.c", INVALID));
    b.add(task("badVariableStatementContract.c", INVALID));
    // Assertions in dead code or empty blocks have no CFA node
    b.add(task("traps.c", INVALID));
    b.add(task("empty.c", INVALID));
    b.add(task("abs2.c", 1));
    b.add(task("even.c", 1));
    b.add(task("even2.c", 1));
    b.add(task("nested.c", 2));
    b.add(task("statements.c", 4));
    b.add(task("minimal_example.c", 1));
    b.add(task("inv_for.c", 1));
    b.add(task("inv_short-for.c", 1));
    b.add(task("after_if.c", 1));
    b.add(task("after_else.c", 1));
    b.add(task("after_loop.c", 1));
    b.add(task("after_loop2.c", 1));
    b.add(task("at_end.c", 1));
    b.add(task("end_of_do_while.c", 1));
    b.add(task("after_for_loop.c", 1));
    b.add(task("after_for_loop2.c", 1));
    b.add(task("in_middle.c", 1));
    b.add(task("no_annotations.c", 0));
    // The statement contract refers to a behavior that is not declared, so it is ignored
    b.add(task("badBehavior.c", 0));
    return b.build();
  }

  private static Object[] task(String pProgram, int pExpectedAnnotations) {
    return new Object[] {pProgram, pExpectedAnnotations};
  }

  @Parameter(0)
  public String programName;

  @Parameter(1)
  public int expectedAnnotations;

  @Test
  public void annotationParsingProducesExpectedNumberOfAnnotations() throws Exception {
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromResource(AcslAnnotatedProgramsTest.class, "withAcslMetadata.properties")
            .build();
    CFACreator cfaCreator =
        new CFACreator(config, LogManager.createTestLogManager(), ShutdownNotifier.createDummy());
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());

    if (expectedAnnotations == INVALID) {
      assertThrows(AcslMetadataException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
    } else {
      CFA cfa = cfaCreator.parseFileAndCreateCFA(files);
      assertThat(cfa.getAcslMetadata().numOfAnnotaniots()).isEqualTo(expectedAnnotations);
    }
  }
}
