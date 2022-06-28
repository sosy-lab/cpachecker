// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.giageneration;
//
// import static com.google.common.truth.Truth.assertThat;
// import static com.google.common.truth.Truth.assertWithMessage;
//
// import com.google.common.base.Strings;
// import com.google.common.base.Throwables;
// import com.google.common.collect.ImmutableList;
// import com.google.common.collect.Lists;
// import com.google.errorprone.annotations.CanIgnoreReturnValue;
// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.LinkedHashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Objects;
// import java.util.Optional;
// import java.util.logging.Level;
// import java.util.stream.Stream;
// import javax.xml.parsers.DocumentBuilder;
// import javax.xml.parsers.DocumentBuilderFactory;
// import javax.xml.parsers.ParserConfigurationException;
// import org.junit.Ignore;
// import org.junit.Test;
// import org.sosy_lab.common.configuration.Configuration;
// import org.sosy_lab.common.configuration.ConfigurationBuilder;
// import org.sosy_lab.common.configuration.InvalidConfigurationException;
// import org.sosy_lab.common.io.PathTemplate;
// import org.sosy_lab.common.io.TempFile;
// import org.sosy_lab.common.log.BasicLogManager;
// import org.sosy_lab.common.log.LogManager;
// import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
// import org.sosy_lab.cpachecker.exceptions.CPAException;
// import org.sosy_lab.cpachecker.util.test.CPATestRunner;
// import org.sosy_lab.cpachecker.util.test.TestDataTools;
// import org.sosy_lab.cpachecker.util.test.TestResults;
// import org.w3c.dom.Document;
// import org.w3c.dom.Element;
// import org.w3c.dom.Node;
// import org.w3c.dom.NodeList;
// import org.xml.sax.SAXException;
//
// public class GIAGenerationTester {
//
//  private static final long TIMEOUT = 900000;
//
//  private enum Testcases {
//    COUNT("count.c"),
//    COUNT2("count2.c"),
//    COUNT_FLOAT("count-float.c"),
//    COUNT_CHAR("count-char.c"),
//    SUM_T2("sumt2.c"),
//    JAIN1_1("jain_1-1.c");
//
//    private final String name;
//
//    Testcases(String pName) {
//      name = pName;
//    }
//  }
//
//  private enum GIAGenerationConfig {
//    GIA2TEST("gia2Testcase"),
//    TEST2GIA("testinput2GIA"),
//    GIA2VIOWIT("gia2Witness"),
//    GIA2CORWIT("gia2Witness"),
//    VIOWIT2GIA("components/violationWitness2GIA"),
//    CORWIT2GIA("components/correctnessWitness2GIA");
//
//    private final String fileName;
//
//    GIAGenerationConfig(String pConfigName) {
//      fileName = String.format("%s.properties", pConfigName);
//    }
//  }
//
//  private static final String specificationFile = "config/specification/default.spc";
//  private static final String specificationFileForViolationWitnesses =
//      "config/specification/sv-comp-reachability.spc";
//  private static final String SPECIFICATION_OPTION = "specification";
//  private static final String TEST_DIR_PATH = "test/programs/gia/";
//
//  private static LogManager logger;
//
//  static {
//    try {
//      logger = BasicLogManager.create(Configuration.defaultConfiguration());
//    } catch (InvalidConfigurationException pE) {
//      logger.log(Level.INFO, Throwables.getStackTraceAsString(pE));
//    }
//  }
//
//  @Test(timeout = TIMEOUT)
//  public void vioWitt2GIAForSumt2() throws Exception {
//    GIATester tester =
//        new GIATester(Testcases.SUM_T2, GIAGenerationConfig.VIOWIT2GIA, "assumptions.giaFile");
//    tester.setWitness(Optional.of(TEST_DIR_PATH + "cex1-sumt2.graphml"));
//    tester.performTest();
//  }
//
//  @Test(timeout = TIMEOUT)
//  public void gia2VioWitForTestForsumt2() throws Exception {
//    GIATester tester =
//        new GIATester(
//            Testcases.SUM_T2, GIAGenerationConfig.GIA2VIOWIT, "counterexample.export.graphml");
//    tester.addOverrideOption(
//        "AssumptionAutomaton.cpa.automaton.inputFile", TEST_DIR_PATH + "gia-sumt2.txt");
//    tester.setPathTemplate(
//        Optional.of(
//            TempFile.builder()
//                .prefix("Counterexample")
//                .suffix(".graphml")
//                .create()
//                .toAbsolutePath()
//                .toString()));
//    tester.performTest();
//  }
//
//  @Test(timeout = TIMEOUT)
//  public void corWitt2GIAForJain11() throws Exception {
//    GIATester tester =
//        new GIATester(Testcases.JAIN1_1, GIAGenerationConfig.CORWIT2GIA, "assumptions.giaFile");
//    tester.setWitness(Optional.of(TEST_DIR_PATH + "corwit-jain11.graphml"));
//    tester.performTest();
//  }
//
//  @Test(timeout = TIMEOUT)
//  public void gia2CorWitForJain11() throws Exception {
//    GIATester tester =
//        new GIATester(Testcases.JAIN1_1, GIAGenerationConfig.GIA2CORWIT, "cpa.arg.proofWitness");
//    tester.addOverrideOption(
//        "AssumptionAutomaton.cpa.automaton.inputFile", TEST_DIR_PATH + "gia-corwit-jain11.txt");
//    tester.setPathTemplate(
//        Optional.of(
//            TempFile.builder()
//                .prefix("witness")
//                .suffix(".graphml")
//                .create()
//                .toAbsolutePath()
//                .toString()));
//    tester.performTest();
//  }
//
//  @Test(timeout = TIMEOUT)
//  public void test2GIAForCount() throws Exception {
//    GIATester tester =
//        new GIATester(Testcases.COUNT, GIAGenerationConfig.TEST2GIA, "assumptions.giaFile");
//    tester.addOverrideOption(
//        "cpa.value.functionValuesForRandom", TEST_DIR_PATH + "testinput-count.xml");
//    tester.performTest();
//  }
//
//  @Test(timeout = TIMEOUT)
//  @Ignore
//  public void test2giaForCountFloat() throws Exception {
//    GIATester tester =
//        new GIATester(Testcases.COUNT_FLOAT, GIAGenerationConfig.TEST2GIA, "assumptions.giaFile");
//    tester.addOverrideOption(
//        "cpa.value.functionValuesForRandom", TEST_DIR_PATH + "testinput-count-float.xml");
//    tester.performTest();
//  }
//
//  @Test(timeout = TIMEOUT)
//  public void test2giaForCountChar() throws Exception {
//    GIATester tester =
//        new GIATester(Testcases.COUNT_CHAR, GIAGenerationConfig.TEST2GIA, "assumptions.giaFile");
//    tester.addOverrideOption(
//        "cpa.value.functionValuesForRandom", TEST_DIR_PATH + "testinput-count-char.xml");
//    tester.addOverrideOption("solver.solver", "Z3");
//    tester.performTest();
//  }
//
//  @Test(timeout = TIMEOUT)
//  public void gia2TestForCount() throws Exception {
//    GIATester tester =
//        new GIATester(Testcases.COUNT, GIAGenerationConfig.GIA2TEST,
// "cpa.testcasegen.exportPath");
//    tester.addOverrideOption(
//        "AssumptionAutomaton.cpa.automaton.inputFile", TEST_DIR_PATH + "gia-count.txt");
//    tester.setPathTemplate(
//        Optional.of(
//            TempFile.builder()
//                .prefix("testcase-%d")
//                .suffix(".xml")
//                .create()
//                .toAbsolutePath()
//                .toString()));
//    tester.performTest();
//  }
//
//  @Test(timeout = TIMEOUT)
//  public void gia2TestForCountWith2Testcases() throws Exception {
//    GIATester tester =
//        new GIATester(Testcases.COUNT2, GIAGenerationConfig.GIA2TEST,
// "cpa.testcasegen.exportPath");
//    tester.addOverrideOption(
//        "AssumptionAutomaton.cpa.automaton.inputFile", TEST_DIR_PATH + "gia-count2.txt");
//    tester.setPathTemplate(
//        Optional.of(
//            TempFile.builder()
//                .prefix("testcase-%d")
//                .suffix(".xml")
//                .create()
//                .toAbsolutePath()
//                .toString()));
//    tester.performTest();
//  }
//
//  @Test(timeout = TIMEOUT)
//  @Ignore
//  public void gia2TestForCountFloat() throws Exception {
//    GIATester tester =
//        new GIATester(
//            Testcases.COUNT_FLOAT, GIAGenerationConfig.GIA2TEST, "cpa.testcasegen.exportPath");
//    tester.addOverrideOption(
//        "AssumptionAutomaton.cpa.automaton.inputFile", TEST_DIR_PATH + "gia-count2-float.txt");
//    tester.setPathTemplate(
//        Optional.of(
//            TempFile.builder()
//                .prefix("testcase-%d")
//                .suffix(".xml")
//                .create()
//                .toAbsolutePath()
//                .toString()));
//    tester.performTest();
//  }
//
//  private static void performTest(
//      Testcases pFilename,
//      GIAGenerationConfig pGenerationConfig,
//      Map<String, String> pOverrideOptions,
//      Optional<String> pgiaInput,
//      Optional<String> pTestcase,
//      Optional<String> pWitness,
//      String pOptionForOutput)
//      throws Exception {
//    String fullPath = Path.of(TEST_DIR_PATH, pFilename.name).toString();
//
//    Path outputFile =
//        TempFile.builder().prefix("outputFile").suffix(".txt").create().toAbsolutePath();
//    pOverrideOptions.put(pOptionForOutput, outputFile.toString());
//    logger.logf(
//        Level.INFO, "Storing output file with option %s at %s", pOptionForOutput, outputFile);
//    Result result =
//        startTransformation(
//            pGenerationConfig, fullPath, pgiaInput, pTestcase, pWitness, pOverrideOptions);
//
//    validateTransformation(pGenerationConfig, pFilename, outputFile, result);
//  }
//
//  private static void performTest(
//      Testcases pFilename,
//      GIAGenerationConfig pGenerationConfig,
//      Map<String, String> pOverrideOptions,
//      Optional<String> pgiaInput,
//      Optional<String> pTestcase,
//      Optional<String> pWitness,
//      String pOptionForOutput,
//      String pTemplateForOutputValue)
//      throws Exception {
//    String fullPath = Path.of(TEST_DIR_PATH, pFilename.name).toString();
//
//    pOverrideOptions.put(pOptionForOutput, pTemplateForOutputValue);
//    Result result =
//        startTransformation(
//            pGenerationConfig, fullPath, pgiaInput, pTestcase, pWitness, pOverrideOptions);
//
//    validateTransformationForFiles(
//        pGenerationConfig, pFilename, PathTemplate.ofFormatString(pTemplateForOutputValue),
// result);
//  }
//
//  /**
//   * Execute a gia generation config
//   *
//   * @param pGenerationConfig the config to execute
//   * @param pFilePath the path to the testcase, should be in test/programs/gia
//   * @param giaInput an optional gia input, should be in test/programs/gia
//   * @param testcase an optional testcase input, should be in test/programs/gia
//   * @param witness an optional witness, should be in test/programs/gia
//   * @param pOverrideOptions options to override, especially the targets for the genreated files
//   * @throws Exception happening during execution
//   * @return the result of the analysis
//   */
//  private static Result startTransformation(
//      GIAGenerationConfig pGenerationConfig,
//      String pFilePath,
//      Optional<String> giaInput,
//      Optional<String> testcase,
//      Optional<String> witness,
//      Map<String, String> pOverrideOptions)
//      throws Exception {
//    Map<String, String> overrideOptions = new LinkedHashMap<>(pOverrideOptions);
//    giaInput.ifPresent(
//        pS -> overrideOptions.put("AssumptionAutomaton.cpa.automaton.inputFile", pS));
//    testcase.ifPresent(pS -> overrideOptions.put("cpa.value.functionValuesForRandom", pS));
//    String spec = specificationFile;
//    if (pGenerationConfig == GIAGenerationConfig.VIOWIT2GIA) {
//      spec = specificationFileForViolationWitnesses;
//    } else if (pGenerationConfig == GIAGenerationConfig.CORWIT2GIA) {
//      spec = "";
//    }
//    if (witness.isPresent()) {
//      spec = String.format("%s,%s", spec, witness.orElseThrow());
//    } else if ( // pGenerationConfig == GIAGenerationConfig.GIA2VIOWIT ||
//    pGenerationConfig == GIAGenerationConfig.GIA2CORWIT) {
//      spec = "";
//    }
//
//    overrideOptions.put("counterexample.export.compressWitness", "false");
//    overrideOptions.put("witness.checkProgramHash", "false");
//    Configuration generationConfig =
//        getProperties(pGenerationConfig.fileName, overrideOptions, spec);
//    TestResults res =
//        CPATestRunner.runAndPrintStatisticsAndOutput(generationConfig, pFilePath, Level.INFO);
//    logger.log(Level.INFO, res.getLog());
//    logger.log(Level.INFO, res.getCheckerResult().getResult());
//    // TODO: Add validation of result
//    return res.getCheckerResult().getResult();
//  }
//
//  private static void validateTransformation(
//      GIAGenerationConfig pGenerationConfig, Testcases pFilename, Path pOutputFile, Result
// pResult)
//      throws IOException, CPAException {
//    switch (pGenerationConfig) {
//      case TEST2GIA:
//        validateTest2GIA(pFilename, pOutputFile);
//        break;
//      case VIOWIT2GIA:
//        validateViowit2GIA(pFilename, pOutputFile, pResult);
//        break;
//      case CORWIT2GIA:
//        validateCorWit2GIA(pFilename, pOutputFile, pResult);
//        break;
//      default:
//        throw new CPAException("Cannot validate the choosen Config!");
//    }
//  }
//
//  private static void validateTransformationForFiles(
//      GIAGenerationConfig pGenerationConfig,
//      Testcases pFilename,
//      PathTemplate pOfFormatString,
//      Result pResult)
//      throws CPAException, IOException {
//    switch (pGenerationConfig) {
//      case GIA2TEST:
//        validateGIA2Test(pFilename, pOfFormatString);
//        break;
//      case GIA2VIOWIT:
//        validateGIA2VioWit(pFilename, pOfFormatString, pResult);
//        break;
//      case GIA2CORWIT:
//        validateGIA2CorWit(pFilename, pOfFormatString, pResult);
//        break;
//      default:
//        throw new CPAException("Cannot validate the choosen Config!");
//    }
//  }
//
//  private static void validateViowit2GIA(Testcases pFilename, Path pOutputFile, Result pResult)
//      throws IOException {
//    List<String> expectedEdgesToQTemp = new ArrayList<>();
//    if (pFilename == Testcases.SUM_T2) {
//      expectedEdgesToQTemp = Lists.newArrayList("[!(!(cond))]", "[!(n <= SIZE)]", "[l < n]");
//    } else {
//      assertWithMessage("No tests known for  %s ", pFilename.name).fail();
//    }
//    assertThat(pResult).isEqualTo(Result.FALSE);
//    assertThat(Files.exists(pOutputFile)).isTrue();
//    List<String> content;
//    try (Stream<String> stream = Files.lines(pOutputFile)) {
//      content = stream.collect(ImmutableList.toImmutableList());
//    }
//    assertThat(content).isNotEmpty();
//
//    assertThat(getAllEdgesToQTEMP(content)).containsExactlyElementsIn(expectedEdgesToQTemp);
//  }
//
//  private static void validateCorWit2GIA(Testcases pFilename, Path pOutputFile, Result pResult)
//      throws IOException {
//    Map<String, String> expectedNodesWithInvariant = new HashMap<>();
//
//    if (pFilename == Testcases.JAIN1_1) {
//      expectedNodesWithInvariant.put(
//          "N24",
//          "( ( ( ( ( y % 2 ) < 2 ) && ( ! ( y < ( y % 2 ) ) ) ) && ( ( y % 2 ) == ( ( y % 2 ) % 2
// )"
//              + " ) ) && ( ( y % 2 ) == 1 ) )");
//
//    } else {
//      assertWithMessage("No tests known for  %s ", pFilename.name).fail();
//    }
//    assertThat(pResult).isEqualTo(Result.TRUE);
//    assertThat(Files.exists(pOutputFile)).isTrue();
//    List<String> content;
//    try (Stream<String> stream = Files.lines(pOutputFile)) {
//      content = stream.collect(ImmutableList.toImmutableList());
//    }
//    assertThat(content).isNotEmpty();
//    assertThat(getAllEdgesToQTEMP(content)).isEmpty();
//    assertThat(getAllNodesWithInvAndInv(content))
//        .containsExactlyEntriesIn(expectedNodesWithInvariant);
//  }
//
//  private static Map<String, String> getAllNodesWithInvAndInv(List<String> pContent) {
//    Map<String, String> nodesWithInvariant = new HashMap<>();
//    String currentNode = "";
//    for (String line : pContent) {
//      if (line.startsWith("STATE USEALL ")) {
//        currentNode = line.substring("STATE USEALL ".length(), line.lastIndexOf(":") - 1);
//      } else if (line.startsWith("    INVARIANT ")) {
//        String inv = line.substring(line.lastIndexOf("{") + 1, line.lastIndexOf("}"));
//        nodesWithInvariant.put(currentNode, inv);
//      }
//    }
//    return nodesWithInvariant;
//  }
//
//  private static List<String> getAllEdgesToQTEMP(List<String> content) {
//    return content.stream()
//        .filter(s -> s.contains("-> GOTO __qTEMP;"))
//        .map(s -> s.substring(s.indexOf("MATCH \"") + "MATCH \"".length(), s.indexOf("\" ->
// GOTO")))
//        .collect(ImmutableList.toImmutableList());
//  }
//
//  private static void validateGIA2VioWit(
//      Testcases pFilename, PathTemplate pOutputFile, Result pResult) {
//
//    Map<Integer, List<String>> lines2Sinks = new HashMap<>();
//    if (pFilename == Testcases.SUM_T2) {
//      lines2Sinks.put(1, Lists.newArrayList("17", "31", "27"));
//
//      assertThat(pResult).isEqualTo(Result.FALSE);
//
//      Map<Integer, Integer> edges = new HashMap<>();
//      edges.put(1, 10);
//      Map<Integer, Integer> nodes = new HashMap<>();
//      nodes.put(1, 9);
//      validateWitness(pFilename, pOutputFile, 1, lines2Sinks, edges, nodes, new HashMap<>());
//    } else {
//      assertWithMessage("No tests known for  %s ", pFilename.name).fail();
//    }
//  }
//
//  private static void validateGIA2CorWit(
//      Testcases pFilename, PathTemplate pOutputFile, Result pResult) {
//
//    Map<Integer, List<String>> lines2Sinks = new HashMap<>();
//    if (pFilename == Testcases.JAIN1_1) {
//
//      assertThat(pResult).isEqualTo(Result.TRUE);
//
//      lines2Sinks.put(1, new ArrayList<>());
//      Map<Integer, Integer> edges = new HashMap<>();
//      edges.put(1, 9);
//      Map<Integer, Integer> nodes = new HashMap<>();
//      nodes.put(1, 8);
//      Map<Integer, Map<String, String>> nodesWithInv = new HashMap<>();
//      Map<String, String> invMap = new HashMap<>();
//      invMap.put("N24", "(y % (2)) == (1)");
//      nodesWithInv.put(1, invMap);
//      validateWitness(pFilename, pOutputFile, 1, lines2Sinks, edges, nodes, nodesWithInv);
//    } else {
//      assertWithMessage("No tests known for  %s ", pFilename.name).fail();
//    }
//  }
//
//  private static void validateWitness(
//      Testcases pFilename,
//      PathTemplate pOutputFile,
//      int numberOfExpectedTestcases,
//      Map<Integer, List<String>> lines2Sinks,
//      Map<Integer, Integer> numberEdges,
//      Map<Integer, Integer> numberNodes,
//      Map<Integer, Map<String, String>> pNodesWithInv) { // Instantiate the Factory
//    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//    for (int i = 1; i < numberOfExpectedTestcases + 1; i++) { // Enumeration starts at 1
//      Path currentFile = pOutputFile.getPath(i);
//      assertThat(Files.exists(currentFile)).isTrue();
//      try {
//        // parse XML file
//        DocumentBuilder db = dbf.newDocumentBuilder();
//        Document doc = db.parse(currentFile.toFile());
//        NodeList edges = doc.getElementsByTagName("edge");
//        NodeList nodes = doc.getElementsByTagName("node");
//        assertThat(edges.getLength()).isEqualTo(numberEdges.get(i));
//        assertThat(nodes.getLength()).isEqualTo(numberNodes.get(i));
//        List<String> edgesToSink = new ArrayList<>();
//        for (int j = 0; j < edges.getLength(); j++) {
//          Node edge = edges.item(j);
//          if (edge.getNodeType() == Node.ELEMENT_NODE) {
//
//            Element element = (Element) edge;
//            if ("sink".equals(element.getAttribute("target"))) {
//              NodeList tl = element.getChildNodes();
//              for (int k = 0; k < tl.getLength(); k++) {
//                Node n = tl.item(k);
//                if (n.getNodeType() == Node.ELEMENT_NODE) {
//                  final String key = ((Element) n).getAttribute("key");
//                  if ("startline".equals(key)) {
//
//                    edgesToSink.add(n.getTextContent());
//                    break;
//                  }
//                }
//              }
//            }
//          }
//        }
//        assertThat(edgesToSink).containsExactlyElementsIn(lines2Sinks.get(i));
//
//        Map<String, String> nodesToInvariant = new HashMap<>();
//        for (int j = 0; j < nodes.getLength(); j++) {
//          Node node = nodes.item(j);
//          if (node.getNodeType() == Node.ELEMENT_NODE) {
//
//            Element element = (Element) node;
//            String nodeName = element.getAttribute("id");
//            NodeList tl = element.getChildNodes();
//            for (int k = 0; k < tl.getLength(); k++) {
//              Node n = tl.item(k);
//              if (n.getNodeType() == Node.ELEMENT_NODE) {
//                final String key = ((Element) n).getAttribute("key");
//                if ("invariant".equals(key)) {
//                  nodesToInvariant.put(nodeName, n.getTextContent());
//                  break;
//                }
//              }
//            }
//          }
//        }
//        assertThat(nodesToInvariant)
//            .containsExactlyEntriesIn(pNodesWithInv.getOrDefault(i, new HashMap<>()));
//
//      } catch (ParserConfigurationException | SAXException | IOException e) {
//        assertWithMessage(
//                "Skipping the validation for %s in iteration %s, due to %s",
//                pFilename.name, Integer.toString(i), Throwables.getStackTraceAsString(e))
//            .fail();
//      }
//    }
//  }
//
//  private static void validateGIA2Test(Testcases pFilename, PathTemplate pOutputFile)
//      throws IOException {
//
//    Map<Integer, List<String>> testcaseID2Assertions = new HashMap<>();
//    if (pFilename == Testcases.COUNT) {
//      testcaseID2Assertions.put(
//          0,
//          Lists.newArrayList(
//              "<input variable=\"t\" type=\"int\">2</input>",
//              "<input variable=\"t\" type=\"int\">3</input>",
//              "<input variable=\"t\" type=\"int\">4</input>",
//              "<input variable=\"t\" type=\"int\">5</input>",
//              "<input variable=\"t\" type=\"int\">6</input>",
//              "<input variable=\"t\" type=\"int\">7</input>",
//              "<input variable=\"t\" type=\"int\">8</input>",
//              "<input variable=\"t\" type=\"int\">9</input>",
//              "<input variable=\"t\" type=\"int\">0</input>"));
//
//      validateTestcase(pFilename, pOutputFile, 1, testcaseID2Assertions);
//    } else if (pFilename == Testcases.COUNT2) {
//      testcaseID2Assertions.put(
//          0,
//          Lists.newArrayList(
//              "<input variable=\"t\" type=\"int\">9</input>",
//              "<input variable=\"t\" type=\"int\">0</input>"));
//      testcaseID2Assertions.put(
//          1,
//          Lists.newArrayList(
//              "<input variable=\"t\" type=\"int\">2</input>",
//              "<input variable=\"t\" type=\"int\">3</input>",
//              "<input variable=\"t\" type=\"int\">4</input>",
//              "<input variable=\"t\" type=\"int\">5</input>",
//              "<input variable=\"t\" type=\"int\">6</input>",
//              "<input variable=\"t\" type=\"int\">7</input>",
//              "<input variable=\"t\" type=\"int\">8</input>",
//              "<input variable=\"t\" type=\"int\">9</input>",
//              "<input variable=\"t\" type=\"int\">0</input>"));
//
//      validateTestcase(pFilename, pOutputFile, 2, testcaseID2Assertions);
//    } else if (pFilename == Testcases.COUNT_FLOAT) {
//      testcaseID2Assertions.put(
//          0,
//          Lists.newArrayList(
//              "<input variable=\"t\" type=\"float\">8.5</input>",
//              "<input variable=\"t\" type=\"float\">10.5</input>"));
//      validateTestcase(pFilename, pOutputFile, 1, testcaseID2Assertions);
//    } else if (pFilename == Testcases.COUNT_CHAR) {
//      testcaseID2Assertions.put(
//          0,
//          Lists.newArrayList(
//              "<input variable=\"t\" type=\"char\">0</input>",
//              "<input variable=\"t\" type=\"char\">'c'</input>"));
//      validateTestcase(pFilename, pOutputFile, 1, testcaseID2Assertions);
//    } else {
//      assertWithMessage("No tests known for  %s ", pFilename.name).fail();
//    }
//  }
//
//  private static void validateTestcase(
//      Testcases pFilename,
//      PathTemplate pOutputFile,
//      int numberOfExpectedTestcases,
//      Map<Integer, List<String>> testcaseID2Assertions)
//      throws IOException {
//    for (int i = 0; i < numberOfExpectedTestcases; i++) {
//      Path currentFile = pOutputFile.getPath(i);
//      assertThat(Files.exists(currentFile)).isTrue();
//      List<String> content;
//      try (Stream<String> stream = Files.lines(currentFile)) {
//        content = stream.map(l -> l.trim()).collect(ImmutableList.toImmutableList());
//      }
//      assertThat(content).isNotEmpty();
//      if (testcaseID2Assertions.containsKey(i)) {
//        final List<String> expectedValuesInTestcase = testcaseID2Assertions.get(i);
//        assertThat(content).containsAtLeastElementsIn(expectedValuesInTestcase);
//        // Now, check the specific odering:
//        int currentIndex = 0;
//        for (String line : content) {
//          if (line.equals(expectedValuesInTestcase.get(currentIndex))) {
//            currentIndex = currentIndex + 1;
//            if (currentIndex >= expectedValuesInTestcase.size()) {
//              // We have seen all elements and the ordering is correct, hence we can break
//              break;
//            }
//          }
//        }
//        // IF this test fails, the ordering of the elements is wrong
//        assertThat(currentIndex).isEqualTo(expectedValuesInTestcase.size());
//
//      } else {
//        assertWithMessage("Skipping the validation for %d in iteration %n", pFilename.name, i)
//            .fail();
//      }
//    }
//  }
//
//  private static void validateTest2GIA(Testcases pFilename, Path pOutputFile) throws IOException {
//    List<String> expectedAssumptions = new ArrayList<>();
//    if (pFilename == Testcases.COUNT) {
//      expectedAssumptions =
//          Lists.newArrayList(
//              "( t == 2 )",
//              "( t == 3 )",
//              "( t == 4 )",
//              "( t == 5 )",
//              "( t == 6 )",
//              "( t == 7 )",
//              "( t == 8 )",
//              "( t == 9 )",
//              "( t == 0 )");
//    } else if (pFilename == Testcases.COUNT_CHAR) {
//      expectedAssumptions = Lists.newArrayList("( t == 0 )", "( t == 99 )");
//    } else if (pFilename == Testcases.COUNT_FLOAT) {
//      expectedAssumptions = Lists.newArrayList("( t == 8.5 )", "( t == 10.5 )");
//    } else {
//      assertWithMessage("No tests known for  %s ", pFilename.name).fail();
//    }
//
//    assertThat(Files.exists(pOutputFile)).isTrue();
//    List<String> content;
//    try (Stream<String> stream = Files.lines(pOutputFile)) {
//      content = stream.collect(ImmutableList.toImmutableList());
//    }
//    assertThat(content).isNotEmpty();
//
//    assertThat(getAllAssumptionsForT(content)).containsExactlyElementsIn(expectedAssumptions);
//  }
//
//  private static List<String> getAllAssumptionsForT(List<String> content) {
//    return content.stream()
//        .filter(s -> s.contains("t = __VERIFIER_nondet_") && s.contains("-> ASSUME {( t =="))
//        .map(s -> s.substring(s.lastIndexOf("("), s.lastIndexOf(")") + 1))
//        .collect(ImmutableList.toImmutableList());
//  }
//
//  private static Configuration getProperties(
//      String pConfigFile, Map<String, String> pOverrideOptions, String pSpecification)
//      throws InvalidConfigurationException, IOException {
//    ConfigurationBuilder configBuilder =
//        TestDataTools.configurationForTest().loadFromFile(Path.of("config/", pConfigFile));
//    if (!Strings.isNullOrEmpty(pSpecification)) {
//      pOverrideOptions.put(SPECIFICATION_OPTION, pSpecification);
//    }
//    pOverrideOptions.keySet().forEach(k -> configBuilder.clearOption(k));
//    return configBuilder.setOptions(pOverrideOptions).build();
//  }
//
//  private static class GIATester {
//
//    private final Testcases programFile;
//    private final GIAGenerationConfig generationConfig;
//    private Optional<String> giaInput;
//    private Optional<String> testcase;
//    private Optional<String> witness;
//    private final Map<String, String> overrideOptionsBuilder = new HashMap<>();
//    private final String optionForOutput;
//    private Optional<String> pathTemplate;
//
//    private GIATester(
//        Testcases pProgramFile, GIAGenerationConfig pGenerationConfig, String pOptionForOutput) {
//      programFile = Objects.requireNonNull(pProgramFile);
//      generationConfig = Objects.requireNonNull(pGenerationConfig);
//      giaInput = Optional.empty();
//      testcase = Optional.empty();
//      witness = Optional.empty();
//      pathTemplate = Optional.empty();
//      optionForOutput = pOptionForOutput;
//    }
//
//    @SuppressWarnings("unused")
//    public void setGIAInput(Optional<String> pGIAInput) {
//      giaInput = pGIAInput;
//    }
//
//    @SuppressWarnings("unused")
//    public void setTestcase(Optional<String> pTestcase) {
//      testcase = pTestcase;
//    }
//
//    public void setWitness(Optional<String> pWitness) {
//      witness = pWitness;
//    }
//
//    @SuppressWarnings("unused")
//    public void setPathTemplate(Optional<String> pPathTemplate) {
//      pathTemplate = pPathTemplate;
//    }
//
//    @CanIgnoreReturnValue
//    public GIATester addOverrideOption(String pOptionName, String pOptionValue) {
//      overrideOptionsBuilder.put(pOptionName, pOptionValue);
//      return this;
//    }
//
//    public void performTest() throws Exception {
//      if (pathTemplate.isPresent()) {
//        GIAGenerationTester.performTest(
//            programFile,
//            generationConfig,
//            overrideOptionsBuilder,
//            giaInput,
//            testcase,
//            witness,
//            optionForOutput,
//            pathTemplate.orElseThrow());
//      } else {
//        GIAGenerationTester.performTest(
//            programFile,
//            generationConfig,
//            overrideOptionsBuilder,
//            giaInput,
//            testcase,
//            witness,
//            optionForOutput);
//      }
//    }
//  }
// }
