// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl.test;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Paths;
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
import org.sosy_lab.cpachecker.cfa.CFAWithACSLAnnotationLocations;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

@RunWith(Parameterized.class)
public class ACSLParserTest {

  private static final String TEST_DIR = "test/programs/acsl/";

  private final String programName;
  private final CFACreator cfaCreator;

  public ACSLParserTest(String pProgramName) throws InvalidConfigurationException {
    programName = pProgramName;
    Configuration config =
        TestDataTools.configurationForTest()
            .loadFromResource(ACSLParserTest.class, "acslToWitness.properties")
            .build();
    cfaCreator =
        new CFACreator(config, LogManager.createTestLogManager(), ShutdownNotifier.createDummy());
  }

  @Parameters(name = "{0}")
  public static Object[] data() {
    ImmutableList.Builder<Object> b = ImmutableList.builder();
    b.add("abs.c");
    b.add("even.c");
    b.add("simple.c");
    return b.build().toArray();
  }

  @Test
  public void test()
      throws InterruptedException, ParserException, InvalidConfigurationException, IOException {
    List<String> files = ImmutableList.of(Paths.get(TEST_DIR, programName).toString());
    CFAWithACSLAnnotationLocations cfaWithLocs =
        (CFAWithACSLAnnotationLocations) cfaCreator.parseFileAndCreateCFA(files);
    assertThat(cfaWithLocs.getCommentPositions().size()).isGreaterThan(0);
    assertThat(cfaWithLocs.getEdgesToAnnotations().keySet().size())
        .isAtLeast(cfaWithLocs.getCommentPositions().size());
  }
}
