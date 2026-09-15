// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.truth.Truth;
import java.nio.file.Files;
import java.nio.file.Path;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cmdline.CPAMain;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner;
import org.sosy_lab.cpachecker.util.test.IntegrationTestRunner.IntegrationTestResult;

/**
 * Helper methods shared by the witness V2 validation integration tests.
 *
 * <p>These methods used to live in a common test base class. They were extracted here so that the
 * individual test classes no longer need to inherit from each other.
 */
public final class WitnessV2ValidationTestUtils {

  private WitnessV2ValidationTestUtils() {}

  /**
   * Verifies the given software, exports the produced witness and validates it again.
   *
   * @param pFilePath The filename of the Sourcecode to analyze
   * @param pExpectedVerdict The expected verdict of the analysis
   * @param pSpecificationFilePath The specification to use for the analysis
   * @throws Exception Gets thrown if the test fails
   */
  public static void verificationPlusValidationTest(
      Path pFilePath, Result pExpectedVerdict, Path pSpecificationFilePath) throws Exception {
    Path outputPath = Files.createTempDirectory("witness-v2-validation");
    Configuration witnessGenerationConfig =
        CPAMain.createConfiguration(
                new String[] {
                  "--spec",
                  pSpecificationFilePath.toString(),
                  "--config",
                  "config/svcomp27.properties",
                  "--output-path",
                  outputPath.toString(),
                  pFilePath.toString(),
                })
            .configuration();

    IntegrationTestResult generationResult =
        IntegrationTestRunner.run(witnessGenerationConfig, pFilePath.toString());
    // Trigger statistics so that the witness is written to the file
    generationResult.cpaCheckerResult().writeOutputFiles();

    Truth.assertThat(generationResult.cpaCheckerResult().getResult()).isEqualTo(pExpectedVerdict);

    Path witnessFile = outputPath.resolve("witness.yml");
    performValidationTest(pFilePath, pExpectedVerdict, pSpecificationFilePath, witnessFile);
  }

  /**
   * Tests if CPAchecker can validate a given c software with given 2.0 witnesses
   *
   * @param pFilePath The filename of the Sourcecode to analyze
   * @param pExpectedVerdict The expected verdict of the analysis
   * @param pSpecificationFilePath The specification to use for the analysis
   * @param pWitnessFilePath The filename of the witness to validate
   * @throws Exception Gets thrown if the test fails
   */
  public static void performValidationTest(
      Path pFilePath, Result pExpectedVerdict, Path pSpecificationFilePath, Path pWitnessFilePath)
      throws Exception {

    // Due to how convoluted the config build system is, the best option to generate the correct
    // config for witness validation is to pass through the command line.
    //
    // The major challenges is that the config is overriden multiple times in its built process,
    // depending on the specification and witness type. Modelling this basically requires
    // reimplementing the parsing of options from the command line.
    //
    // Maybe at some point I will refactor this, but currently this is not worth the effort.
    // Additionally, since the particular configs may change, but we want to test the top-level
    // witnessValidation config it is necessary to use that particular one. This is to avoid
    // regressions which are not detected because the top-level config changed and now the
    // validation does no longer work.
    Configuration generationConfig =
        CPAMain.createConfiguration(
                new String[] {
                  "--witness",
                  pWitnessFilePath.toString(),
                  "--spec",
                  pSpecificationFilePath.toString(),
                  "--config",
                  "config/witnessValidation.properties",
                  "--no-output-files",
                  pFilePath.toString(),
                })
            .configuration();

    IntegrationTestResult results =
        IntegrationTestRunner.run(generationConfig, pFilePath.toString());

    Truth.assertThat(results.cpaCheckerResult().getResult()).isEqualTo(pExpectedVerdict);
  }
}
