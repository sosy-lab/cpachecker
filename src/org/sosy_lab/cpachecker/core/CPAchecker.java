/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.BMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.CounterexampleCheckAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.RestartAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.AbstractElements;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;

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

  public CPAchecker(Configuration pConfiguration, LogManager pLogManager) throws InvalidConfigurationException {
    config = pConfiguration;
    logger = pLogManager;

    options = new CPAcheckerOptions();
    config.inject(options);
    reachedSetFactory = new ReachedSetFactory(pConfiguration);
  }

  public CPAcheckerResult run(String filename) {

    logger.log(Level.INFO, "CPAchecker started");

    MainCPAStatistics stats = null;
    ReachedSet reached = null;
    Result result = Result.UNKNOWN;

    if(options.useRestartingAlgorithm){
      logger.log(Level.INFO, "Using Restarting Algorithm");

      try {
        stats = new MainCPAStatistics(config, logger);

        // create parser, cpa, algorithm
        stats.creationTime.start();

        CFACreator cfaCreator = new CFACreator(config, logger);
        stats.setCFACreator(cfaCreator);

        // create CFA
        cfaCreator.parseFileAndCreateCFA(filename);

        if (cfaCreator.getFunctions().isEmpty()) {
          // empty program, do nothing
          return new CPAcheckerResult(Result.UNKNOWN, null, null);
        }

        Pair<Algorithm, ReachedSet> algorithmReachedPair = createRestartAlgorithm(config, stats, cfaCreator);

        Set<String> unusedProperties = config.getUnusedProperties();
        if (!unusedProperties.isEmpty()) {
          logger.log(Level.WARNING, "The following configuration options were specified but are not used:\n",
              Joiner.on("\n ").join(unusedProperties), "\n");
        }

        stats.creationTime.stop();

        stopIfNecessary();

        // register management interface for CPAchecker
        CPAcheckerBean mxbean = new CPAcheckerBean(reached, logger);
        try {

          result = runAlgorithm(algorithmReachedPair.getFirst(), algorithmReachedPair.getSecond(), stats);

        } finally {
          // unregister management interface for CPAchecker
          mxbean.unregister();
        }

      } catch (IOException e) {
        logger.log(Level.SEVERE, "Could not read file", filename,
            (e.getMessage() != null ? "(" + e.getMessage() + ")" : ""));

      } catch (ParserException e) {
        // only log message, not whole exception because this is a C problem,
        // not a CPAchecker problem
        logger.log(Level.SEVERE, Throwables.getRootCause(e).getMessage());
        logger.log(Level.INFO, "Make sure that the code was preprocessed using Cil (HowTo.txt).\n"
            + "If the error still occurs, please send this error message together with the input file to cpachecker-users@sosy-lab.org.");

      } catch (InvalidConfigurationException e) {
        logger.log(Level.SEVERE, "Invalid configuration:", e.getMessage());

      } catch (UnsatisfiedLinkError e) {
        if (e.getMessage().contains("libgmpxx.so.4")) {
          logger.log(Level.SEVERE, "Error: The GNU Multiprecision arithmetic library is required, but missing on this system!\n"
              + "Please install libgmpxx.so.4 and try again.\n"
              + "On Ubuntu you need to install the package 'libgmpxx4ldbl'.");
        } else {
          logger.logException(Level.SEVERE, e, null);
        }

      } catch (InterruptedException e) {
        // CPAchecker must exit because it was asked to
        // we return normally instead of propagating the exception
        // so we can return the partial result we have so far

      } catch (CPAException e) {
        logger.logException(Level.SEVERE, e, null);
      }

    }

    else{
      try {
        stats = new MainCPAStatistics(config, logger);

        // create parser, cpa, algorithm
        stats.creationTime.start();

        CFACreator cfaCreator = new CFACreator(config, logger);
        stats.setCFACreator(cfaCreator);

        ConfigurableProgramAnalysis cpa = createCPA(stats);

        Algorithm algorithm = createAlgorithm(cpa, stats);

        Set<String> unusedProperties = config.getUnusedProperties();
        if (!unusedProperties.isEmpty()) {
          logger.log(Level.WARNING, "The following configuration options were specified but are not used:\n",
              Joiner.on("\n ").join(unusedProperties), "\n");
        }

        stats.creationTime.stop();

        stopIfNecessary();

        // create CFA
        cfaCreator.parseFileAndCreateCFA(filename);

        if (cfaCreator.getFunctions().isEmpty()) {
          // empty program, do nothing
          return new CPAcheckerResult(Result.UNKNOWN, null, null);
        }

        reached = createInitialReachedSet(cpa, cfaCreator.getMainFunction());

        stopIfNecessary();

        // register management interface for CPAchecker
        CPAcheckerBean mxbean = new CPAcheckerBean(reached, logger);
        try {

          result = runAlgorithm(algorithm, reached, stats);

        } finally {
          // unregister management interface for CPAchecker
          mxbean.unregister();
        }

      } catch (IOException e) {
        logger.log(Level.SEVERE, "Could not read file", filename,
            (e.getMessage() != null ? "(" + e.getMessage() + ")" : ""));

      } catch (ParserException e) {
        // only log message, not whole exception because this is a C problem,
        // not a CPAchecker problem
        logger.log(Level.SEVERE, Throwables.getRootCause(e).getMessage());
        logger.log(Level.INFO, "Make sure that the code was preprocessed using Cil (HowTo.txt).\n"
            + "If the error still occurs, please send this error message together with the input file to cpachecker-users@sosy-lab.org.");

      } catch (InvalidConfigurationException e) {
        logger.log(Level.SEVERE, "Invalid configuration:", e.getMessage());

      } catch (UnsatisfiedLinkError e) {
        if (e.getMessage().contains("libgmpxx.so.4")) {
          logger.log(Level.SEVERE, "Error: The GNU Multiprecision arithmetic library is required, but missing on this system!\n"
              + "Please install libgmpxx.so.4 and try again.\n"
              + "On Ubuntu you need to install the package 'libgmpxx4ldbl'.");
        } else {
          logger.logException(Level.SEVERE, e, null);
        }

      } catch (InterruptedException e) {
        // CPAchecker must exit because it was asked to
        // we return normally instead of propagating the exception
        // so we can return the partial result we have so far

      } catch (CPAException e) {
        logger.logException(Level.SEVERE, e, null);
      }
    }
    return new CPAcheckerResult(result, reached, stats);
  }

  private Result runAlgorithm(final Algorithm algorithm,
      final ReachedSet reached,
      final MainCPAStatistics stats) throws CPAException, InterruptedException {

    logger.log(Level.INFO, "Starting analysis ...");
    stats.analysisTime.start();

    boolean sound = true;
    do {
      sound &= algorithm.run(reached);

      // either run only once (if stopAfterError == true)
      // or until the waitlist is empty
    } while (!options.stopAfterError && reached.hasWaitingElement());

    logger.log(Level.INFO, "Stopping analysis ...");
    stats.analysisTime.stop();
    stats.programTime.stop();

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

  private ConfigurableProgramAnalysis createCPA(MainCPAStatistics stats) throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating CPAs");

    CPABuilder builder = new CPABuilder(config, logger, reachedSetFactory);
    ConfigurableProgramAnalysis cpa = builder.buildCPAs();

    if (cpa instanceof StatisticsProvider) {
      ((StatisticsProvider)cpa).collectStatistics(stats.getSubStatistics());
    }
    return cpa;
  }

  private ConfigurableProgramAnalysis createCPA(ReachedSetFactory pReachedSetFactory, Configuration pConfig, MainCPAStatistics stats) throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating CPAs");

    CPABuilder builder = new CPABuilder(pConfig, logger, pReachedSetFactory);
    ConfigurableProgramAnalysis cpa = builder.buildCPAs();

    if (cpa instanceof StatisticsProvider) {
      ((StatisticsProvider)cpa).collectStatistics(stats.getSubStatistics());
    }
    return cpa;
  }

  private Algorithm createAlgorithm(
      final ConfigurableProgramAnalysis cpa, final MainCPAStatistics stats)
  throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating algorithms");

    Algorithm algorithm = new CPAAlgorithm(cpa, logger);

    if (options.useRefinement) {
      algorithm = new CEGARAlgorithm(algorithm, config, logger);
    }

    if (options.useBMC) {
      algorithm = new BMCAlgorithm(algorithm, config, logger, reachedSetFactory);
    }

    if (options.useCBMC) {
      algorithm = new CounterexampleCheckAlgorithm(algorithm, config, logger);
    }

    if (options.useAssumptionCollector) {
      algorithm = new AssumptionCollectorAlgorithm(algorithm, config, logger);
    }

    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(stats.getSubStatistics());
    }
    return algorithm;
  }

  private Algorithm createAlgorithm(
      final ConfigurableProgramAnalysis cpa, Configuration pConfig,
      final MainCPAStatistics stats, ReachedSetFactory singleReachedSetFactory)
  throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating algorithms");

    Algorithm algorithm = new CPAAlgorithm(cpa, logger);

    if (options.useRefinement) {
      algorithm = new CEGARAlgorithm(algorithm, pConfig, logger);
    }

    if (options.useBMC) {
      algorithm = new BMCAlgorithm(algorithm, pConfig, logger, singleReachedSetFactory);
    }

    if (options.useCBMC) {
      algorithm = new CounterexampleCheckAlgorithm(algorithm, pConfig, logger);
    }

    if (options.useAssumptionCollector) {
      algorithm = new AssumptionCollectorAlgorithm(algorithm, pConfig, logger);
    }

    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(stats.getSubStatistics());
    }
    return algorithm;
  }

  private Pair<Algorithm, ReachedSet> createRestartAlgorithm(Configuration config, MainCPAStatistics stats, CFACreator cfaCreator) {
    List<Algorithm> algorithmsList = new ArrayList<Algorithm>();

    String[] configFiles = config.getPropertiesArray("restartAlgorithm.configFiles");

    ReachedSet reached = null;

    for(String configFileName: configFiles){
      Algorithm algorithm = null;
      Configuration.Builder singleConfigBuilder = Configuration.builder();
      Preconditions.checkNotNull(configFileName);
      try {
        singleConfigBuilder.loadFromFile(configFileName);
        Configuration singleConfig = singleConfigBuilder.build();
        singleConfig.inject(options);
        ReachedSetFactory singleReachedSetFactory = new ReachedSetFactory(singleConfig);
        ConfigurableProgramAnalysis cpa = createCPA(singleReachedSetFactory, singleConfig, stats);
        algorithm = createAlgorithm(cpa, singleConfig, stats, singleReachedSetFactory);

        reached = createInitialReachedSetForRestart(cpa, cfaCreator.getMainFunction(), singleReachedSetFactory);

        stopIfNecessary();

      } catch (IOException e) {
        e.printStackTrace();
      } catch (InvalidConfigurationException e) {
        e.printStackTrace();
      } catch (CPAException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      Preconditions.checkNotNull(algorithm);
      algorithmsList.add(algorithm);
    }

    Algorithm restartAlgorithm = null;

    try {
      restartAlgorithm = new RestartAlgorithm(algorithmsList, config, logger);
    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    } catch (CPAException e) {
      e.printStackTrace();
    }

    Preconditions.checkNotNull(restartAlgorithm);
    return Pair.of(restartAlgorithm, reached);
  }

  private ReachedSet createInitialReachedSetForRestart(
      ConfigurableProgramAnalysis cpa,
      CFAFunctionDefinitionNode mainFunction,
      ReachedSetFactory pReachedSetFactory) {
    logger.log(Level.FINE, "Creating initial reached set");

    AbstractElement initialElement = cpa.getInitialElement(mainFunction);
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction);

    ReachedSet reached = pReachedSetFactory.create();
    reached.add(initialElement, initialPrecision);
    return reached;
  }

  private ReachedSet createInitialReachedSet(
      final ConfigurableProgramAnalysis cpa,
      final CFAFunctionDefinitionNode mainFunction) {
    logger.log(Level.FINE, "Creating initial reached set");

    AbstractElement initialElement = cpa.getInitialElement(mainFunction);
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction);

    ReachedSet reached = reachedSetFactory.create();
    reached.add(initialElement, initialPrecision);
    return reached;
  }
}
