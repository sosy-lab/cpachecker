// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.test;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.ConsoleLogFormatter;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.StringBuildingLogHandler;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;

/**
 * Helper class for running integration tests, i.e., full CPAchecker runs.
 *
 * <p>The primary recommendation is for integration tests to be added as BuildBot tests, which have
 * significant performance advantage due BenchCloud usage (cf. doc/Test.md).
 *
 * <p>However, for some cases writing a few integration test as JUnit tests may be desired (in
 * addition to BuildBot tests). These tests can use the utilities in this class. Such test classes
 * should typically have the name suffix "IntegrationTest" such that they are executed together with
 * the other integration tests and not the unit tests.
 *
 * <p>However, such integration tests as JUnit tests should be disabled by default in order to not
 * let local test execution and CI pipelines take too much time. Thus all developers adding
 * integration tests as JUnit tests should guard them with {@link
 * #skipUnlessExtendedTestsEnabled()}, with the exceptions of:
 *
 * <ul>
 *   <li>{@link org.sosy_lab.cpachecker.core.CPAcheckerIntegrationTest}, a test class with a small
 *       set of central integration tests
 *   <li>integration tests that check more than the just the verification result, i.e., witness
 *       content, etc. (such tests are more valuable and rare than basic integration tests and can
 *       thus be enabled by default)
 * </ul>
 *
 * <p>Cf. #1609 for a discussion of the background of this.
 *
 * <p>Use <code>ant integration-tests -DenableExtendedTests=true</code> to enable all such tests.
 */
public class IntegrationTestRunner {

  /**
   * Automatically skips the current tests if only the most important integration tests are desired.
   * This should be called by developers adding integration tests as JUnit test, cf. the JavaDoc of
   * this class for more details.
   *
   * <p>A good place to call this is in a public static {@link org.junit.BeforeClass} method (after
   * separating unit tests and integration tests into different classes).
   *
   * <p>This method is actually the same as {@link TestUtils#skipUnlessExtendedTestsEnabled()} and
   * duplicate here just for visibility.
   */
  public static void skipUnlessExtendedTestsEnabled() {
    TestUtils.skipUnlessExtendedTestsEnabled();
  }

  public enum ExpectedVerdict {
    TRUE,
    FALSE,
    NONE
  }

  public record IntegrationTestResult(String log, CPAcheckerResult cpaCheckerResult) {

    public void assertIs(CPAcheckerResult.Result expected) {
      if (cpaCheckerResult.getResult() != expected) {
        throw new AssertionError(
            String.format(
                "Not true that verification result is %s, it is %s. Log output was:%n---%n%s%n---",
                expected, cpaCheckerResult.getResult(), log.trim()));
      }
    }

    public void assertIsSafe() {
      assertIs(CPAcheckerResult.Result.TRUE);
    }

    public void assertIsUnsafe() {
      assertIs(CPAcheckerResult.Result.FALSE);
    }

    @Override
    public String toString() {
      return log;
    }
  }

  /**
   * Execute CPAchecker with the given options on the given program file and collect the result and
   * log output (with the default log level).
   *
   * <p>Note that most tests using this should be disabled by default by calling {@link
   * #skipUnlessExtendedTestsEnabled()} before, cf. the JavaDoc of this class for more details.
   *
   * <p>Classes calling this should typically have the name suffix "IntegrationTest".
   */
  public static IntegrationTestResult run(
      Map<String, String> pProperties, String pSourceCodeFilePath) throws Exception {

    Configuration config = TestUtils.configurationForTest().setOptions(pProperties).build();
    return run(config, pSourceCodeFilePath);
  }

  /**
   * Execute CPAchecker with the given configuration on the given program file and collect the
   * result and log output (with the default log level).
   *
   * <p>Note that most tests using this should be disabled by default by calling {@link
   * #skipUnlessExtendedTestsEnabled()} before, cf. the JavaDoc of this class for more details.
   *
   * <p>Classes calling this should typically have the name suffix "IntegrationTest".
   */
  public static IntegrationTestResult run(Configuration config, String pSourceCodeFilePath)
      throws Exception {
    return run(config, pSourceCodeFilePath, Level.INFO);
  }

  /**
   * Execute CPAchecker with the given configuration on the given program file and collect the
   * result and log output.
   *
   * <p>Note that most tests using this should be disabled by default by calling {@link
   * #skipUnlessExtendedTestsEnabled()} before, cf. the JavaDoc of this class for more details.
   *
   * <p>Classes calling this should typically have the name suffix "IntegrationTest".
   */
  public static IntegrationTestResult run(
      Configuration config, String pSourceCodeFilePath, Level logLevel) throws Exception {
    StringBuildingLogHandler stringLogHandler = new StringBuildingLogHandler();
    stringLogHandler.setLevel(logLevel);
    stringLogHandler.setFormatter(ConsoleLogFormatter.withoutColors());
    LogManager logger = BasicLogManager.createWithHandler(stringLogHandler);

    ShutdownManager shutdownManager = ShutdownManager.create();
    CPAchecker cpaChecker = new CPAchecker(config, logger, shutdownManager);
    CPAcheckerResult results = cpaChecker.run(ImmutableList.of(pSourceCodeFilePath));
    logger.flush();
    return new IntegrationTestResult(stringLogHandler.getLog(), results);
  }
}
