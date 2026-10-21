// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import static com.google.common.truth.Truth.assertWithMessage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/** Opt-in project-wide test run with every parsed CFA replaced by a structural clone. */
public class ClonedCfaProjectTest {

  @Test
  public void runProjectTestsWithClonedCfas()
      throws IOException, InterruptedException, URISyntaxException {
    TestUtils.skipUnlessExtendedTestsEnabled();

    Path agentJar =
        Path.of(
            net.bytebuddy.agent.ByteBuddyAgent.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
    String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    Process process =
        new ProcessBuilder(
                javaExecutable,
                "-javaagent:" + agentJar,
                "-ea",
                "-DenableExtendedTests=true",
                "-Djava.awt.headless=true",
                "--enable-native-access=ALL-UNNAMED",
                "-cp",
                System.getProperty("java.class.path"),
                ClonedCfaProjectTestRunner.class.getName())
            .inheritIO()
            .start();
    int exitCode = process.waitFor();
    assertWithMessage("Cloned-CFA project test runner exited with code %s", exitCode)
        .that(exitCode)
        .isEqualTo(0);
  }
}
