// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cmdline.CPAMain;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/**
 * Checks that unrolling the loops of a program does not change the witnesses that are written for
 * it. A witness is about the program as it is written, so it must say the same whether or not the
 * analysis ran on a program whose loops were taken apart, and a validator that does not unroll has
 * to accept it.
 */
public class LoopUnrollingWitnessIntegrationTest {

  private static final String TEST_DIR = "test/programs/simple/loop-unrolling/";
  // The property file of the task is only read from the command line, so the analysis uses the
  // specification that it stands for, cf. WitnessExporterIntegrationTest.
  private static final String SPECIFICATION = "config/specification/sv-comp-reachability.spc";
  private static final String PROPERTY = "config/properties/unreach-call.prp";
  private static final Path ANALYSIS = Path.of("config/valueAnalysis.properties");

  /** A program with a loop that the heuristic counts, and a branch inside that loop. */
  private static final String UNSAFE_PROGRAM = "counted-continue-unsafe.c";

  /** A program with a loop that the heuristic counts, and an invariant to report for it. */
  private static final String SAFE_PROGRAM = "counted-while-safe.c";

  private static final String VIOLATION_WITNESS = "Counterexample.*.witness-*.yml";
  private static final String CORRECTNESS_WITNESS = "witness-*.yml";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private final PrintStream discardedOutput =
      new PrintStream(ByteStreams.nullOutputStream(), true, Charset.defaultCharset());

  @Test(timeout = 90000)
  public void violationWitnessIsTheSameAsWithoutUnrolling() throws Exception {
    Path unrolled = exportWitness(UNSAFE_PROGRAM, true, Result.FALSE, VIOLATION_WITNESS);
    Path kept = exportWitness(UNSAFE_PROGRAM, false, Result.FALSE, VIOLATION_WITNESS);

    assertThat(witnessWithoutIdentity(unrolled)).isEqualTo(witnessWithoutIdentity(kept));
  }

  @Test(timeout = 90000)
  public void correctnessWitnessReportsAboutTheSamePlacesAsWithoutUnrolling() throws Exception {
    Path unrolled = exportWitness(SAFE_PROGRAM, true, Result.TRUE, CORRECTNESS_WITNESS);
    Path kept = exportWitness(SAFE_PROGRAM, false, Result.TRUE, CORRECTNESS_WITNESS);

    // What an invariant says is a disjunction over the iterations of the loop and the order of
    // that disjunction is not fixed, so this compares the places that get one.
    assertThat(invariantPlaces(unrolled)).isEqualTo(invariantPlaces(kept));
    assertWithMessage("the loop of the program should get an invariant")
        .that(invariantPlaces(unrolled))
        .isNotEmpty();
  }

  @Test(timeout = 90000)
  public void violationWitnessOfAnUnrolledProgramIsAccepted() throws Exception {
    Path witness = exportWitness(UNSAFE_PROGRAM, true, Result.FALSE, VIOLATION_WITNESS);

    assertIsAccepted(UNSAFE_PROGRAM, witness, Result.FALSE);
  }

  @Test(timeout = 90000)
  public void correctnessWitnessOfAnUnrolledProgramIsAccepted() throws Exception {
    Path witness = exportWitness(SAFE_PROGRAM, true, Result.TRUE, CORRECTNESS_WITNESS);

    assertIsAccepted(SAFE_PROGRAM, witness, Result.TRUE);
  }

  /**
   * Analyzes the given program, with or without unrolling its loops, and returns the witness that
   * was written for the result.
   */
  private Path exportWitness(
      String pProgram, boolean pUnrollLoops, Result pExpected, String pWitnessFile)
      throws Exception {
    Configuration config =
        TestUtils.configurationForTestWithOutput(tempFolder)
            .loadFromFile(ANALYSIS)
            .setOption("specification", SPECIFICATION)
            .setOption("cfa.unrollBoundedLoops", Boolean.toString(pUnrollLoops))
            // The check of a counterexample uses a witness of the other format, which has its own
            // problems with unrolled loops and is not what this test is about.
            .setOption("analysis.checkCounterexamples", "false")
            .build();

    IntegrationTestResult result = IntegrationTestRunner.run(config, TEST_DIR + pProgram);
    // A witness of a counterexample is written while the statistics are printed, one of a proof
    // when the output files are written.
    result.cpaCheckerResult().printStatistics(discardedOutput);
    result.cpaCheckerResult().writeOutputFiles();
    result.assertIs(pExpected);

    Path outputDirectory = tempFolder.getRoot().toPath().resolve("output");
    ImmutableList<Path> witnesses;
    try (DirectoryStream<Path> written = Files.newDirectoryStream(outputDirectory, pWitnessFile)) {
      witnesses = ImmutableList.copyOf(written);
    }
    assertWithMessage("the witnesses %s of %s", pWitnessFile, outputDirectory)
        .that(witnesses)
        .hasSize(1);

    // Every run writes into the same directory, so the witness is taken out of it: the next run
    // would otherwise overwrite it or be mistaken for it.
    Path kept =
        tempFolder
            .getRoot()
            .toPath()
            .resolve((pUnrollLoops ? "unrolled" : "kept") + "-witness.yml");
    Files.copy(witnesses.get(0), kept, StandardCopyOption.REPLACE_EXISTING);
    Files.delete(witnesses.get(0));
    return kept;
  }

  /** Validates the given witness against the program, which the validation does not unroll. */
  private void assertIsAccepted(String pProgram, Path pWitness, Result pExpected) throws Exception {
    String program = TEST_DIR + pProgram;
    // The configuration of a validation is built from the witness and the specification, which
    // only the command line does, cf. AutomatonWitnessV2d0ValidationIntegrationTest.
    Configuration config =
        CPAMain.createConfiguration(
                new String[] {
                  "--witness",
                  pWitness.toString(),
                  "--spec",
                  PROPERTY,
                  "--config",
                  "config/witnessValidation.properties",
                  "--no-output-files",
                  program,
                })
            .configuration();

    IntegrationTestRunner.run(config, program).assertIs(pExpected);
  }

  /** The witness without the parts that differ between any two runs. */
  private static ImmutableList<String> witnessWithoutIdentity(Path pWitness) throws IOException {
    return FluentIterable.from(Files.readAllLines(pWitness))
        .filter(line -> !line.contains("uuid:") && !line.contains("creation_time:"))
        .toList();
  }

  /** The kinds of the invariants of the witness and the places they are about. */
  private static ImmutableList<String> invariantPlaces(Path pWitness) throws IOException {
    return FluentIterable.from(Files.readAllLines(pWitness))
        .transform(String::strip)
        .filter(line -> line.startsWith("type:") || line.startsWith("line:"))
        .toList();
  }
}
