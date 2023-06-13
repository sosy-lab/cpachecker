// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.test;

import java.nio.file.Path;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sosy_lab.common.configuration.Configuration;

@Ignore("prevent this abstract class being executed as testcase by ant")
@RunWith(Parameterized.class)
public abstract class ToCTranslationTest extends AbstractTranslationTest {

  protected final Path targetProgram;
  private final boolean verdict;
  private final Configuration checkerConfig;

  protected ToCTranslationTest(
      Path pTargetProgram, boolean pVerdict, Configuration pCheckerConfig) {
    verdict = pVerdict;
    checkerConfig = pCheckerConfig;
    targetProgram = pTargetProgram;
  }

  protected abstract void createProgram(Path pTargetPath) throws Exception;

  @Test
  public void testVerdictsStaySame() throws Exception {
    createProgram(targetProgram);

    // test whether C program still gives correct verdict:
    check(checkerConfig, targetProgram, verdict);
  }

  @Test
  public void testProgramsParsable() throws Exception {
    createProgram(targetProgram);

    checkProgramValid(targetProgram);
  }

  @Test
  public void testProgramsCompilable() throws Exception {
    createProgram(targetProgram);

    checkProgramCompilable(targetProgram);
  }
}
