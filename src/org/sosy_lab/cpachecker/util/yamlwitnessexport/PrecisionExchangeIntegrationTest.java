// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cmdline.CPAMain;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;

/**
 * Integration tests for the witness-based precision-exchange format.
 *
 * <p>Each test performs a full round trip: it first runs an analysis that exports the precision as
 * a YAML witness, and then runs the same analysis again importing that precision. This exercises
 * both the export and the import side of the feature for the predicate and the value analysis.
 *
 * <p>The structure follows {@code AutomatonWitnessV2d0ValidationIntegrationTest}: the configuration
 * is built through the real command line via {@link CPAMain#createConfiguration(String[])} and the
 * run is executed with {@link IntegrationTestRunner}.
 */
public class PrecisionExchangeIntegrationTest {

  @BeforeClass
  public static void skipUnlessExtendedTestsEnabled() {
    IntegrationTestRunner.skipUnlessExtendedTestsEnabled();
  }

  private static final Path SPECIFICATION = Path.of("config/properties/unreach-call.prp");

  // Safe program with rich loop invariants, so the predicate analysis refines several predicates.
  private static final Path PREDICATE_PROGRAM =
      Path.of("test/programs/witness-v2-validation/simple.c");

  // Safe program with concrete values, so the value analysis tracks concrete memory locations.
  private static final Path VALUE_PROGRAM =
      Path.of("test/programs/precision-exchange/value_tracked.c");

  @Test(timeout = 5000)
  public void predicatePrecisionRoundTrip() throws Exception {
    runRoundtripPrecisionExchange(
        "config/predicateAnalysis.properties",
        PREDICATE_PROGRAM,
        // A plain predicate analysis already writes a witness-based precision by default.
        /* pExportOptions= */ ImmutableList.of(),
        /* pExportedPrecisionFile= */ "witness-predmap-C.yml",
        /* pPrecisionType= */ "predicates",
        /* pImportOption= */ "cpa.predicate.abstraction.initialPredicates");
  }

  @Test(timeout = 5000)
  public void valuePrecisionRoundTrip() throws Exception {
    runRoundtripPrecisionExchange(
        "config/valueAnalysis.properties",
        VALUE_PROGRAM,
        // The value analysis only writes the witness precision when the option is set.
        /* pExportOptions= */ ImmutableList.of("cpa.value.witnessPrecisionFile=valuePrecision.yml"),
        /* pExportedPrecisionFile= */ "valuePrecision.yml",
        /* pPrecisionType= */ "memory_locations",
        /* pImportOption= */ "cpa.value.initialPrecisionFile");
  }

  /**
   * Runs a full precision-exchange round trip: exports the precision of an analysis to a witness
   * and then runs the same analysis again while importing that witness. Asserts that both runs
   * succeed and that a precision of the expected type was exported.
   *
   * @param pConfig the base configuration file for the analysis
   * @param pProgram the (safe) program to analyze
   * @param pExportOptions additional {@code key=value} options needed to enable the export
   * @param pExportedPrecisionFile the name of the precision witness written to the output directory
   * @param pPrecisionType the expected precision type in the exported witness (e.g. {@code
   *     predicates} or {@code memory_locations})
   * @param pImportOption the option key used to import the precision witness
   */
  private void runRoundtripPrecisionExchange(
      String pConfig,
      Path pProgram,
      ImmutableList<String> pExportOptions,
      String pExportedPrecisionFile,
      String pPrecisionType,
      String pImportOption)
      throws Exception {
    Path outputDir = Files.createTempDirectory("precision-exchange");

    // Export the precision to a witness.
    ImmutableList.Builder<String> exportArgs = ImmutableList.builder();
    exportArgs.add(
        "--config",
        pConfig,
        "--spec",
        SPECIFICATION.toString(),
        "--output-path",
        outputDir.toString());
    for (String option : pExportOptions) {
      exportArgs.add("--option", option);
    }
    exportArgs.add(pProgram.toString());
    IntegrationTestResult exportResult = runAnalysis(exportArgs.build(), pProgram);

    // The precision export is performed while the statistics are printed, so trigger them here
    // (discarding the printed statistics) to write the precision file to disk.
    exportResult
        .cpaCheckerResult()
        .printStatistics(
            new PrintStream(ByteStreams.nullOutputStream(), true, Charset.defaultCharset()));
    assertThat(exportResult.cpaCheckerResult().getResult()).isEqualTo(Result.TRUE);

    Path precisionWitness = outputDir.resolve(pExportedPrecisionFile);
    assertThat(Files.exists(precisionWitness)).isTrue();
    String precisionContent = Files.readString(precisionWitness);
    assertThat(precisionContent).contains("entry_type: \"precision_set\"");
    assertThat(precisionContent).contains("type: \"" + pPrecisionType + "\"");

    // Import the exported precision witness and check that the analysis still succeeds.
    IntegrationTestResult importResult =
        runAnalysis(
            ImmutableList.of(
                "--config",
                pConfig,
                "--spec",
                SPECIFICATION.toString(),
                "--no-output-files",
                "--option",
                pImportOption + "=" + precisionWitness,
                pProgram.toString()),
            pProgram);
    assertThat(importResult.cpaCheckerResult().getResult()).isEqualTo(Result.TRUE);
  }

  private IntegrationTestResult runAnalysis(ImmutableList<String> pArgs, Path pProgram)
      throws Exception {
    Configuration config =
        CPAMain.createConfiguration(pArgs.toArray(new String[0])).configuration();
    return IntegrationTestRunner.run(config, pProgram.toString());
  }
}
