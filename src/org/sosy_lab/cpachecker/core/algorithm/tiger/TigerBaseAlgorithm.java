/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.cpachecker.core.algorithm.tiger;

import com.google.common.base.Function;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmWithResult;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.BDDUtils;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.cpa.arg.ErrorPathShrinker;
import org.sosy_lab.cpachecker.cpa.timeout.TimeoutCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

public abstract class TigerBaseAlgorithm<T extends Goal>
    implements AlgorithmWithResult, ShutdownRequestListener {
  enum TimeoutStrategy {
    SKIP_AFTER_TIMEOUT,
    RETRY_AFTER_TIMEOUT
  }
  enum ReachabilityAnalysisResult {
    SOUND,
    UNSOUND,
    TIMEDOUT
  }

  public static String originalMainFunction = null;
  protected TigerAlgorithmConfiguration tigerConfig;
  protected int currentTestCaseID;
  protected InputOutputValues values;
  protected LogManager logger;
  protected CFA cfa;
  protected ConfigurableProgramAnalysis cpa;
  protected Configuration config;
  // protected ReachedSet reachedSet = null;
  protected StartupConfig startupConfig;
  protected Specification stats;
  protected TestSuite<T> testsuite;
  protected BDDUtils bddUtils;
  protected TimeoutCPA timeoutCPA;

  protected LinkedList<T> goalsToCover;

  protected void init(
      LogManager pLogger,
      CFA pCfa,
      Configuration pConfig,
      ConfigurableProgramAnalysis pCpa,
      ShutdownNotifier pShutdownNotifier,
      @Nullable final Specification pStats)
      throws InvalidConfigurationException {
    tigerConfig = new TigerAlgorithmConfiguration(pConfig);
    cfa = pCfa;
    cpa = pCpa;
    startupConfig = new StartupConfig(pConfig, pLogger, pShutdownNotifier);
    // startupConfig.getConfig().inject(this);
    logger = pLogger;
    assert originalMainFunction != null;
    config = pConfig;

    logger.logf(Level.INFO, "FQL query string: %s", tigerConfig.getFqlQuery());
    this.stats = pStats;
    values =
        new InputOutputValues(tigerConfig.getInputInterface(), tigerConfig.getOutputInterface());
    currentTestCaseID = 0;

    // Check if BDD is enabled for variability-aware test-suite generation
    bddUtils = new BDDUtils(cpa, pLogger);
    timeoutCPA = getTimeoutCPA(cpa);
  }

  public TimeoutCPA getTimeoutCPA(ConfigurableProgramAnalysis pCpa) {
    if (pCpa instanceof WrapperCPA) {
      TimeoutCPA bddCpa = ((WrapperCPA) pCpa).retrieveWrappedCpa(TimeoutCPA.class);
      return bddCpa;
    } else if (pCpa instanceof TimeoutCPA) {
      return ((TimeoutCPA) pCpa);
    }

    return null;
  }

  protected Pair<Boolean, Boolean>
      runAlgorithm(Algorithm algorithm, ReachedSet pReachedSet)
          throws CPAEnabledAnalysisPropertyViolationException, CPAException, InterruptedException {
    boolean analysisWasSound = false;
    boolean hasTimedOut = false;
    analysisWasSound = algorithm.run(pReachedSet).isSound();
    hasTimedOut = timeoutCPA.hasTimedout();
    if (hasTimedOut) {
      logger.logf(Level.INFO, "Test goal timed out!");
    }
    return Pair.of(analysisWasSound, hasTimedOut);
  }

  private Element createAndAppendElement(
      String elementName,
      String elementTest,
      Element parentElement,
      Document dom) {
    Element newElement = dom.createElement(elementName);
    newElement.appendChild(dom.createTextNode(elementTest));
    parentElement.appendChild(newElement);
    return newElement;
  }

  private static String getFileChecksum(MessageDigest digest, File file) throws IOException {
    // Get file input stream for reading the file content
    FileInputStream fis = new FileInputStream(file);

    // Create byte array to read data in chunks
    byte[] byteArray = new byte[1024];
    int bytesCount = 0;

    // Read file data and update in message digest
    while ((bytesCount = fis.read(byteArray)) != -1) {
      digest.update(byteArray, 0, bytesCount);
    } ;

    // close the stream; We don't need it now.
    fis.close();

    // Get the hash's bytes
    byte[] bytes = digest.digest();

    // This bytes[] has bytes in decimal format;
    // Convert it to hexadecimal format
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
      sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
    }

    // return complete hash
    return sb.toString();
  }

  private void writeMetaData() {
    Document dom;
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder db = dbf.newDocumentBuilder();
      dom = db.newDocument();
      Element root = dom.createElement("test-metadata");

      createAndAppendElement("sourcecodelang", "C", root, dom);
      createAndAppendElement("producer", "CPA-Tiger", root, dom);
      createAndAppendElement("specification", tigerConfig.getFqlQuery(), root, dom);
      Path file = cfa.getFileNames().get(0);
      createAndAppendElement("programfile", file.toString(), root, dom);
      createAndAppendElement(
          "programhash",
          getFileChecksum(MessageDigest.getInstance("MD5"), file.toFile()),
          root,
          dom);
      createAndAppendElement("entryfunction", originalMainFunction, root, dom);

      String architecture = "32bit";
      @org.checkerframework.checker.nullness.qual.Nullable
      String prop = null;
      try {
        config.getProperty("Architecture");
      } catch (Exception ex) {
        // ignore for now
      }
      if (prop != null && !prop.isEmpty()) {
        architecture = prop;
      }
      createAndAppendElement("architecture", architecture, root, dom);
      Timestamp timestamp = new Timestamp(System.currentTimeMillis());
      Instant instant = timestamp.toInstant();
      createAndAppendElement("creationtime", instant.toString(), root, dom);

      dom.appendChild(root);
      try {
        Transformer tr = TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        tr.setOutputProperty(OutputKeys.METHOD, "xml");
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        DOMImplementation domImpl = dom.getImplementation();
        DocumentType doctype =
            domImpl.createDocumentType(
                "doctype",
                "+//IDN sosy-lab.org//DTD test-format test-metadata 1.0//EN",
                "https://gitlab.com/sosy-lab/software/test-format/raw/v1.0/test-metadata.dtd");
        tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
        tr.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());

        tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        // send DOM to file
        tr.transform(
            new DOMSource(dom),
            new StreamResult(new FileOutputStream("output/metadata.xml")));

      } catch (TransformerException te) {
        logger.log(Level.WARNING, te.getMessage());
      } catch (IOException ioe) {
        logger.log(Level.WARNING, ioe.getMessage());
      }
    } catch (ParserConfigurationException pce) {
      logger.log(Level.WARNING, "UsersXML: Error trying to instantiate DocumentBuilder " + pce);
    } catch (NoSuchAlgorithmException e) {
      logger.log(Level.WARNING, e.getMessage());
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage());
    }
  }

  private void writeTestCases() {
    for (TestCase testcase : testsuite.getTestCases()) {
      Document dom;
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      try {
        DocumentBuilder db = dbf.newDocumentBuilder();
        dom = db.newDocument();
        Element root = dom.createElement("testcase");
        // TODO order of variables if important for testcomp!
        for (Entry<String, BigInteger> var : testcase.getInputs().entrySet()) {
          Element input = createAndAppendElement("input", var.getValue().toString(), root, dom);
          input.setAttribute("variable", var.getKey());
        }

        dom.appendChild(root);
        try {
          Transformer tr = TransformerFactory.newInstance().newTransformer();
          tr.setOutputProperty(OutputKeys.INDENT, "yes");
          tr.setOutputProperty(OutputKeys.METHOD, "xml");
          tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
          DOMImplementation domImpl = dom.getImplementation();
          DocumentType doctype =
              domImpl.createDocumentType(
                  "doctype",
                  "+//IDN sosy-lab.org//DTD test-format testcase 1.0//EN",
                  "https://gitlab.com/sosy-lab/software/test-format/raw/v1.0/testcase.dtd");
          tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
          tr.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());

          tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

          // send DOM to file
          tr.transform(
              new DOMSource(dom),
              new StreamResult(
                  new FileOutputStream("output/testcase-" + testcase.getId() + ".xml")));

        } catch (TransformerException te) {
          logger.log(Level.WARNING, te.getMessage());
        } catch (IOException ioe) {
          logger.log(Level.WARNING, ioe.getMessage());
        }
      } catch (ParserConfigurationException pce) {
        logger.log(Level.WARNING, "UsersXML: Error trying to instantiate DocumentBuilder " + pce);
      }
    }
  }

  protected void writeTestsuite() {
    String outputFolder = "output/";
    String testSuiteName = "testsuite.txt";
    File testSuiteFile = new File(outputFolder + testSuiteName);
    if (!testSuiteFile.getParentFile().exists()) {
      testSuiteFile.getParentFile().mkdirs();
    }

    if (tigerConfig.shouldUseTestCompOutput()) {
      writeMetaData();
      writeTestCases();
    } else {
      try (Writer writer =
          new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream("output/testsuite.txt"), "utf-8"))) {
        writer.write(testsuite.toString());
        writer.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      try (Writer writer =
          new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream("output/testsuite.json"), "utf-8"))) {
        writer.write(testsuite.toJsonString());
        writer.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  protected TestCase createTestcase(final CounterexampleInfo cex, final Region pPresenceCondition) {
    Map<String, BigInteger> inputValues = values.extractInputValues(cex);
    Map<String, BigInteger> outputValus = values.extractOutputValues(cex);
    // calcualte shrinked error path
    List<Pair<CFAEdgeWithAssumptions, Boolean>> shrinkedErrorPath =
        new ErrorPathShrinker()
            .shrinkErrorPath(cex.getTargetPath(), cex.getCFAPathWithAssignments());
    TestCase testcase =
        new TestCase(
            currentTestCaseID,
            inputValues,
            outputValus,
            cex.getTargetPath().asEdgesList(),
            shrinkedErrorPath,
            pPresenceCondition,
            bddUtils);
    currentTestCaseID++;
    return testcase;
  }
  public Region
      getPresenceConditionFromCexUpToEdge(
          CounterexampleInfo cex,
          Function<CFAEdge, Boolean> isFinalEdgeForGoal) {
    if (!bddUtils.isVariabilityAware()) {
      return null;
    }

    Region pc = bddUtils.makeTrue();
    List<CFAEdge> cfaPath = cex.getTargetPath().getFullPath();
    String validFunc = tigerConfig.getValidProductMethodName();

    for (CFAEdge cfaEdge : cfaPath) {
      String predFun = cfaEdge.getPredecessor().getFunctionName();
      String succFun = cfaEdge.getSuccessor().getFunctionName();
      if (predFun.contains(validFunc)
          && succFun.contains(tigerConfig.getValidProductMethodName())) {
        continue;
      }

      if (cfaEdge instanceof CAssumeEdge) {
        CAssumeEdge assumeEdge = (CAssumeEdge) cfaEdge;
        if (assumeEdge.getExpression() instanceof CBinaryExpression) {

          CBinaryExpression expression = (CBinaryExpression) assumeEdge.getExpression();
          String name = expression.getOperand1().toString() + "@0";

          if (name.contains(tigerConfig.getFeatureVariablePrefix())) {
            Region predNew = bddUtils.createPredicate(name);
            if (assumeEdge.getTruthAssumption()) {
              predNew = bddUtils.makeNot(predNew);
            }

            pc = bddUtils.makeAnd(pc, predNew);
          }
        }
      }
      if (isFinalEdgeForGoal.apply(cfaEdge)) {
        break;
      }
    }

    return pc;
  }

  protected void checkGoalCoverage(
      Set<T> pGoalsToCheckCoverage,
      TestCase testCase,
      boolean removeCoveredGoals,
      CounterexampleInfo cex) {
    for (T goal : testCase.getCoveredGoals(pGoalsToCheckCoverage)) {
      // TODO add infeasiblitpropagaion to testsuite
      Region simplifiedPresenceCondition = getPresenceConditionFromCexForGoal(cex, goal);
      testsuite.updateTestcaseToGoalMapping(testCase, goal, simplifiedPresenceCondition);
      String log = "Goal " + goal.getName() + " is covered by testcase " + testCase.getId();
      if (removeCoveredGoals && !bddUtils.isVariabilityAware()) {
        pGoalsToCheckCoverage.remove(goal);
        log += "and is removed from goal list";
      }
      logger.log(Level.INFO, log);
    }
  }

  protected abstract Region getPresenceConditionFromCexForGoal(CounterexampleInfo pCex, T pGoal);

  protected Algorithm rebuildAlgorithm(
      ShutdownManager algNotifier,
      ConfigurableProgramAnalysis lARTCPA,
      ReachedSet pReached)
      throws CPAException {
    Algorithm algorithm;
    try {
      Configuration internalConfiguration =
          Configuration.builder().loadFromFile(tigerConfig.getAlgorithmConfigurationFile()).build();

      Set<UnmodifiableReachedSet> unmodifiableReachedSets = new HashSet<>();

      unmodifiableReachedSets.add(pReached);

      AggregatedReachedSets aggregatedReachedSets =
          new AggregatedReachedSets(unmodifiableReachedSets);

      CoreComponentsFactory coreFactory =
          new CoreComponentsFactory(
              internalConfiguration,
              logger,
              algNotifier.getNotifier(),
              aggregatedReachedSets);

      algorithm = coreFactory.createAlgorithm(lARTCPA, cfa, stats);

      if (algorithm instanceof CEGARAlgorithm) {
        CEGARAlgorithm cegarAlg = (CEGARAlgorithm) algorithm;

        ARGStatistics lARTStatistics;
        try {
          lARTStatistics = new ARGStatistics(internalConfiguration, logger, lARTCPA, stats, cfa);
        } catch (InvalidConfigurationException e) {
          throw new RuntimeException(e);
        }
        Set<Statistics> lStatistics = new HashSet<>();
        lStatistics.add(lARTStatistics);
        cegarAlg.collectStatistics(lStatistics);
      }

    } catch (IOException | InvalidConfigurationException e) {
      throw new RuntimeException(e);
    }
    return algorithm;
  }

  protected void initializeReachedSet(ReachedSet pReachedSet, ARGCPA lRTCPA)
      throws InterruptedException {
    // initialize reachedSet
    pReachedSet.clear();
    AbstractState lInitialElement =
        lRTCPA.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
    Precision lInitialPrecision =
        lRTCPA
            .getInitialPrecision(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
    pReachedSet.add(lInitialElement, lInitialPrecision);
  }

  @Override
  public AlgorithmResult getResult() {
    return testsuite;
  }
}
