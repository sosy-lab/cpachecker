// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class CPreprocessorTest {

  private static final String TEST_FILE = "../foo/test file.c";

  @Test
  public void testCommandLine() throws InvalidConfigurationException {
    CPreprocessor preprocessor =
        new CPreprocessor(
            TestDataTools.configurationForTest().build(), LogManager.createTestLogManager());

    Iterable<String> cmdLine = preprocessor.getFullCommandLine(Path.of(TEST_FILE));

    assertThat(cmdLine).contains(TEST_FILE);
  }
}
