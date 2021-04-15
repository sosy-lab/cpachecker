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
import org.sosy_lab.cpachecker.cfa.CFAWithACSLAnnotations;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

@RunWith(Parameterized.class)
public class ACSLParserTest {

  private static final String TEST_DIR = "test/programs/acsl/";

  private final String programName;
  private final boolean shouldSucceed;
  private final CFACreator cfaCreator;

  public ACSLParserTest(String pProgramName, boolean pShouldSucceed)
      throws InvalidConfigurationException {
    programName = pProgramName;
    shouldSucceed = pShouldSucceed;
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
    b.add(new Object[] {"abs.c", true});
    b.add(new Object[] {"abs2.c", true});
    b.add(new Object[] {"even.c", true});
    b.add(new Object[] {"even2.c", true});
    b.add(new Object[] {"nested.c", true});
    b.add(new Object[] {"simple.c", true});
    b.add(new Object[] {"badVariable.c", false});
    return b.build();
  }

  @Test
  public void test()
      throws InterruptedException, ParserException, InvalidConfigurationException, IOException {
    List<String> files = ImmutableList.of(Paths.get(TEST_DIR, programName).toString());
    try {
      CFAWithACSLAnnotations cfaWithLocs =
          (CFAWithACSLAnnotations) cfaCreator.parseFileAndCreateCFA(files);
      assertThat(shouldSucceed).isTrue();
      assertThat(cfaWithLocs.getEdgesToAnnotations().size()).isGreaterThan(0);
    } catch (AssertionError e) {
      assertThat(shouldSucceed).isFalse();
    }
  }
}
