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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.BMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.ExternalCBMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartWithConditionsAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.AbstractElements;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;

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

  @Options
  private static class CPAcheckerOptions {

    @Option(name="analysis.useAssumptionCollector",
        description="use assumption collecting algorithm")
        boolean useAssumptionCollector = false;

    @Option(name="analysis.useAdjustableConditions",
        description="use adjustable conditions algorithm")
        boolean useAdjustableConditions = false;

    @Option(name = "analysis.useRefinement",
        description = "use CEGAR algorithm for lazy counter-example guided analysis"
          + "\nYou need to specify a refiner with the cegar.refiner option."
          + "\nCurrently all refiner require the use of the ARTCPA.")
          boolean useRefinement = false;

    @Option(name="analysis.useCBMC",
        description="use CBMC to double-check counter-examples")
        boolean useCBMC = false;

    @Option(name="analysis.useBMC",
        description="use a BMC like algorithm that checks for satisfiability "
          + "after the analysis has finished, works only with PredicateCPA")
          boolean useBMC = false;

    @Option(name="analysis.stopAfterError",
        description="stop after the first error has been found")
        boolean stopAfterError = true;

    @Option(name="analysis.restartAfterUnknown",
        description="restart the algorithm using a different CPA after unknown result")
        boolean useRestartingAlgorithm = false;

    @Option(name="analysis.externalCBMC",
        description="use CBMC as an external tool from CPAchecker")
        boolean runCBMCasExternalTool = false;

    @Option(name="analysis.disable",
        description="stop CPAchecker after startup (internal option, not intended for users)")
        boolean disableAnalysis = false;
  }

  private final LogManager logger;
  private final Configuration config;
  private final CPAcheckerOptions options;
  private final ReachedSetFactory reachedSetFactory;

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

    options = new CPAcheckerOptions();
    config.inject(options);
    reachedSetFactory = new ReachedSetFactory(pConfiguration, pLogManager);
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
      reached = reachedSetFactory.create();

      Algorithm algorithm;

      if (options.runCBMCasExternalTool) {
        algorithm = new ExternalCBMCAlgorithm(filename, config, logger);

      } else {
        CFA cfa = parse(filename, stats);
        stopIfNecessary();

        ConfigurableProgramAnalysis cpa = createCPA(stats, cfa);

        algorithm = createAlgorithm(cpa, stats, filename, cfa);

        if (algorithm instanceof RestartAlgorithm) {
          // this algorithm needs an indirection so that it can change
          // the actual reached set instance on the fly
          reached = new ForwardingReachedSet(reached);
        }

        initializeReachedSet(reached, cpa, cfa.getMainFunction());
      }

      printConfigurationWarnings();

      stats.creationTime.stop();
      stopIfNecessary();
      // now everything necessary has been instantiated

      if (options.disableAnalysis) {
        return new CPAcheckerResult(Result.NOT_YET_STARTED, null, null);
      }

      // run analysis
      result = Result.UNKNOWN; // set to unknown so that the result is correct in case of exception

      boolean sound = runAlgorithm(algorithm, reached, stats);

      result = analyzeResult(reached, sound);

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

    boolean sound = true;

    // register management interface for CPAchecker
    CPAcheckerBean mxbean = new CPAcheckerBean(reached, logger);

    stats.analysisTime.start();
    try {

      do {
        sound &= algorithm.run(reached);

        // either run only once (if stopAfterError == true)
        // or until the waitlist is empty
      } while (!options.stopAfterError && reached.hasWaitingElement());

      logger.log(Level.INFO, "Stopping analysis ...");
      return sound;

    } finally {
      stats.analysisTime.stop();
      stats.programTime.stop();

      // unregister management interface for CPAchecker
      mxbean.unregister();
    }
  }

  private Result analyzeResult(final ReachedSet reached, boolean sound) {
    if (Iterables.any(reached, AbstractElements.IS_TARGET_ELEMENT)) {
      return Result.UNSAFE;
    }

    if (reached.hasWaitingElement()) {
      logger.log(Level.WARNING, "Analysis not completed: there are still elements to be processed.");
      return Result.UNKNOWN;
    }

    if (!sound) {
      logger.log(Level.WARNING, "Analysis incomplete: no errors found, but not everything could be checked.");
      return Result.UNKNOWN;
    }

    return Result.SAFE;
  }

  private ConfigurableProgramAnalysis createCPA(MainCPAStatistics stats, CFA cfa) throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating CPAs");
    stats.cpaCreationTime.start();
    try {

      if (options.useRestartingAlgorithm) {
        // hard-coded dummy CPA
        return LocationCPA.factory().set(cfa, CFA.class).createInstance();
      }

      CPABuilder builder = new CPABuilder(config, logger, reachedSetFactory, cfa);
      ConfigurableProgramAnalysis cpa = builder.buildCPAs();

      if (cpa instanceof StatisticsProvider) {
        ((StatisticsProvider)cpa).collectStatistics(stats.getSubStatistics());
      }
      return cpa;

    } finally {
      stats.cpaCreationTime.stop();
    }
  }

  private Algorithm createAlgorithm(final ConfigurableProgramAnalysis cpa,
        final MainCPAStatistics stats, final String filename, CFA cfa)
        throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating algorithms");

    Algorithm algorithm;

    if (options.useRestartingAlgorithm) {
      logger.log(Level.INFO, "Using Restarting Algorithm");
      algorithm = new RestartAlgorithm(config, logger, filename, cfa);

    } else {
      algorithm = new CPAAlgorithm(cpa, logger);

      if (options.useRefinement) {
        algorithm = new CEGARAlgorithm(algorithm, cpa, config, logger);
      }

      if (options.useBMC) {
        algorithm = new BMCAlgorithm(algorithm, cpa, config, logger, reachedSetFactory, cfa);
      }

      if (options.useCBMC) {
        algorithm = new CounterexampleCheckAlgorithm(algorithm, cpa, config, logger, reachedSetFactory, cfa);
      }

      if (options.useAssumptionCollector) {
        algorithm = new AssumptionCollectorAlgorithm(algorithm, cpa, config, logger);
      }

      if (options.useAdjustableConditions) {
        algorithm = new RestartWithConditionsAlgorithm(algorithm, cpa, config, logger);
      }

    }

    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(stats.getSubStatistics());
    }
    return algorithm;
  }

  private void initializeReachedSet(
      final ReachedSet reached,
      final ConfigurableProgramAnalysis cpa,
      final CFAFunctionDefinitionNode mainFunction) {
    logger.log(Level.FINE, "Creating initial reached set");

    AbstractElement initialElement = cpa.getInitialElement(mainFunction);
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction);

    reached.add(initialElement, initialPrecision);
  }
}
