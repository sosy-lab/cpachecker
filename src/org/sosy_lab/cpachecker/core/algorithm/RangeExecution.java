//// This file is part of CPAchecker,
//// a tool for configurable software verification:
//// https://cpachecker.sosy-lab.org
////
//// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
////
//// SPDX-License-Identifier: Apache-2.0
//
package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;

@Options(prefix = "rangeExecution")
public class RangeExecution extends NestingAlgorithm {

  @Option(
      secure = true,
      required = true,
      description =
          "List of files with configurations to use. The files are used in order, hence the first file is used for the left most bound, ..., the last file is used for the right most bound.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> configFiles;

  @Option(
      secure = true,
      description =
          "List of values for the loopbound. The values are used in order, hence the first loopbound is used for the upper bound of the leftmost analysis and so on. Need to have the same size as configFiles.")
  private List<Integer> valuesForLoopBound = Lists.newArrayList(3);

  @Option(
      secure = true,
      required = true,
      description = "How to determine the bound splitting lower and upper half")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path configForTestcase;

  @Option(
      secure = true,
      name = "testcaseNames",
      description = "Names of the files for the testcases")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate testcaseNames = PathTemplate.ofFormatString("testcase.%d.xml");

  private CFA cfa;
  private static AggregatedReachedSets aggregatedReachedSets;

  protected RangeExecution(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException {
    super(config, pLogger, pShutdownNotifier, pSpecification);
    config.inject(this);
    this.cfa = pCfa;
    aggregatedReachedSets = pAggregatedReachedSets;
  }

  public static Algorithm create(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException {
    return new RangeExecution(
        pConfig, pLogger, pShutdownNotifier, pSpecification, pCfa, pAggregatedReachedSets);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReached) throws CPAException, InterruptedException {

    // First, determine the bound
    Iterable<CFANode> initialNodes = AbstractStates.extractLocations(pReached.getFirstState());
    CFANode mainFunction = Iterables.getOnlyElement(initialNodes);
    List<Path> path2LoopBound = computeAndLoadBound(mainFunction, pReached);

    if (path2LoopBound.size() == configFiles.size() - 1
        && path2LoopBound.stream().allMatch(f -> f.toFile().exists())) {

      try {
        ConfigurationBuilder configBuilder = Configuration.builder();
        configBuilder.copyFrom(super.globalConfig);
        configBuilder.setOption("parallelAlgorithm.configFiles", "cfg");
        ParallelAlgorithmForRangedExecution parallelAlgorithm =
            new ParallelAlgorithmForRangedExecution(
                configBuilder.build(),
                logger,
                shutdownNotifier,
                specification,
                cfa,
                aggregatedReachedSets);
        parallelAlgorithm.computeAnalyses(path2LoopBound, configFiles);
      return parallelAlgorithm.run(pReached);
      } catch (InvalidConfigurationException | IOException pE) {
        logger.logException(
            Level.SEVERE, pE, "Could not create parallel algorithm, hence aborting");
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      }
  }
  return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  /**
   * TODO: Update Generate loopbounds for the given int valuesForLoopBound and returns it. If no
   * loopbound is generated, an empty list is generated
   *
   * @param mainFunction THe cfa node of the main function entry
   * @return either a list of path for the loopbounds (if generated), otherwise an empty list
   */
  private List<Path> computeAndLoadBound(CFANode mainFunction, ReachedSet pReached) {

    ShutdownManager singleShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);

    List<Path> res = new ArrayList<>(valuesForLoopBound.size());
    // FIXME: Compute in parallel!
    logger.log(Level.INFO, "Generating bound");
    for (int i = 0; i < valuesForLoopBound.size(); i++) {

      @Nullable Algorithm currentAlgorithm;
      @Nullable ReachedSet currentReached;
      Path path2File = testcaseNames.getPath(i);
      try {
        Collection<Pair<String, String>> additionalParams;

        additionalParams =
            Lists.newArrayList(
                Pair.of(
                    "cpa.aggressiveloopbound.maxLoopIterations",
                    Integer.toString(valuesForLoopBound.get(i))),
                Pair.of(
                    "cpa.rangedExecutionInput.testcaseName",
                    path2File.toAbsolutePath().toString()));
        Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> currentAlg =
            createNextAlgorithm(
                this.configForTestcase,
                mainFunction,
                singleShutdownManager,
                false,
                pReached,
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
        return new ArrayList<>();
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
        return new ArrayList<>();
      } catch (CPAException | InterruptedException pE) {
        logger.logUserException(
            Level.WARNING,
            pE,
            String.format(
                "An error occured during execution of ranged analysis: %s at iteration %d",
                this.configForTestcase, i));
        return new ArrayList<>();
      }

      // run algorith
      try {
        currentAlgorithm.run(currentReached);
        if (path2File.toFile().exists()) {
          res.add(path2File);
        } else {
          logger.logf(Level.WARNING, "Could not generate a test-input for loopbound %d!", i);
          return new ArrayList<>();
        }
      } catch (CPAException | InterruptedException pE) {
        logger.logUserException(
            Level.WARNING,
            pE,
            String.format(
                "An error occured during execution of ranged analysis: %s at iteration %d",
                this.configForTestcase, i));
        return new ArrayList<>();
      }
    }
    return res;
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
