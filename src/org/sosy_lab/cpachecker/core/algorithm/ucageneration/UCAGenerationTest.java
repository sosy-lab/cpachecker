// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.ucageneration;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class UCAGenerationTest {

  private static final long TIMEOUT = 900000;

  private enum Testcases {
    COUNT("count.c"),
    COUNT2("count2.c"),
    COUNT_FLOAT("count-float.c"),
    COUNT_CHAR("count-char.c");

    private final String name;

    Testcases(String pName) {
      name = pName;
    }
  }

  private enum UCAGenerationConfig {
    UCA2TEST("uca2Testcase"),

    TEST2UCA("testinput2UCA"),

    UCA2VIOWIT("uca2Witness"),
    VIOWIT2UCA("witness2UCA");

    private final String fileName;

    UCAGenerationConfig(String pConfigName) {
      fileName = String.format("%s.properties", pConfigName);
    }
  }

  private static final String specificationFile = "config/specification/default.spc";
  private static final String SPECIFICATION_OPTION = "specification";
  private static final String TEST_DIR_PATH = "test/programs/uca/";

  private static LogManager logger = LogManager.createTestLogManager();


  @Test(timeout = TIMEOUT)
  public void test2UcaForCount() throws Exception {
    UCATester tester =
        new UCATester(Testcases.COUNT, UCAGenerationConfig.TEST2UCA, "assumptions.ucaFile");
    tester.addOverrideOption(
        "cpa.value.functionValuesForRandom", TEST_DIR_PATH + "testinput-count.xml");
    tester.performTest();
  }

  @Test(timeout = TIMEOUT)
  public void test2UcaForCountFloat() throws Exception {
    UCATester tester =
        new UCATester(Testcases.COUNT_FLOAT, UCAGenerationConfig.TEST2UCA, "assumptions.ucaFile");
    tester.addOverrideOption(
        "cpa.value.functionValuesForRandom", TEST_DIR_PATH + "testinput-count-float.xml");
    tester.performTest();
  }

  @Test(timeout = TIMEOUT)
  public void test2UcaForCountChar() throws Exception {
    UCATester tester =
        new UCATester(Testcases.COUNT_CHAR, UCAGenerationConfig.TEST2UCA, "assumptions.ucaFile");
    tester.addOverrideOption(
        "cpa.value.functionValuesForRandom", TEST_DIR_PATH + "testinput-count-char.xml");
    tester.addOverrideOption("solver.solver", "Z3");
    tester.performTest();
  }

  @Test(timeout = TIMEOUT)
  public void uca2TestForCount() throws Exception {
    UCATester tester =
        new UCATester(Testcases.COUNT, UCAGenerationConfig.UCA2TEST, "cpa.testcasegen.exportPath");
    tester.addOverrideOption(
        "AssumptionAutomaton.cpa.automaton.inputFile", TEST_DIR_PATH + "uca-count.txt");
    tester.setPathTemplate(
        Optional.of(
            TempFile.builder()
                .prefix("testcase-%d")
                .suffix(".xml")
                .create()
                .toAbsolutePath()
                .toString()));
    tester.performTest();
  }

  @Test(timeout = TIMEOUT)
  public void uca2TestForCountWith2Testcases() throws Exception {
    UCATester tester =
        new UCATester(Testcases.COUNT2, UCAGenerationConfig.UCA2TEST, "cpa.testcasegen.exportPath");
    tester.addOverrideOption(
        "AssumptionAutomaton.cpa.automaton.inputFile", TEST_DIR_PATH + "uca-count2.txt");
    tester.setPathTemplate(
        Optional.of(
            TempFile.builder()
                .prefix("testcase-%d")
                .suffix(".xml")
                .create()
                .toAbsolutePath()
                .toString()));
    tester.performTest();
  }

  @Test(timeout = TIMEOUT)
  public void uca2TestForCountFloat() throws Exception {
    UCATester tester =
        new UCATester(
            Testcases.COUNT_FLOAT, UCAGenerationConfig.UCA2TEST, "cpa.testcasegen.exportPath");
    tester.addOverrideOption(
        "AssumptionAutomaton.cpa.automaton.inputFile", TEST_DIR_PATH + "uca-count2-float.txt");
    tester.setPathTemplate(
        Optional.of(
            TempFile.builder()
                .prefix("testcase-%d")
                .suffix(".xml")
                .create()
                .toAbsolutePath()
                .toString()));
    tester.performTest();
  }

  private static void performTest(
      Testcases pFilename,
      UCAGenerationConfig pGenerationConfig,
      Map<String, String> pOverrideOptions,
      Optional<String> pUcaInput,
      Optional<String> pTestcase,
      Optional<String> pWitness,
      String pOptionForOutput)
      throws Exception {
    String fullPath = Path.of(TEST_DIR_PATH, pFilename.name).toString();

    Path outputFile =
        TempFile.builder().prefix("outputFile").suffix(".txt").create().toAbsolutePath();
    pOverrideOptions.put(pOptionForOutput, outputFile.toString());

    startTransformation(
        pGenerationConfig, fullPath, pUcaInput, pTestcase, pWitness, pOverrideOptions);

    validateTransformation(pGenerationConfig, pFilename, outputFile);
  }

  private static void performTest(
      Testcases pFilename,
      UCAGenerationConfig pGenerationConfig,
      Map<String, String> pOverrideOptions,
      Optional<String> pUcaInput,
      Optional<String> pTestcase,
      Optional<String> pWitness,
      String pOptionForOutput,
      String pTemplateForOutputValue)
      throws Exception {
    String fullPath = Path.of(TEST_DIR_PATH, pFilename.name).toString();

    pOverrideOptions.put(pOptionForOutput, pTemplateForOutputValue);

    startTransformation(
        pGenerationConfig, fullPath, pUcaInput, pTestcase, pWitness, pOverrideOptions);

    validateTransformationForFiles(
        pGenerationConfig, pFilename, PathTemplate.ofFormatString(pTemplateForOutputValue));
  }

  /**
   * Execute a uca generation config
   *
   * @param pGenerationConfig the config to execute
   * @param pFilePath the path to the testcase, should be in test/programs/uca
   * @param ucaInput an optional uca input, should be in test/programs/uca
   * @param testcase an optional testcase input, should be in test/programs/uca
   * @param witness an optional witness, should be in test/programs/uca
   * @param pOverrideOptions options to override, especially the targets for the genreated files
   * @throws Exception happening during execution
   */
  private static void startTransformation(
      UCAGenerationConfig pGenerationConfig,
      String pFilePath,
      Optional<String> ucaInput,
      Optional<String> testcase,
      Optional<String> witness,
      Map<String, String> pOverrideOptions)
      throws Exception {
    Map<String, String> overrideOptions = new LinkedHashMap<>(pOverrideOptions);
    ucaInput.ifPresent(
        pS -> overrideOptions.put("AssumptionAutomaton.cpa.automaton.inputFile", pS));
    testcase.ifPresent(pS -> overrideOptions.put("cpa.value.functionValuesForRandom", pS));
    witness.ifPresent(pS -> overrideOptions.put("witness.validation.file", pS));
    overrideOptions.put("counterexample.export.compressWitness", "false");
    overrideOptions.put("witness.checkProgramHash", "false");
    Configuration generationConfig =
        getProperties(pGenerationConfig.fileName, overrideOptions, specificationFile);
    TestResults res = CPATestRunner.runAndPrintStatistics(generationConfig, pFilePath, Level.INFO);
    logger.log(Level.INFO, res.getLog());
    // TODO: Add validation of result
  }

  private static void validateTransformation(
      UCAGenerationConfig pGenerationConfig, Testcases pFilename, Path pOutputFile)
      throws IOException, CPAException {
    switch (pGenerationConfig) {
      case TEST2UCA:
        validateTest2UCA(pFilename, pOutputFile);
        break;
      case UCA2VIOWIT:
        validateTest2UCA(pFilename, pOutputFile);
        break;
      case VIOWIT2UCA:
        validateTest2UCA(pFilename, pOutputFile);
        break;
      default:
        throw new CPAException("Cannot validate the choosen Config!");
    }
  }

  private static void validateTransformationForFiles(
      UCAGenerationConfig pGenerationConfig, Testcases pFilename, PathTemplate pOfFormatString)
      throws CPAException, IOException {
    switch (pGenerationConfig) {
      case UCA2TEST:
        validateUca2Test(pFilename, pOfFormatString);
        break;
      default:
        throw new CPAException("Cannot validate the choosen Config!");
    }
  }

  private static void validateUca2Test(Testcases pFilename, PathTemplate pOutputFile)
      throws IOException {

    Map<Integer, List<String>> testcaseID2Assertions = Maps.newHashMap();
    if (pFilename == Testcases.COUNT) {
      testcaseID2Assertions.put(
          0,
          Lists.newArrayList(
              "<input variable=\"t\" type=\"int\">2</input>",
              "<input variable=\"t\" type=\"int\">3</input>",
              "<input variable=\"t\" type=\"int\">4</input>",
              "<input variable=\"t\" type=\"int\">5</input>",
              "<input variable=\"t\" type=\"int\">6</input>",
              "<input variable=\"t\" type=\"int\">7</input>",
              "<input variable=\"t\" type=\"int\">8</input>",
              "<input variable=\"t\" type=\"int\">9</input>",
              "<input variable=\"t\" type=\"int\">0</input>"));

      validateTestcase(pFilename, pOutputFile, 1, testcaseID2Assertions);
    } else if (pFilename == Testcases.COUNT2) {
      testcaseID2Assertions.put(
          0,
          Lists.newArrayList(
              "<input variable=\"t\" type=\"int\">9</input>",
              "<input variable=\"t\" type=\"int\">0</input>"));
      testcaseID2Assertions.put(
          1,
          Lists.newArrayList(
              "<input variable=\"t\" type=\"int\">2</input>",
              "<input variable=\"t\" type=\"int\">3</input>",
              "<input variable=\"t\" type=\"int\">4</input>",
              "<input variable=\"t\" type=\"int\">5</input>",
              "<input variable=\"t\" type=\"int\">6</input>",
              "<input variable=\"t\" type=\"int\">7</input>",
              "<input variable=\"t\" type=\"int\">8</input>",
              "<input variable=\"t\" type=\"int\">9</input>",
              "<input variable=\"t\" type=\"int\">0</input>"));

      validateTestcase(pFilename, pOutputFile, 2, testcaseID2Assertions);
    } else if (pFilename == Testcases.COUNT_FLOAT) {
      testcaseID2Assertions.put(
          0,
          Lists.newArrayList(
              "<input variable=\"t\" type=\"float\">8.5</input>",
              "<input variable=\"t\" type=\"float\">10.5</input>"));
      validateTestcase(pFilename, pOutputFile, 1, testcaseID2Assertions);
    } else if (pFilename == Testcases.COUNT_CHAR) {
      testcaseID2Assertions.put(
          0,
          Lists.newArrayList(
              "<input variable=\"t\" type=\"char\">0</input>",
              "<input variable=\"t\" type=\"char\">'c'</input>"));
      validateTestcase(pFilename, pOutputFile, 1, testcaseID2Assertions);
    } else {
      assertWithMessage("No tests known for  %s ", pFilename.name).fail();
    }
  }

  private static void validateTestcase(
      Testcases pFilename,
      PathTemplate pOutputFile,
      int numberOfExpectedTestcases,
      Map<Integer, List<String>> testcaseID2Assertions)
      throws IOException {
    for (int i = 0; i < numberOfExpectedTestcases; i++) {
      Path currentFile = pOutputFile.getPath(i);
      assertThat(Files.exists(currentFile)).isTrue();
      List<String> content;
      try (Stream<String> stream = Files.lines(currentFile)) {
        content = stream.map(l -> l.trim()).collect(Collectors.toList());
      }
      assertThat(content).isNotEmpty();
      if (testcaseID2Assertions.containsKey(i)) {
        final List<String> expectedValuesInTestcase = testcaseID2Assertions.get(i);
        assertThat(content).containsAtLeastElementsIn(expectedValuesInTestcase);
        // Now, check the specific odering:
        int currentIndex = 0;
        for (String line : content) {
          if (line.equals(expectedValuesInTestcase.get(currentIndex))) {
            currentIndex = currentIndex + 1;
            if (currentIndex >= expectedValuesInTestcase.size()) {
              // We have seen all elements and the ordering is correct, hence we can break
              break;
            }
          }
        }
        // IF this test fails, the ordering of the elements is wrong
        assertThat(currentIndex).isEqualTo(expectedValuesInTestcase.size());

      } else {
        logger.logf(Level.WARNING, "Skipping the validation for %s in iteration %d%n", pFilename.name, i);
      }
    }
  }

  private static void validateTest2UCA(Testcases pFilename, Path pOutputFile) throws IOException {
    List<String> expectedAssumptions = Lists.newArrayList();
    if (pFilename == Testcases.COUNT) {
      expectedAssumptions =
          Lists.newArrayList(
              "( t == 2 )",
              "( t == 3 )",
              "( t == 4 )",
              "( t == 5 )",
              "( t == 6 )",
              "( t == 7 )",
              "( t == 8 )",
              "( t == 9 )",
              "( t == 0 )");
    } else if (pFilename == Testcases.COUNT_CHAR) {
      expectedAssumptions = Lists.newArrayList("( t == 0 )", "( t == 99 )");
    } else if (pFilename == Testcases.COUNT_FLOAT) {
      expectedAssumptions = Lists.newArrayList("( t == 8.5 )", "( t == 10.5 )");
    } else {
      assertWithMessage("No tests known for  %s ", pFilename.name).fail();
    }

    assertThat(Files.exists(pOutputFile)).isTrue();
    List<String> content;
    try (Stream<String> stream = Files.lines(pOutputFile)) {
      content = stream.collect(Collectors.toList());
    }
    assertThat(content).isNotEmpty();

    assertThat(getAllAssumptionsForT(content)).containsExactlyElementsIn(expectedAssumptions);
  }


  private static List<String> getAllAssumptionsForT(List<String> content) {
    return content.stream()
        .filter(s -> s.contains("t = __VERIFIER_nondet_") && s.contains("-> ASSUME {( t =="))
        .map(s -> s.substring(s.lastIndexOf("("), s.lastIndexOf(")") + 1))
        .collect(Collectors.toList());
  }

  private static Configuration getProperties(
      String pConfigFile, Map<String, String> pOverrideOptions, String pSpecification)
      throws InvalidConfigurationException, IOException {
    ConfigurationBuilder configBuilder =
        TestDataTools.configurationForTest().loadFromFile(Path.of("config/", pConfigFile));
    if (!Strings.isNullOrEmpty(pSpecification)) {
      pOverrideOptions.put(SPECIFICATION_OPTION, pSpecification);
    }
    pOverrideOptions.keySet().forEach(k -> configBuilder.clearOption(k));
    return configBuilder.setOptions(pOverrideOptions).build();
  }

  private static class UCATester {

    private final Testcases programFile;
    private final UCAGenerationConfig generationConfig;
    private Optional<String> ucaInput;
    private Optional<String> testcase;
    private Optional<String> witness;
    private final Map<String, String> overrideOptionsBuilder = Maps.newHashMap();
    private final String optionForOutput;
    private Optional<String> pathTemplate;

    private UCATester(
        Testcases pProgramFile, UCAGenerationConfig pGenerationConfig, String pOptionForOutput) {
      programFile = Objects.requireNonNull(pProgramFile);
      generationConfig = Objects.requireNonNull(pGenerationConfig);
      ucaInput = Optional.empty();
      testcase = Optional.empty();
      witness = Optional.empty();
      pathTemplate = Optional.empty();
      optionForOutput = pOptionForOutput;
    }

    public void setUcaInput(Optional<String> pUcaInput) {
      ucaInput = pUcaInput;
    }

    public void setTestcase(Optional<String> pTestcase) {
      testcase = pTestcase;
    }

    public void setWitness(Optional<String> pWitness) {
      witness = pWitness;
    }

    public void setPathTemplate(Optional<String> pPathTemplate) {
      pathTemplate = pPathTemplate;
    }

    @CanIgnoreReturnValue
    public UCATester addOverrideOption(String pOptionName, String pOptionValue) {
      overrideOptionsBuilder.put(pOptionName, pOptionValue);
      return this;
    }

    public void performTest() throws Exception {
      if (pathTemplate.isPresent()) {
        UCAGenerationTest.performTest(
            programFile,
            generationConfig,
            overrideOptionsBuilder,
            ucaInput,
            testcase,
            witness,
            optionForOutput,
            pathTemplate.get());
      } else {
        UCAGenerationTest.performTest(
            programFile,
            generationConfig,
            overrideOptionsBuilder,
            ucaInput,
            testcase,
            witness,
            optionForOutput);
      }
    }
  }
}
