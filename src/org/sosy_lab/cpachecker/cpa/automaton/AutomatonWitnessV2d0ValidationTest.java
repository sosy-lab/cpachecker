// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cmdline.CPAMain;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class AutomatonWitnessV2d0ValidationTest {

  private String TEST_DIR_PATH = "test/programs/witness-v2-validation";
  private String SPECIFICATION_PATH = "config/properties/";

  /**
   * Tests if CPAchecker can validate a given c software with given 2.0 witnesses
   *
   * @param pFilePath The filename of the Sourcecode to analyze
   * @param pExpectedVerdict The expected verdict of the analysis
   * @param pSpecificationFilePath The specification to use for the analysis
   * @param pWitnessFilePath The filename of the witness to validate
   * @throws Exception Gets thrown if the test fails
   */
  protected void performValidationTest(
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

    TestResults results = CPATestRunner.run(generationConfig, pFilePath.toString());

    assertThat(results.getCheckerResult().getResult()).isEqualTo(pExpectedVerdict);
  }

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_1() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-invalid-witness-v2--1.yml");
    performValidationTest(inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_2() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-invalid-witness-v2--2.yml");
    performValidationTest(inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_3() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-invalid-witness-v2--3.yml");
    performValidationTest(inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_invalid_invariant_4() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-invalid-witness-v2--4.yml");
    performValidationTest(inputFilePath, Result.FALSE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_valid_invariant_1() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-valid-witness-v2--1.yml");
    performValidationTest(inputFilePath, Result.TRUE, specificationFilePath, witnessFilePath);
  }

  @Test(timeout = 3000)
  public void validate_witness_valid_invariant_2() throws Exception {
    Path specificationFilePath = Path.of(SPECIFICATION_PATH, "no-overflow.prp");
    Path inputFilePath = Path.of(TEST_DIR_PATH, "simple.c");
    Path witnessFilePath = Path.of(TEST_DIR_PATH, "simple-valid-witness-v2--2.yml");
    performValidationTest(inputFilePath, Result.TRUE, specificationFilePath, witnessFilePath);
  }
}
