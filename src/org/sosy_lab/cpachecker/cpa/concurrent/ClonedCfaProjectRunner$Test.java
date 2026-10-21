// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.RunListener;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreator;

/** Main entry point for the isolated, instrumented project-wide JUnit run. */
public final class ClonedCfaProjectRunner$Test {

  private static final long TEST_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(3);

  /** Tests excluded because they cannot validate cloned-CFA behavior in this runner. */
  private static final ImmutableSet<String> SKIPPED_TESTS =
      ImmutableSet.of(
          "toCfaTest(org.sosy_lab.cpachecker.cfa.parser.svlib.SvLibToCfaParserTest)",
          "testWitnessExportForSafeSvLibProgram(org.sosy_lab.cpachecker.core.CPAcheckerIntegrationTest)",
          "testWitnessExportForUnsafeSvLibProgram(org.sosy_lab.cpachecker.core.CPAcheckerIntegrationTest)",
          "testRunPropertyFileForUnsafeSvLibProgram(org.sosy_lab.cpachecker.core.CPAcheckerIntegrationTest)",
          "testRunForUnsafeSvLibProgram(org.sosy_lab.cpachecker.core.CPAcheckerIntegrationTest)",
          "testRunForSafeSvLibProgram(org.sosy_lab.cpachecker.core.CPAcheckerIntegrationTest)",
          "testRunDeprecatedSpecificationForUnsafeSvLibProgram(org.sosy_lab.cpachecker.core.CPAcheckerIntegrationTest)",
          "testRunForUnsafeJavaProgram(org.sosy_lab.cpachecker.core.CPAcheckerIntegrationTest)",
          "testRunForSafeJavaProgram(org.sosy_lab.cpachecker.core.CPAcheckerIntegrationTest)",
          // This native numeric test does not create a CFA. It passes alone but fails after the
          // project's floating-point tests have run in the same JVM, so it is unrelated to CFA
          // cloning and cannot be assessed by this suite.
          "testNum_Float(org.sosy_lab.cpachecker.util.octagon.OctagonManagerTest)");

  private ClonedCfaProjectRunner$Test() {}

  public static void main(String[] args) throws IOException, URISyntaxException {
    ClonedCfaCreationHook.install();
    Path classesDirectory =
        Path.of(CFACreator.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    List<Class<?>> testClasses = findTestClasses(classesDirectory);

    LogManager logger = LogManager.createTestLogManager();
    JUnitCore junit = new JUnitCore();
    ScheduledExecutorService timeoutExecutor =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "cloned-CFA-test-timeout");
              thread.setDaemon(true);
              return thread;
            });
    junit.addListener(
        new RunListener() {
          private ScheduledFuture<?> timeoutTask;

          @Override
          public void testStarted(Description pDescription) {
            logger.log(Level.INFO, pDescription.getDisplayName());
            timeoutTask =
                timeoutExecutor.schedule(
                    () -> {
                      logger.log(
                          Level.SEVERE,
                          "Test exceeded the 3-minute timeout: " + pDescription.getDisplayName());
                      System.exit(1);
                    },
                    TEST_TIMEOUT_MILLIS,
                    TimeUnit.MILLISECONDS);
          }

          @Override
          public void testFinished(Description pDescription) {
            if (timeoutTask != null) {
              timeoutTask.cancel(false);
            }
          }
        });
    Request projectTests =
        Request.classes(testClasses.toArray(new Class<?>[0]))
            .filterWith(
                new Filter() {
                  @Override
                  public boolean shouldRun(Description pDescription) {
                    return !SKIPPED_TESTS.contains(pDescription.getDisplayName());
                  }

                  @Override
                  public String describe() {
                    return "excluding tests that cannot validate cloned-CFA behavior";
                  }
                });
    Result result = junit.run(projectTests);
    List<String> failureDescriptions =
        result.getFailures().stream().map(failure -> failure + "\n" + failure.getTrace()).toList();
    assertWithMessage(
            "Failures while running %s tests with cloned CFAs (%s failures, %s ignored):\n%s",
            result.getRunCount(),
            result.getFailureCount(),
            result.getIgnoreCount(),
            String.join("\n\n", failureDescriptions))
        .that(failureDescriptions)
        .isEmpty();
  }

  private static List<Class<?>> findTestClasses(Path pClassesDirectory) throws IOException {
    String suiteClassName = ClonedCfaProjectTest.class.getName();
    try (Stream<Path> classFiles = Files.walk(pClassesDirectory)) {
      return classFiles
          .filter(path -> path.getFileName().toString().endsWith("Test.class"))
          .filter(path -> !path.getFileName().toString().contains("$"))
          .map(pClassesDirectory::relativize)
          .map(path -> path.toString().replace(path.getFileSystem().getSeparator(), "."))
          .map(name -> name.substring(0, name.length() - ".class".length()))
          .filter(name -> !name.equals(suiteClassName))
          .sorted()
          .<Class<?>>map(
              name -> {
                try {
                  return Class.forName(
                      name, false, ClonedCfaProjectRunner$Test.class.getClassLoader());
                } catch (ClassNotFoundException e) {
                  throw new IllegalStateException("Could not load test class " + name, e);
                }
              })
          .filter(ClonedCfaProjectRunner$Test::isJUnitTest)
          .toList();
    }
  }

  private static boolean isJUnitTest(Class<?> pClass) {
    return !Modifier.isAbstract(pClass.getModifiers())
        && (junit.framework.TestCase.class.isAssignableFrom(pClass)
            || pClass.isAnnotationPresent(org.junit.runner.RunWith.class)
            || Arrays.stream(pClass.getMethods())
                .anyMatch(method -> method.isAnnotationPresent(org.junit.Test.class)));
  }
}
