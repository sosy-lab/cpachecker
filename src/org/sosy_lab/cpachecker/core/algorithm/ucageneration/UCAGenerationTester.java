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
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.io.TempFile;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.cpachecker.util.test.TestResults;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class UCAGenerationTester {

  private static final long TIMEOUT = 900000;

  private enum Testcases {
    COUNT("count.c"),
    COUNT2("count2.c"),
    COUNT_FLOAT("count-float.c"),
    COUNT_CHAR("count-char.c"),
    SUM_T2("sumt2.c");

    private final String name;

    Testcases(String pName) {
      name = pName;
    }
  }

  private enum UCAGenerationConfig {
    UCA2TEST("uca2Testcase"),

    TEST2UCA("testinput2UCA"),

    UCA2VIOWIT("uca2Witness"),
    VIOWIT2UCA("components/violationWitness2UCA");

    private final String fileName;

    UCAGenerationConfig(String pConfigName) {
      fileName = String.format("%s.properties", pConfigName);
    }
  }

  private static final String specificationFile = "config/specification/default.spc";
  private static final String specificationFileForWitnesses =
      "config/specification/sv-comp-reachability.spc";
  private static final String SPECIFICATION_OPTION = "specification";
  private static final String TEST_DIR_PATH = "test/programs/uca/";

  private static LogManager logger;

  static {
    try {
      logger = BasicLogManager.create(Configuration.defaultConfiguration());
    } catch (InvalidConfigurationException pE) {
      logger.log(Level.INFO, Throwables.getStackTraceAsString(pE));
    }
  }

  @Test(timeout = TIMEOUT)
  public void vioWitt2UcaForSumt2() throws Exception {
    UCATester tester =
        new UCATester(Testcases.SUM_T2, UCAGenerationConfig.VIOWIT2UCA, "assumptions.ucaFile");
    tester.setWitness(Optional.of(TEST_DIR_PATH + "cex1-sumt2.graphml"));
    tester.performTest();
  }

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

  @Test(timeout = TIMEOUT)
  public void uca2VioWitForTestForsumt2() throws Exception {
    UCATester tester =
        new UCATester(
            Testcases.SUM_T2, UCAGenerationConfig.UCA2VIOWIT, "counterexample.export.graphml");
    tester.addOverrideOption(
        "AssumptionAutomaton.cpa.automaton.inputFile", TEST_DIR_PATH + "uca-sumt2.txt");
    tester.setPathTemplate(
        Optional.of(
            TempFile.builder()
                .prefix("Counterexample")
                .suffix(".graphml")
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
    logger.logf(
        Level.INFO, "Storing putput file with option %s at %s", pOptionForOutput, outputFile);

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
    String spec = specificationFile;
    if (pGenerationConfig == UCAGenerationConfig.VIOWIT2UCA) {
      spec = specificationFileForWitnesses;
    }
    if (witness.isPresent()) {
      spec = String.format("%s,%s", spec, witness.orElseThrow());
    } else if (pGenerationConfig == UCAGenerationConfig.UCA2VIOWIT) {
      spec = "";
    }

    overrideOptions.put("counterexample.export.compressWitness", "false");
    overrideOptions.put("witness.checkProgramHash", "false");
    Configuration generationConfig =
        getProperties(pGenerationConfig.fileName, overrideOptions, spec);
    TestResults res =
        CPATestRunner.runAndPrintStatisticsAndOutput(generationConfig, pFilePath, Level.INFO);
    logger.log(Level.INFO, res.getLog());
    logger.log(Level.INFO, res.getCheckerResult().getResult());
    // TODO: Add validation of result
  }

  private static void validateTransformation(
      UCAGenerationConfig pGenerationConfig, Testcases pFilename, Path pOutputFile)
      throws IOException, CPAException {
    switch (pGenerationConfig) {
      case TEST2UCA:
        validateTest2UCA(pFilename, pOutputFile);
        break;
      case VIOWIT2UCA:
        validateViowit2UCA(pFilename, pOutputFile);
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
      case UCA2VIOWIT:
        validateUca2VioWit(pFilename, pOfFormatString);
        break;
      default:
        throw new CPAException("Cannot validate the choosen Config!");
    }
  }

  private static void validateViowit2UCA(Testcases pFilename, Path pOutputFile) throws IOException {
    List<String> expectedEdgesToQTemp = new ArrayList<>();
    if (pFilename == Testcases.SUM_T2) {
      expectedEdgesToQTemp = Lists.newArrayList("[!(!(cond))]", "[!(n <= SIZE)]", "[l < n]");
    } else {
      assertWithMessage("No tests known for  %s ", pFilename.name).fail();
    }

    assertThat(Files.exists(pOutputFile)).isTrue();
    List<String> content;
    try (Stream<String> stream = Files.lines(pOutputFile)) {
      content = stream.collect(ImmutableList.toImmutableList());
    }
    assertThat(content).isNotEmpty();

    assertThat(getAllEdgesToQTEMP(content)).containsExactlyElementsIn(expectedEdgesToQTemp);
  }

  private static List<String> getAllEdgesToQTEMP(List<String> content) {
    return content.stream()
        .filter(s -> s.contains("-> GOTO __qTEMP;"))
        .map(s -> s.substring(s.indexOf("MATCH \"") + "MATCH \"".length(), s.indexOf("\" -> GOTO")))
        .collect(ImmutableList.toImmutableList());
  }

  private static void validateUca2VioWit(Testcases pFilename, PathTemplate pOutputFile) {

    Map<Integer, List<String>> testcaseID2Assertions = new HashMap<>();
    if (pFilename == Testcases.SUM_T2) {
      testcaseID2Assertions.put(1, Lists.newArrayList("19", "23", "9"));

      Map<Integer, Integer> edges = new HashMap<>();
      edges.put(1, 10);
      Map<Integer, Integer> nodes = new HashMap<>();
      nodes.put(1, 9);
      validateWitness(pFilename, pOutputFile, 1, testcaseID2Assertions, edges, nodes);
    } else {
      assertWithMessage("No tests known for  %s ", pFilename.name).fail();
    }
  }

  private static void validateWitness(
      Testcases pFilename,
      PathTemplate pOutputFile,
      int numberOfExpectedTestcases,
      Map<Integer, List<String>> lines2Sinks,
      Map<Integer, Integer> numberEdges,
      Map<Integer, Integer> numberNodes) { // Instantiate the Factory
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    for (int i = 1; i < numberOfExpectedTestcases + 1; i++) { // Enumeration starts at 1
      Path currentFile = pOutputFile.getPath(i);
      assertThat(Files.exists(currentFile)).isTrue();
      try {
        // parse XML file
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(currentFile.toFile());
        NodeList edges = doc.getElementsByTagName("edge");
        NodeList nodes = doc.getElementsByTagName("node");
        assertThat(edges.getLength()).isEqualTo(numberEdges.get(i));
        assertThat(nodes.getLength()).isEqualTo(numberNodes.get(i));
        List<Element> edgesToSInk = new ArrayList<>();
        for (int j = 0; j < edges.getLength(); j++) {
          Node edge = edges.item(j);
          if (edge.getNodeType() == Node.ELEMENT_NODE) {

            Element element = (Element) edge;
            if ("sink".equals(element.getAttribute("target"))) {
              NodeList tl = element.getChildNodes();
              for (int k = 0; k < tl.getLength(); k++) {
                Node n = tl.item(k);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                  final String key = ((Element) n).getAttribute("key");
                  if ("startline".equals(key)) {
                    if (lines2Sinks.get(i).contains(n.getTextContent())) {
                      edgesToSInk.add(element);
                      break;
                    }
                  }
                }
              }
            }
          }
        }
        assertThat(edgesToSInk).hasSize(lines2Sinks.get(i).size());
      } catch (ParserConfigurationException | SAXException | IOException e) {
        logger.logf(
            Level.WARNING,
            "Skipping the validation for %s in iteration %d%n, due to %s",
            pFilename.name,
            i,
            Throwables.getStackTraceAsString(e));
      }
    }
  }

  private static void validateUca2Test(Testcases pFilename, PathTemplate pOutputFile)
      throws IOException {

    Map<Integer, List<String>> testcaseID2Assertions = new HashMap<>();
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
        content = stream.map(l -> l.trim()).collect(ImmutableList.toImmutableList());
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
        logger.logf(
            Level.WARNING, "Skipping the validation for %s in iteration %d%n", pFilename.name, i);
      }
    }
  }

  private static void validateTest2UCA(Testcases pFilename, Path pOutputFile) throws IOException {
    List<String> expectedAssumptions = new ArrayList<>();
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
      content = stream.collect(ImmutableList.toImmutableList());
    }
    assertThat(content).isNotEmpty();

    assertThat(getAllAssumptionsForT(content)).containsExactlyElementsIn(expectedAssumptions);
  }

  private static List<String> getAllAssumptionsForT(List<String> content) {
    return content.stream()
        .filter(s -> s.contains("t = __VERIFIER_nondet_") && s.contains("-> ASSUME {( t =="))
        .map(s -> s.substring(s.lastIndexOf("("), s.lastIndexOf(")") + 1))
        .collect(ImmutableList.toImmutableList());
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
    private final Map<String, String> overrideOptionsBuilder = new HashMap<>();
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

    @SuppressWarnings("unused")
    public void setUcaInput(Optional<String> pUcaInput) {
      ucaInput = pUcaInput;
    }

    @SuppressWarnings("unused")
    public void setTestcase(Optional<String> pTestcase) {
      testcase = pTestcase;
    }

    public void setWitness(Optional<String> pWitness) {
      witness = pWitness;
    }

    @SuppressWarnings("unused")
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
        UCAGenerationTester.performTest(
            programFile,
            generationConfig,
            overrideOptionsBuilder,
            ucaInput,
            testcase,
            witness,
            optionForOutput,
            pathTemplate.orElseThrow());
      } else {
        UCAGenerationTester.performTest(
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
