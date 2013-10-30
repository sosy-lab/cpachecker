/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.ExternalCBMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.impact.ImpactAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Resources;

@Options(prefix="analysis")
public class CPAchecker {

  public static interface CPAcheckerMXBean {
    public int getReachedSetSize();

    public void stop();
  }

  private static class CPAcheckerBean extends AbstractMBean implements CPAcheckerMXBean {

    private final ReachedSet reached;
    private final Thread cpacheckerThread;

    public CPAcheckerBean(ReachedSet pReached, LogManager logger) {
      super("org.sosy_lab.cpachecker:type=CPAchecker", logger);
      reached = pReached;
      cpacheckerThread = Thread.currentThread();
      register();
    }

    @Override
    public int getReachedSetSize() {
      return reached.size();
    }

    @Override
    public void stop() {
      cpacheckerThread.interrupt();
    }

  }

  @Option(description="stop after the first error has been found")
  private boolean stopAfterError = true;

  @Option(name="disable",
      description="stop CPAchecker after startup (internal option, not intended for users)")
  private boolean disableAnalysis = false;

  @Option(name="externalCBMC",
      description="use CBMC as an external tool from CPAchecker")
  private boolean runCBMCasExternalTool = false;

  private final LogManager logger;
  private final Configuration config;
  private final CoreComponentsFactory factory;

  /**
   * This method will throw an exception if the user has requested CPAchecker to
   * stop immediately. This exception should not be caught by the caller.
   */
  public static void stopIfNecessary() throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
  }

  // The content of this String is read from a file that is created by the
  // ant task "init".
  // To change the version, update the property in build.xml.
  private static final String version;
  static {
    String v = "(unknown version)";
    try {
      URL url = CPAchecker.class.getClassLoader().getResource("org/sosy_lab/cpachecker/VERSION.txt");
      if (url != null) {
        String content = Resources.toString(url, Charsets.US_ASCII).trim();
        if (content.matches("[a-zA-Z0-9 ._+:-]+")) {
          v = content;
        }
      }
    } catch (IOException e) {
      // Ignore exception, no better idea what to do here.
    }
    version = v;
  }

  public static String getVersion() {
    return version;
  }

  public CPAchecker(Configuration pConfiguration, LogManager pLogManager) throws InvalidConfigurationException {
    config = pConfiguration;
    logger = pLogManager;

    config.inject(this);
    factory = new CoreComponentsFactory(pConfiguration, pLogManager);
  }

  public CPAcheckerResult run(String filename) {

    logger.log(Level.INFO, "CPAchecker", getVersion(), "started");

    MainCPAStatistics stats = null;
    ReachedSet reached = null;
    Result result = Result.NOT_YET_STARTED;

    try {
      stats = new MainCPAStatistics(config, logger);

      // create reached set, cpa, algorithm
      stats.creationTime.start();
      reached = factory.createReachedSet();

      Algorithm algorithm;

      if (runCBMCasExternalTool) {
        algorithm = new ExternalCBMCAlgorithm(filename, config, logger);

      } else {
        CFA cfa = parse(filename, stats);
        GlobalInfo.getInstance().storeCFA(cfa);
        stopIfNecessary();

        ConfigurableProgramAnalysis cpa = factory.createCPA(cfa, stats);

        algorithm = factory.createAlgorithm(cpa, filename, cfa, stats);

        if (algorithm instanceof ImpactAlgorithm) {
          ImpactAlgorithm mcmillan = (ImpactAlgorithm)algorithm;
          reached.add(mcmillan.getInitialState(cfa.getMainFunction()), mcmillan.getInitialPrecision(cfa.getMainFunction()));
        } else {
          initializeReachedSet(reached, cpa, cfa.getMainFunction());
        }
      }

      printConfigurationWarnings();

      stats.creationTime.stop();
      stopIfNecessary();
      // now everything necessary has been instantiated

      if (disableAnalysis) {
        return new CPAcheckerResult(Result.NOT_YET_STARTED, null, null);
      }

      // run analysis
      result = Result.UNKNOWN; // set to unknown so that the result is correct in case of exception

      boolean isComplete = runAlgorithm(algorithm, reached, stats);

      result = analyzeResult(reached, isComplete);

    } catch (IOException e) {
      logger.logUserException(Level.SEVERE, e, "Could not read file");

    } catch (ParserException e) {
      // only log message, not whole exception because this is a C problem,
      // not a CPAchecker problem
      logger.logUserException(Level.SEVERE, e, "Parsing failed");
      logger.log(Level.INFO, "Make sure that the code was preprocessed using Cil (HowTo.txt).\n"
          + "If the error still occurs, please send this error message together with the input file to cpachecker-users@sosy-lab.org.");

    } catch (InvalidConfigurationException e) {
      logger.logUserException(Level.SEVERE, e, "Invalid configuration");

    } catch (InterruptedException e) {
      // CPAchecker must exit because it was asked to
      // we return normally instead of propagating the exception
      // so we can return the partial result we have so far

    } catch (CPAException e) {
      logger.logUserException(Level.SEVERE, e, null);
    }
    return new CPAcheckerResult(result, reached, stats);
  }

  private CFA parse(String filename, MainCPAStatistics stats) throws InvalidConfigurationException, IOException,
      ParserException, InterruptedException {
    // parse file and create CFA
    CFACreator cfaCreator = new CFACreator(config, logger);
    stats.setCFACreator(cfaCreator);

    return cfaCreator.parseFileAndCreateCFA(filename);
  }

  private void printConfigurationWarnings() {
    Set<String> unusedProperties = config.getUnusedProperties();
    if (!unusedProperties.isEmpty()) {
      logger.log(Level.WARNING, "The following configuration options were specified but are not used:\n",
          Joiner.on("\n ").join(unusedProperties), "\n");
    }
    Set<String> deprecatedProperties = config.getDeprecatedProperties();
    if (!deprecatedProperties.isEmpty()) {
      logger.log(Level.WARNING, "The following options are deprecated and will be removed in the future:\n",
          Joiner.on("\n ").join(deprecatedProperties), "\n");
    }
  }

  private boolean runAlgorithm(final Algorithm algorithm,
      final ReachedSet reached,
      final MainCPAStatistics stats) throws CPAException, InterruptedException {

    logger.log(Level.INFO, "Starting analysis ...");

    boolean isComplete = true;

    // register management interface for CPAchecker
    CPAcheckerBean mxbean = new CPAcheckerBean(reached, logger);

    stats.analysisTime.start();
    try {

      do {
        isComplete &= algorithm.run(reached);

        // either run only once (if stopAfterError == true)
        // or until the waitlist is empty
      } while (!stopAfterError && reached.hasWaitingState());

      logger.log(Level.INFO, "Stopping analysis ...");
      return isComplete;

    } finally {
      stats.analysisTime.stop();
      stats.programTime.stop();

      // unregister management interface for CPAchecker
      mxbean.unregister();
    }
  }

  private Result analyzeResult(final ReachedSet reached, boolean isComplete) {
    if (from(reached).anyMatch(IS_TARGET_STATE)) {
      return Result.UNSAFE;
    }

    if (reached.hasWaitingState()) {
      logger.log(Level.WARNING, "Analysis not completed: there are still states to be processed.");
      return Result.UNKNOWN;
    }

    if (!isComplete) {
      logger.log(Level.WARNING, "Analysis incomplete: no errors found, but not everything could be checked.");
      return Result.UNKNOWN;
    }

    return Result.SAFE;
  }

  private void initializeReachedSet(
      final ReachedSet reached,
      final ConfigurableProgramAnalysis cpa,
      final FunctionEntryNode mainFunction) {
    logger.log(Level.FINE, "Creating initial reached set");

    AbstractState initialState = cpa.getInitialState(mainFunction);
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction);

    reached.add(initialState, initialPrecision);
  }
}
