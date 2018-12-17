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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmWithResult;
import org.sosy_lab.cpachecker.core.algorithm.testgen.util.StartupConfig;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.BDDUtils;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestSuite;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.timeout.TimeoutCPA;

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
  protected ReachedSet reachedSet = null;
  protected StartupConfig startupConfig;
  protected Specification stats;
  protected TestSuite<T> testsuite;
  protected BDDUtils bddUtils;
  protected TimeoutCPA timeoutCPA;


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
  }

  protected void writeTestsuite() {
    String outputFolder = "output/";
    String testSuiteName = "testsuite.txt";
    File testSuiteFile = new File(outputFolder + testSuiteName);
    if (!testSuiteFile.getParentFile().exists()) {
      testSuiteFile.getParentFile().mkdirs();
    }

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
