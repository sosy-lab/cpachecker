// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;

@Options(prefix = "boundedRangeExecution")
public class BoundedRangeExecution extends NestingAlgorithm {

  @Option(
      secure = true,
      required = true,
      description = "Which analysis to use for the analysis on the lower half")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path configLowerBound;

  @Option(
      secure = true,
      required = true,
      description = "Which analysis to use for the analysis on the upper half")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path configUpperBound;

  @Option(
      secure = true,
      required = true,
      description = "How to determine the bound splitting lower and upper half")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path configForTestcase;

  private boolean useSpecificLoopbound = false;
  private CFA cfa;

  protected BoundedRangeExecution(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(config, pLogger, pShutdownNotifier, pSpecification);
    config.inject(this);
    this.cfa = pCfa;
  }

  public static Algorithm create(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    return new BoundedRangeExecution(pConfig, pLogger, pShutdownNotifier, pSpecification, pCfa);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {

    // First, determine the bound
    int loopBound = 3;
    Optional<Path> path2LoopBound = computeAndLoadBound(loopBound, reachedSet);

    if (path2LoopBound.isPresent() && path2LoopBound.orElseThrow().toFile().exists()) {

      List<Pair<String, String>> paramsLower =
          Lists.newArrayList(
              Pair.of(
                  "cpa.rangeExecution.path2UpperInputFile",
                  path2LoopBound.get().toAbsolutePath().toString()));
      List<Pair<String, String>> paramsUpper =
          Lists.newArrayList(
              Pair.of(
                  "cpa.rangeExecution.path2LowerInputFile",
                  path2LoopBound.get().toAbsolutePath().toString()));

      Pair<AlgorithmStatus, Optional<Boolean>> res =
          loadAndExecuteAnalysis(this.configLowerBound, reachedSet, paramsLower);
      if (res.getSecond().isPresent() && !res.getSecond().get()) {
        // First Analysis found an error, hence abort
        return res.getFirst();
      }
      res = loadAndExecuteAnalysis(this.configUpperBound, reachedSet, paramsUpper);
      return res.getFirst();
    } else {
      // Special case: No input generated!
      logger.logf(
          Level.WARNING,
          "\n\n"
              + " +++WARNING+++\n"
              + "Could not generate a test-input for %s, hence only running %s\n"
              + " ++++++\n\n",
          cfa.getFileNames().get(0),
          this.configLowerBound);
      Pair<AlgorithmStatus, Optional<Boolean>> res =
          loadAndExecuteAnalysis(this.configLowerBound, reachedSet, new ArrayList<>());
      return res.getFirst();
    }
  }

  /**
   * Generate a loopbound for the given int pLoopBound and returns it. If no loopbound is returned,
   * an empyt Optional is returned
   *
   * @param pLoopBound the value for the loopbound
   * @param pReached the current reached set
   * @return either a path to the loopbound (if generated), otherwisse an empty optional
   */
  private Optional<Path> computeAndLoadBound(int pLoopBound, ReachedSet pReached) {

    Iterable<CFANode> initialNodes = AbstractStates.extractLocations(pReached.getFirstState());
    CFANode mainFunction = Iterables.getOnlyElement(initialNodes);
    ShutdownManager singleShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
    try {
      logger.log(Level.INFO, "Generating bound");

      @Nullable Algorithm currentAlgorithm;
      @Nullable ReachedSet currentReached;
      try {
        Collection<Pair<String, String>> additionalParams;
        if (useSpecificLoopbound) {
          additionalParams =
              Lists.newArrayList(
                  Pair.of(
                      "cpa.aggressiveloopbound.maxLoopIterations", Integer.toString(pLoopBound)));
        } else {
          additionalParams = new ArrayList<>();
        }
        Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> currentAlg =
            createNextAlgorithm(
                this.configForTestcase,
                mainFunction,
                singleShutdownManager,
                false,
                null,
                additionalParams);
        currentAlgorithm = currentAlg.getFirst();
        currentReached = currentAlg.getThird();

      } catch (InvalidConfigurationException e) {
        logger.logUserException(
            Level.WARNING,
            e,
            "Skipping one analysis because the configuration file "
                + this.configForTestcase
                + " is invalid");
        return Optional.empty();
      } catch (IOException e) {
        String message =
            "Skipping one analysis because the configuration file "
                + configForTestcase.toString()
                + " could not be read";
        if (shutdownNotifier.shouldShutdown() && e instanceof ClosedByInterruptException) {
          logger.log(Level.WARNING, message);
        } else {
          logger.logUserException(Level.WARNING, e, message);
        }
        return Optional.empty();
      } catch (CPAException | InterruptedException pE) {
        logger.logUserException(
            Level.WARNING,
            pE,
            "Failed to generate a loop bound during execution of the analysis "
                + this.configForTestcase);
        return Optional.empty();
      }

      // run algorith
      try {
        currentAlgorithm.run(currentReached);
        Path path2File = Paths.get("output/testinput.xml");
        if (path2File.toFile().exists()) {return Optional.of(path2File);}
        return Optional.empty();
      } catch (CPAException | InterruptedException pE) {
        logger.logUserException(
            Level.WARNING,
            pE,
            "An error occured during execution of ranged analysis:" + this.configForTestcase);
        return Optional.empty();
      }

    } catch (Exception pE) {
      logger.logUserException(
          Level.WARNING, pE, "An unexpected error occured during the execution");
      return Optional.empty();
    }
  }

  /**
   * @param path2AnalysisConfig the path to the analysis config file to load from
   * @param pReached the reached set
   * @param pAdditionalParams Additional Params to use in the config, as set of Pairs, where the
   *     first String corresponds to the name of the configuration parameter and the second string
   *     to the value. (e.g. Pair("cpa.rangeExecution.path2UpperInputFile", "output.testcase.xml").
   *     Note that no "=" is needed.
   * @return The status of the analysis and, if the analysis succeeded without error (isPrecise &
   *     isSound), a boolean, that is true if the reached set computed does **not** contain an error
   * @throws InterruptedException docu
   */
  private Pair<AlgorithmStatus, Optional<Boolean>> loadAndExecuteAnalysis(
      Path path2AnalysisConfig,
      ReachedSet pReached,
      List<Pair<String, String>> pAdditionalParams)
      throws InterruptedException {

    ForwardingReachedSet reached = (ForwardingReachedSet) pReached;
    AlgorithmStatus status = AlgorithmStatus.NO_PROPERTY_CHECKED;
    Iterable<CFANode> initialNodes = AbstractStates.extractLocations(pReached.getFirstState());
    CFANode mainFunction = Iterables.getOnlyElement(initialNodes);
    ShutdownManager singleShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
    try {
      logger.logf(Level.INFO, "Loading analysis from file %s ...", path2AnalysisConfig);

      @Nullable Algorithm currentAlgorithm;
      @Nullable ReachedSet currentReached;
      try {
        Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> currentAlg =
            createNextAlgorithm(
                path2AnalysisConfig,
                mainFunction,
                singleShutdownManager,
                false,
                null,
                pAdditionalParams);
        currentAlgorithm = currentAlg.getFirst();
        currentReached = currentAlg.getThird();

      } catch (InvalidConfigurationException e) {
        logger.logUserException(
            Level.WARNING,
            e,
            "Skipping one analysis because the configuration file "
                + path2AnalysisConfig
                + " is invalid");
        return Pair.of(AlgorithmStatus.NO_PROPERTY_CHECKED, Optional.empty());
      } catch (IOException e) {
        String message =
            "Skipping one analysis because the configuration file "
                + path2AnalysisConfig
                + " could not be read";
        if (shutdownNotifier.shouldShutdown() && e instanceof ClosedByInterruptException) {
          logger.log(Level.WARNING, message);
        } else {
          logger.logUserException(Level.WARNING, e, message);
        }
        return Pair.of(AlgorithmStatus.NO_PROPERTY_CHECKED, Optional.empty());
      } catch (CPAException | InterruptedException pE) {
        logger.logUserException(Level.WARNING, pE,
            "An error occured during execution of the analysis " + path2AnalysisConfig);
        return Pair.of(AlgorithmStatus.NO_PROPERTY_CHECKED, Optional.empty());
      }

      reached.setDelegate(currentReached);

      shutdownNotifier.shutdownIfNecessary();

      // run algorith

      try {
        logger.logf(Level.INFO, "Starting analysis %s ...", path2AnalysisConfig);
        status = currentAlgorithm.run(currentReached);

        if (currentReached.stream().anyMatch(s -> AbstractStates.isTargetState(s))
            && status.isPrecise()) {
          // If the algorithm is not _precise_, verdict "false" actually means "unknown".
          return Pair.of(status, Optional.of(false));
        }

        if (!status.isSound()) {
          // if the analysis is not sound and we can proceed with
          // another algorithm, continue with the next algorithm
          logger.logf(
              Level.INFO, "Analysis %s terminated, but result is unsound.", path2AnalysisConfig);

        } else if (currentReached.hasWaitingState()) {
          // if there are still states in the waitlist, the result is unknown
          // continue with the next algorithm
          logger.logf(
              Level.INFO,
              "Analysis %s terminated but did not finish: There are still states to be processed.",
              path2AnalysisConfig);
        }
      } catch (CPAException pE) {
        logger.logUserException(Level.INFO,pE, "An error occured during execution of the analysis " + path2AnalysisConfig);
      }
    } finally {
      singleShutdownManager.requestShutdown(
          "Analysis terminated"); // shutdown any remaining components
    }
    boolean errorFree = reached.stream().anyMatch(a -> AbstractStates.isTargetState(a));

    return Pair.of(status, Optional.of(errorFree));
  }

  protected Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> createNextAlgorithm(
      Path singleConfigFileName,
      CFANode mainFunction,
      ShutdownManager singleShutdownManager,
      boolean pProvideReachedForNextAlgorithm,
      ReachedSet pCurrentReached,
      Collection<Pair<String, String>> pAdditionalParams)
      throws InvalidConfigurationException, CPAException, IOException, InterruptedException {

    AggregatedReachedSets aggregateReached;
    if (pProvideReachedForNextAlgorithm) {
      aggregateReached = AggregatedReachedSets.singleton(pCurrentReached);
    } else {
      aggregateReached = AggregatedReachedSets.empty();
    }

    return super.createAlgorithm(
        singleConfigFileName,
        mainFunction,
        cfa,
        singleShutdownManager,
        aggregateReached,
        Sets.newHashSet(
            "restartAlgorithm.configFiles", "analysis.useCombinedRangeExecutionAlgorithm"),
        pAdditionalParams,
        new ArrayList<>());
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {}
}
