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
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractElements;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

@Options(prefix="restartAlgorithm")
public class RestartAlgorithm implements Algorithm, StatisticsProvider {

  private static class RestartAlgorithmStatistics implements Statistics {

    private final int noOfAlgorithms;
    private final Collection<Statistics> subStats;
    private int noOfAlgorithmsUsed = 0;
    private Timer totalTime = new Timer();

    public RestartAlgorithmStatistics(int pNoOfAlgorithms) {
      noOfAlgorithms = pNoOfAlgorithms;
      subStats = new ArrayList<Statistics>();
    }

    public Collection<Statistics> getSubStatistics() {
      return subStats;
    }

    public void resetSubStatistics(){
      subStats.clear();
      totalTime = new Timer();
    }

    @Override
    public String getName() {
      return "Restart Algorithm";
    }

    private void printIntermediateStatistics(PrintStream out, Result result,
        ReachedSet reached) {

      String text = "Statistics for algorithm " + noOfAlgorithmsUsed + " of " + noOfAlgorithms;
      out.println(text);
      out.println(Strings.repeat("=", text.length()));

      printSubStatistics(out, result, reached);
      out.println();
    }

    @Override
    public void printStatistics(PrintStream out, Result result,
        ReachedSet reached) {

      out.println("Number of algorithms provided:    " + noOfAlgorithms);
      out.println("Number of algorithms used:        " + noOfAlgorithmsUsed);

      printSubStatistics(out, result, reached);
    }

    private void printSubStatistics(PrintStream out, Result result, ReachedSet reached) {
      out.println("Total time for algorithm " + noOfAlgorithmsUsed + ": " + totalTime);

      for (Statistics s : subStats) {
        String name = s.getName();
        if (!isNullOrEmpty(name)) {
          name = name + " statistics";
          out.println("");
          out.println(name);
          out.println(Strings.repeat("-", name.length()));
        }
        s.printStatistics(out, result, reached);
      }
    }

  }

  @Option(description = "list of files with configurations to use")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private List<File> configFiles;

  private final LogManager logger;
  private final RestartAlgorithmStatistics stats;
  private final String filename;
  private final CFA cfa;
  private final Configuration globalConfig;

  private Algorithm currentAlgorithm;

  public RestartAlgorithm(Configuration config, LogManager pLogger, String pFilename, CFA pCfa) throws InvalidConfigurationException {
    config.inject(this);

    if (configFiles.isEmpty()) {
      throw new InvalidConfigurationException("Need at least one configuration for restart algorithm!");
    }

    this.stats = new RestartAlgorithmStatistics(configFiles.size());
    this.logger = pLogger;
    this.filename = pFilename;
    this.cfa = pCfa;
    this.globalConfig = config;
  }

  @Override
  public boolean run(ReachedSet pReached) throws CPAException, InterruptedException {
    checkArgument(pReached instanceof ForwardingReachedSet, "RestartAlgorithm needs ForwardingReachedSet");
    checkArgument(pReached.size() <= 1, "RestartAlgorithm does not support being called several times with the same reached set");
    checkArgument(!pReached.isEmpty(), "RestartAlgorithm needs non-empty reached set");

    ForwardingReachedSet reached = (ForwardingReachedSet)pReached;

    CFANode mainFunction = AbstractElements.extractLocation(pReached.getFirstElement());
    assert mainFunction != null : "Location information needed";

    Iterator<File> configFilesIterator = configFiles.iterator();

    while (configFilesIterator.hasNext()) {
      stats.totalTime.start();
      ReachedSet currentReached;
      try {
        File singleConfigFileName = configFilesIterator.next();

        try {
          Pair<Algorithm, ReachedSet> currentPair = createNextAlgorithm(singleConfigFileName, mainFunction);
          currentAlgorithm = currentPair.getFirst();
          currentReached = currentPair.getSecond();
        } catch (InvalidConfigurationException e) {
          logger.logUserException(Level.WARNING, e, "Skipping one analysis because its configuration is invalid");
          continue;
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Skipping one analysis due to unreadable configuration file");
          continue;
        }

        reached.setDelegate(currentReached);

        if (currentAlgorithm instanceof StatisticsProvider) {
          ((StatisticsProvider)currentAlgorithm).collectStatistics(stats.getSubStatistics());
        }

        stats.noOfAlgorithmsUsed++;

        // run algorithm
        try {
          boolean sound = currentAlgorithm.run(currentReached);

          if (Iterables.any(currentReached, AbstractElements.IS_TARGET_ELEMENT)) {
            return sound;
          }

          if (!sound) {
            // if the analysis is not sound and we can proceed with
            // another algorithm, continue with the next algorithm
            logger.log(Level.INFO, "Analysis result was unsound.");

          } else if (currentReached.hasWaitingElement()) {
            // if there are still elements in the waitlist, the result is unknown
            // continue with the next algorithm
            logger.log(Level.INFO, "Analysis not completed: There are still elements to be processed.");

          } else {
            // sound analysis and completely finished, terminate
            return true;
          }
        } catch (CPAException e) {
          if (configFilesIterator.hasNext()) {
            logger.logUserException(Level.WARNING, e, "Analysis not completed");
          } else {
            throw e;
          }
        }
      } finally {
        stats.totalTime.stop();
      }

      if (configFilesIterator.hasNext()) {
        stats.printIntermediateStatistics(System.out, Result.UNKNOWN, currentReached);
        stats.resetSubStatistics();
        logger.log(Level.INFO, "RestartAlgorithm switches to the next configuration...");
      }
    }

    // no further configuration available, and analysis has not finished
    logger.log(Level.INFO, "No further configuration available.");
    return false;
  }

  @Options
  private static class RestartAlgorithmOptions {

    @Option(name="analysis.useAssumptionCollector",
        description="use assumption collecting algorithm")
        boolean useAssumptionCollector = false;

    @Option(name = "analysis.useRefinement",
        description = "use CEGAR algorithm for lazy counter-example guided analysis"
          + "\nYou need to specify a refiner with the cegar.refiner option."
          + "\nCurrently all refiner require the use of the ARGCPA.")
          boolean useRefinement = false;

    @Option(name="analysis.useCBMC",
        description="use CBMC to double-check counter-examples")
        boolean useCBMC = false;

    @Option(name="analysis.useBMC",
        description="use a BMC like algorithm that checks for satisfiability "
          + "after the analysis has finished, works only with PredicateCPA")
          boolean useBMC = false;

    @Option(name="analysis.externalCBMC",
        description="use CBMC as an external tool from CPAchecker")
        boolean runCBMCasExternalTool = false;

  }

  private Pair<Algorithm, ReachedSet> createNextAlgorithm(File singleConfigFileName, CFANode mainFunction) throws InvalidConfigurationException, CPAException, InterruptedException, IOException {

    ReachedSet reached;
    Algorithm algorithm;

    Configuration.Builder singleConfigBuilder = Configuration.builder();
    singleConfigBuilder.copyFrom(globalConfig);
    singleConfigBuilder.clearOption("restartAlgorithm.configFiles");
    singleConfigBuilder.clearOption("analysis.restartAfterUnknown");

    RestartAlgorithmOptions singleOptions = new RestartAlgorithmOptions();
    singleConfigBuilder.loadFromFile(singleConfigFileName);
    Configuration singleConfig = singleConfigBuilder.build();
    singleConfig.inject(singleOptions);

    if(singleOptions.runCBMCasExternalTool){
      algorithm = new ExternalCBMCAlgorithm(filename, singleConfig, logger);
      reached = new ReachedSetFactory(singleConfig, logger).create();
    }
    else{
      ReachedSetFactory singleReachedSetFactory = new ReachedSetFactory(singleConfig, logger);
      ConfigurableProgramAnalysis cpa = createCPA(singleReachedSetFactory, singleConfig, stats);
      algorithm = createAlgorithm(cpa, singleConfig, stats, singleReachedSetFactory, singleOptions);
      reached = createInitialReachedSetForRestart(cpa, mainFunction, singleReachedSetFactory);
    }

    CPAchecker.stopIfNecessary();

    return Pair.of(algorithm, reached);
  }

  private ReachedSet createInitialReachedSetForRestart(
      ConfigurableProgramAnalysis cpa,
      CFANode mainFunction,
      ReachedSetFactory pReachedSetFactory) {
    logger.log(Level.FINE, "Creating initial reached set");

    AbstractElement initialElement = cpa.getInitialElement(mainFunction);
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction);

    ReachedSet reached = pReachedSetFactory.create();
    reached.add(initialElement, initialPrecision);
    return reached;
  }

  private ConfigurableProgramAnalysis createCPA(ReachedSetFactory pReachedSetFactory, Configuration pConfig, RestartAlgorithmStatistics stats) throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating CPAs");

    CPABuilder builder = new CPABuilder(pConfig, logger, pReachedSetFactory);
    ConfigurableProgramAnalysis cpa = builder.buildCPAs(cfa);

    if (cpa instanceof StatisticsProvider) {
      ((StatisticsProvider)cpa).collectStatistics(stats.getSubStatistics());
    }
    return cpa;
  }

  private Algorithm createAlgorithm(
      final ConfigurableProgramAnalysis cpa, Configuration pConfig,
      final RestartAlgorithmStatistics stats, ReachedSetFactory singleReachedSetFactory,
      RestartAlgorithmOptions pOptions)
  throws InvalidConfigurationException, CPAException {
    logger.log(Level.FINE, "Creating algorithms");

    Algorithm algorithm = new CPAAlgorithm(cpa, logger, pConfig);

    if (pOptions.useRefinement) {
      algorithm = new CEGARAlgorithm(algorithm, cpa, pConfig, logger);
    }

    if (pOptions.useBMC) {
      algorithm = new BMCAlgorithm(algorithm, cpa, pConfig, logger, singleReachedSetFactory, cfa);
    }

    if (pOptions.useCBMC) {
      algorithm = new CounterexampleCheckAlgorithm(algorithm, cpa, pConfig, logger, singleReachedSetFactory, cfa);
    }

    if (pOptions.useAssumptionCollector) {
      algorithm = new AssumptionCollectorAlgorithm(algorithm, cpa, pConfig, logger);
    }

    return algorithm;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if(currentAlgorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)currentAlgorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }
}
