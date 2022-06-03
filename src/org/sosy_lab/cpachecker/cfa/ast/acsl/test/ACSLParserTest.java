// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.test;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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
import org.sosy_lab.cpachecker.cfa.CFAWithACSLAnnotations;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLAnnotation;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

@RunWith(Parameterized.class)
public class ACSLParserTest {

  private static final String TEST_DIR = "test/programs/acsl/";

  private final String programName;
  private final int expectedAnnotations;
  private final CFACreator cfaCreator;

  public ACSLParserTest(String pProgramName, int pExpectedAnnotations)
      throws InvalidConfigurationException {
    programName = pProgramName;
    expectedAnnotations = pExpectedAnnotations;
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
    b.add(task("abs.c", 1));
    b.add(task("abs2.c", 1));
    b.add(task("even.c", 1));
    b.add(task("even2.c", 1));
    b.add(task("nested.c", 2));
    b.add(task("simple.c", 3));
    b.add(task("badVariable.c", 0));
    b.add(task("statements.c", 4));
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
    b.add(task("traps.c", 2));
    b.add(task("empty.c", 1));
    b.add(task("no_annotations.c", 0));
    b.add(task("badBehavior.c", 0));
    return b.build();
  }

  private static Object[] task(String program, int expectedAnnotations) {
    return new Object[] {program, expectedAnnotations};
  }

  @Test
  public void annotationParsingProducesExpectedNumberOfAnnotations() throws Exception {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());
    CFA cfa = cfaCreator.parseFileAndCreateCFA(files);
    if (cfa instanceof CFAWithACSLAnnotations) {
      CFAWithACSLAnnotations cfaWithLocs = (CFAWithACSLAnnotations) cfa;
      Set<ACSLAnnotation> annotations =
          ImmutableSet.copyOf(cfaWithLocs.getEdgesToAnnotations().values());
      assertThat(annotations).hasSize(expectedAnnotations);
    } else {
      assertThat(expectedAnnotations).isEqualTo(0);
    }
  }
}
