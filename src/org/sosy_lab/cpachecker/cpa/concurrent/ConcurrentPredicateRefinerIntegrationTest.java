// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.TestUtils;

public class ConcurrentPredicateRefinerIntegrationTest {
  private static final String HEADER =
      """
      typedef unsigned long pthread_t;
      extern int pthread_create(pthread_t *, const void *, void *(*)(void *), void *);
      extern int pthread_join(pthread_t, void **);
      """;

  @Rule public TemporaryFolder temporary = new TemporaryFolder();

  @Test
  public void reconstructsTheScheduleThatReadsBetweenWrites() throws Exception {
    try (CPAcheckerResult result =
        run(
            """
            int x;
            void *writer(void *arg) { x = 1; x = 2; return 0; }
            void *reader(void *arg) {
              int value = x;
              if (value == 1) { ERROR: return 0; }
              return 0;
            }
            int main(void) {
              pthread_t a, b;
              pthread_create(&a, 0, writer, 0);
              pthread_create(&b, 0, reader, 0);
              pthread_join(a, 0); pthread_join(b, 0);
              return 0;
            }
            """)) {
      CounterexampleInfo counterexample = preciseCounterexample(result);
      List<String> statements =
          counterexample.getTargetPath().getFullPath().stream()
              .map(edge -> edge.getRawStatement())
              .toList();
      int firstWrite = statements.indexOf("x = 1;");
      int read = statements.indexOf("int value = x;");
      int secondWrite = statements.indexOf("x = 2;");
      assertThat(firstWrite).isAtLeast(0);
      assertThat(read).isGreaterThan(firstWrite);
      assertThat(secondWrite).isGreaterThan(read);
      assertThat(
              counterexample.getTargetPath().asStatesList().stream()
                  .anyMatch(state -> state.getParents().size() > 1))
          .isTrue();
    }
  }

  @Test
  public void infeasibleMergedSchedulesStillRefine() throws Exception {
    try (CPAcheckerResult result =
        run(
            """
            int x;
            void *writer(void *arg) { x = 1; x = 2; return 0; }
            void *reader(void *arg) {
              int value = x;
              if (value == 3) { ERROR: return 0; }
              return 0;
            }
            int main(void) {
              pthread_t a, b;
              pthread_create(&a, 0, writer, 0);
              pthread_create(&b, 0, reader, 0);
              pthread_join(a, 0); pthread_join(b, 0);
              return 0;
            }
            """)) {
      assertThat(result.getResult()).isEqualTo(CPAcheckerResult.Result.TRUE);
    }
  }

  @Test
  public void replaysThreadArgumentsAndThreadLocalInitialization() throws Exception {
    try (CPAcheckerResult result =
        run(
            """
            int x;
            __thread int local = 7;
            void *writer(void *arg) { *(int *)arg = local; return 0; }
            int main(void) {
              pthread_t a;
              pthread_create(&a, 0, writer, &x);
              pthread_join(a, 0);
              if (x == 7) { ERROR: return 0; }
              return 0;
            }
            """)) {
      preciseCounterexample(result);
    }
  }

  @Test
  public void replaysInterleavedFunctionCalls() throws Exception {
    try (CPAcheckerResult result =
        run(
            """
            int x;
            int write(int a) { x = a; return a; }
            int read(int a) { return x + a; }
            void *writer(void *arg) { write(1); write(2); return 0; }
            void *reader(void *arg) {
              int value = read(0);
              if (value == 1) { ERROR: return 0; }
              return 0;
            }
            int main(void) {
              pthread_t a, b;
              pthread_create(&a, 0, writer, 0);
              pthread_create(&b, 0, reader, 0);
              pthread_join(a, 0); pthread_join(b, 0);
              return 0;
            }
            """)) {
      preciseCounterexample(result);
    }
  }

  @Test
  public void replaysCandidateJoins() throws Exception {
    try (CPAcheckerResult result =
        run(
            """
            int x;
            void *writer(void *arg) { x = 1; return 0; }
            int main(void) {
              pthread_t handles[1];
              int index = 0;
              pthread_create(&handles[0], 0, writer, 0);
              pthread_join(handles[index], 0);
              if (x == 1) { ERROR: return 0; }
              return 0;
            }
            """)) {
      preciseCounterexample(result);
    }
  }

  @Test
  public void refinesAcrossLoopAbstractions() throws Exception {
    try (CPAcheckerResult result =
        run(
            """
            int x;
            void *writer(void *arg) {
              for (int i = 0; i < 3; i++) { x = i; }
              return 0;
            }
            int main(void) {
              pthread_t a;
              pthread_create(&a, 0, writer, 0);
              pthread_join(a, 0);
              if (x != 2) { ERROR: return 0; }
              return 0;
            }
            """)) {
      assertThat(result.getResult()).isEqualTo(CPAcheckerResult.Result.TRUE);
    }
  }

  @Test
  public void replaysRecursiveCalls() throws Exception {
    try (CPAcheckerResult result =
        run(
            """
            int x;
            int recurse(int n) {
              if (n == 0) { return 0; }
              int value = recurse(n - 1);
              return value + 1;
            }
            void *writer(void *arg) { x = recurse(2); return 0; }
            int main(void) {
              pthread_t a;
              pthread_create(&a, 0, writer, 0);
              pthread_join(a, 0);
              if (x == 2) { ERROR: return 0; }
              return 0;
            }
            """,
            "cpa.callstack.depth",
            "4",
            "cpa.predicate.satCheck",
            "1")) {
      preciseCounterexample(result);
    }
  }

  private CPAcheckerResult run(String program, String... options) throws Exception {
    var source = temporary.newFile("program.c").toPath();
    Files.writeString(source, HEADER + program, StandardCharsets.UTF_8);
    var builder =
        TestUtils.configurationForTest()
            .loadFromFile("config/por-pred.properties")
            .setOption("specification", "config/specification/ErrorLabel.spc")
            .setOption("cpa.concurrent.merge", "JOIN")
            .setOption("cpa.predicate.satCheck", "0")
            .setOption("counterexample.export.allowImpreciseCounterexamples", "false")
            .setOption("analysis.traversal.order", "bfs")
            .setOption("analysis.traversal.useReversePostorder", "false")
            .setOption("analysis.traversal.useNumberOfThreads", "false");
    for (int i = 0; i < options.length; i += 2) {
      builder.setOption(options[i], options[i + 1]);
    }
    return IntegrationTestRunner.run(builder.build(), source.toString()).cpaCheckerResult();
  }

  private static CounterexampleInfo preciseCounterexample(CPAcheckerResult result) {
    assertThat(result.getResult()).isEqualTo(CPAcheckerResult.Result.FALSE);
    for (var state : requireNonNull(result.getReached())) {
      ARGState arg = (ARGState) state;
      if (arg.isTarget() && arg.getCounterexampleInformation().isPresent()) {
        CounterexampleInfo counterexample = arg.getCounterexampleInformation().orElseThrow();
        assertThat(counterexample.isPreciseCounterExample()).isTrue();
        assertThat(counterexample.getCFAPathWithAssignments()).isNotEmpty();
        return counterexample;
      }
    }
    throw new AssertionError("No precise counterexample was attached to the ARG");
  }
}
