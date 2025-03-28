// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.CPATestRunner.ExpectedVerdict;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;
import org.yaml.snakeyaml.Yaml;

public class YAMLWitnessContentTest {

  private static final Yaml yaml = new Yaml();

  private static final String TEST_DIR_PATH = "test/programs/witnessValidation/";
  private static final String CONFIG_DIR_PATH = "config";
  private static final String SPEC_DIR_PATH = "config/specification";

  private enum TestConfig {
    SMG2("smg2"),
    SV_COMP("svcomp25");

    private final String filename;

    TestConfig(String pConfigName) {
      this.filename = String.format("%s/%s.properties", CONFIG_DIR_PATH, pConfigName);
    }
  }

  @Test
  public void testSimpleMemtrackWitnessExport() throws Exception {
    String memorysafety = String.format("%s/memorysafety.spc", SPEC_DIR_PATH);

    performExportTest(
        "simple-memtrack-unsafe.c",
        "simple-memtrack-unsafe-expected.witness.yml",
        ExpectedVerdict.FALSE,
        memorysafety,
        TestConfig.SMG2,
        ImmutableMap.of("parser.usePreprocessor", "true"));
  }

  @Test
  public void testSimpleValidWitnessExport() throws Exception {
    String memorysafety = String.format("%s/memorysafety.spc", SPEC_DIR_PATH);

    performExportTest(
        "simple-valid.c",
        "simple-valid-expected.witness.yml",
        ExpectedVerdict.FALSE,
        memorysafety,
        TestConfig.SMG2,
        ImmutableMap.of("parser.usePreprocessor", "true"));
  }

  /**
   * Fails if the exported YAML witnesses do not match the expected witnesses.
   *
   * @param pFilename The filename of the Sourcecode to analyze.
   * @param pExpectedWitnessFilename The filename of the expected witnesses.
   * @param pExpectedVerdict The expected verdict of the analysis.
   * @param pSpecificationFilePath FilePath The specification to use for the analysis.
   * @param pAnalysisType The type of analysis to perform.
   * @param pOverrideOptions Map of options to override in the configuration.
   * @throws Exception Gets thrown if the test fails.
   */
  private void performExportTest(
      String pFilename,
      String pExpectedWitnessFilename,
      ExpectedVerdict pExpectedVerdict,
      String pSpecificationFilePath,
      TestConfig pAnalysisType,
      Map<String, String> pOverrideOptions)
      throws Exception {

    String filePath = Path.of(TEST_DIR_PATH, pFilename).toString();

    Path witnessExport = TempFile.builder()
        .prefix("witness")
        .suffix(".yml")
        .create()
        .toAbsolutePath();
    witnessExport.toFile().deleteOnExit();

    generateWitness(
        filePath,
        pExpectedVerdict,
        pSpecificationFilePath,
        pAnalysisType.filename,
        pOverrideOptions,
        witnessExport.toString());

    assertWitnessContentMatches(witnessExport, Path.of(TEST_DIR_PATH, pExpectedWitnessFilename));
  }

  /**
   * Generates witnesses in 2.0 Format and writes them into the {@code pWitnessFile}.
   *
   * @param pFilePath The filename of the Sourcecode to analyze.
   * @param pExpectedVerdict The expected verdict of the analysis.
   * @param pSpecificationFilePath FilePath The specification to use for the analysis.
   * @param pConfigPath Path to the configuration file.
   * @param pOverrideOptions Map of options to override in the configuration.
   * @param pWitnessFile Path to the file to write the witness to.
   * @throws Exception Gets thrown if invalid configuration is passed or an invalid
   *     configuration-filepath is passed.
   */
  private void generateWitness(
      String pFilePath,
      ExpectedVerdict pExpectedVerdict,
      String pSpecificationFilePath,
      String pConfigPath,
      Map<String, String> pOverrideOptions,
      String pWitnessFile)
      throws Exception {
    if (pOverrideOptions == null) {
      pOverrideOptions = new LinkedHashMap<>();
    }

    Map<String, String> overrideOptions = new LinkedHashMap<>(pOverrideOptions);
    overrideOptions.put("counterexample.export.yaml", pWitnessFile);
    overrideOptions.put("counterexample.export.graphml", ""); // unset graphml export violation witness
    overrideOptions.put("cpa.arg.yamlProofWitness", pWitnessFile);
    overrideOptions.put("cpa.arg.proofWitness", ""); // unset graphml export correctness witness
    overrideOptions.put("cpa.arg.compressWitness", "false");
    Configuration generationConfig =
        generateConfiguration(pConfigPath, overrideOptions, pSpecificationFilePath);

    TestResults results = CPATestRunner.run(generationConfig, pFilePath);

    // Trigger statistics so that the witness is written to the file
    results.getCheckerResult().writeOutputFiles();
    results.assertIs(Result.valueOf(pExpectedVerdict.toString()));
  }

  /**
   * Asserts that the content of the witnesses generated by cpachecker matches the expected content.
   *
   * @param pWitnessExport Path to the exported witness file.
   * @param pExpectedWitnessFilename Path to the expected content file.
   */
  private void assertWitnessContentMatches(Path pWitnessExport, Path pExpectedWitnessFilename) {
    try (InputStream witnessExportStream = new FileInputStream(pWitnessExport.toFile());
        InputStream expectedWitnessStream =
            new FileInputStream(pExpectedWitnessFilename.toFile())) {
      List<Map<String, Object>> witnessExportYAML = yaml.load(witnessExportStream);
      List<Map<String, Object>> expectedWitnessYAML = yaml.load(expectedWitnessStream);

      assertThat(witnessExportYAML.get(0).get("content")).isEqualTo(expectedWitnessYAML.get(0).get("content"));

    } catch (IOException e) {
      throw new AssertionError("Could not read witness export file", e);
    }
  }

  /**
   * Builds a {@link Configuration} object from the given configuration file, the given override
   * options, and adds the specification to the configuration.
   *
   * @param pConfigFile The path to the configuration file. (Relative to root)
   * @param pOverrideOptions A map of options to override in the configuration.
   * @param pSpecification The path to the specification. (Relative to root)
   * @return A {@link Configuration} object to pass into the {@link CPATestRunner}.
   * @throws InvalidConfigurationException If the configuration is invalid.
   * @throws IOException If the configuration file could not be read.
   */
  private Configuration generateConfiguration(
      String pConfigFile, Map<String, String> pOverrideOptions, String pSpecification)
      throws InvalidConfigurationException, IOException {
    ConfigurationBuilder configBuilder =
        TestDataTools.configurationForTest().loadFromFile(pConfigFile);
    if (!Strings.isNullOrEmpty(pSpecification)) {
      pOverrideOptions.put("specification", pSpecification);
    }
    return configBuilder.setOptions(pOverrideOptions).build();
  }
}
