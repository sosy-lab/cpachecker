/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Strings;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.argReplay.ARGReplayCPA;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

@Options(prefix="restartAlgorithmWithARGReplay")
public class RestartAlgorithmWithARGReplay implements Algorithm, StatisticsProvider {

  private static class RestartAlgorithmStatistics implements Statistics {

    private final int noOfAlgorithms;
    private final Collection<Statistics> subStats;
    private int noOfAlgorithmsUsed = 0;
    private Timer totalTime = new Timer();

    public RestartAlgorithmStatistics(int pNoOfAlgorithms) {
      noOfAlgorithms = pNoOfAlgorithms;
      subStats = new ArrayList<>();
    }

    public Collection<Statistics> getSubStatistics() {
      return subStats;
    }

    public void resetSubStatistics() {
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

  @Option(secure=true, required=true, description = "List of files with configurations to use. 2 filenames expected.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> configFiles;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final RestartAlgorithmStatistics stats;
  private final CFA cfa;
  private final Configuration globalConfig;
  private final Specification specification;

  public RestartAlgorithmWithARGReplay(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Specification pSpecification)
      throws InvalidConfigurationException {
    config.inject(this);

    if (configFiles.size() != 2) {
      throw new InvalidConfigurationException("Need at least one configuration for restart algorithm!");
    }

    this.stats = new RestartAlgorithmStatistics(configFiles.size());
    this.logger = pLogger;
    this.shutdownNotifier = pShutdownNotifier;
    this.cfa = pCfa;
    this.globalConfig = config;
    specification = checkNotNull(pSpecification);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReached) throws CPAException, InterruptedException {
    checkArgument(pReached instanceof ForwardingReachedSet, "RestartAlgorithm needs ForwardingReachedSet");
    ForwardingReachedSet reached = (ForwardingReachedSet) pReached;

    CFANode mainFunction = AbstractStates.extractLocation(pReached.getFirstState());
    assert mainFunction != null : "Location information needed";

    AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_PRECISE;

    try {
      ReachedSetFactory reachedSetFactory = new ReachedSetFactory(globalConfig);

      // predicate analysis
      logger.log(Level.FINE, "Creating CPA for PredicateAnalysis");
      Configuration singleConfig1 = getConfigFromFile(configFiles.get(0));
      ConfigurableProgramAnalysis cpa1 = getCPA(reachedSetFactory, singleConfig1);
      Algorithm algorithm1 = getAlgorithm(shutdownNotifier, singleConfig1, logger, cpa1);
      ReachedSet reached1 = createInitialReachedSetForRestart(cpa1, mainFunction, singleConfig1, logger);

      reached.setDelegate(reached1);

      stats.noOfAlgorithmsUsed++;
      stats.totalTime.start();

      status = run0(reached1, algorithm1);

      //stats.printIntermediateStatistics(System.out, Result.UNKNOWN, reached); // disabled, because table-generator can not distinguish 1st and 2nd statistics.
      stats.resetSubStatistics();

      // predicate bit-precise analysis
      logger.log(Level.FINE, "Creating CPA for PredicateAnalysis-Bitprecise");
      Configuration singleConfig2 = getConfigFromFile(configFiles.get(1));
      ConfigurableProgramAnalysis cpa2 = getCPA(reachedSetFactory, singleConfig2);

      {
        // this is the important step: re-use the reached-set
        ARGReplayCPA argReplay = CPAs.retrieveCPA(cpa2, ARGReplayCPA.class);
        checkNotNull(argReplay, "ARGReplay-CPA is needed for second analysis");
        argReplay.setARGAndCPA(reached1, cpa1);
      }

      Algorithm algorithm2 = getAlgorithm(shutdownNotifier, singleConfig2, logger, cpa2);
      ReachedSet reached2 = createInitialReachedSetForRestart(cpa2, mainFunction, singleConfig2, logger);


      reached.setDelegate(reached2);

      stats.noOfAlgorithmsUsed++;
      stats.totalTime.start();

      status = run0(reached2, algorithm2);

      stats.printIntermediateStatistics(System.out, Result.UNKNOWN, reached);
      stats.resetSubStatistics();

    } catch (InvalidConfigurationException e) {
      logger.logUserException(Level.WARNING, e, "Exiting analysis because the configuration file is invalid");
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Exiting analysis because the configuration file could not be read");
    } finally {
      // TODO close CPAs and algorithms
    }

    return status;
  }


  private Configuration getConfigFromFile(Path path) throws IOException, InvalidConfigurationException {
    ConfigurationBuilder singleConfigBuilder = Configuration.builder();
    singleConfigBuilder.copyFrom(globalConfig);
    singleConfigBuilder.clearOption("restartAlgorithm.configFiles");
    singleConfigBuilder.loadFromFile(path);
    Configuration singleConfig = singleConfigBuilder.build();
    return singleConfig;
  }

  private ConfigurableProgramAnalysis getCPA(ReachedSetFactory reachedSetFactory, Configuration singleConfig1)
      throws InvalidConfigurationException, CPAException {
    CPABuilder builder1 = new CPABuilder(singleConfig1, logger, shutdownNotifier, reachedSetFactory);
    ConfigurableProgramAnalysis cpa1 =
        builder1.buildCPAs(cfa, specification, new AggregatedReachedSets());
    if (cpa1 instanceof StatisticsProvider) {
      ((StatisticsProvider)cpa1).collectStatistics(stats.getSubStatistics());
    }
    return cpa1;
  }

  private Algorithm getAlgorithm(ShutdownNotifier singleShutdownNotifier, Configuration singleConfig,
      LogManager singleLogger, ConfigurableProgramAnalysis cpa) throws InvalidConfigurationException, CPAException {
    singleLogger.log(Level.FINE, "Creating algorithms");
    Algorithm algorithm = CPAAlgorithm.create(cpa, singleLogger, singleConfig, singleShutdownNotifier);

    CEGARAlgorithm cegarAlgorithm = new CEGARAlgorithm(algorithm, cpa, singleConfig, singleLogger);
    cegarAlgorithm.collectStatistics(stats.getSubStatistics());

    return cegarAlgorithm;
  }

  private ReachedSet createInitialReachedSetForRestart(
      ConfigurableProgramAnalysis cpa,
      CFANode mainFunction,
      Configuration singleConfig,
      LogManager singleLogger) throws InvalidConfigurationException, InterruptedException {
    singleLogger.log(Level.FINE, "Creating initial reached set");

    ReachedSetFactory reachedSetFactory = new ReachedSetFactory(singleConfig);
    AbstractState initialState = cpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition());
    Precision initialPrecision = cpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition());

    ReachedSet reached = reachedSetFactory.create();
    reached.add(initialState, initialPrecision);
    return reached;
  }

  private AlgorithmStatus run0(ReachedSet reached, Algorithm algorithm)
      throws InterruptedException, CPAException, CPAEnabledAnalysisPropertyViolationException {
    logger.log(Level.INFO, "Starting sub-analysis");
    shutdownNotifier.shutdownIfNecessary();
    AlgorithmStatus status = algorithm.run(reached);
    shutdownNotifier.shutdownIfNecessary();
    logger.log(Level.INFO, "Finished sub-analysis");
    return status;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
