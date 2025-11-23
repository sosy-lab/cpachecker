// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.CfaTransformationMetadata;
import org.sosy_lab.cpachecker.cfa.CfaTransformationMetadata.ProgramTransformation;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/**
 * The Modular Partial Order Reduction (MPOR) algorithm produces a sequentialization of a concurrent
 * C program. The algorithm contains options that allow both static and dynamic reductions in the
 * state space. Sequentializations can be given to any verifier capable of verifying sequential C
 * programs, hence modular.
 */
public class MporPreprocessingAlgorithm implements Algorithm, StatisticsProvider {

  private final MPOROptions options;

  private final LogManager logger;

  private final ShutdownNotifier shutdownNotifier;

  private final Configuration config;
  private final Specification specification;

  private final CFA cfa;

  private final SequentializationStatistics sequentializationStatistics;

  public MporPreprocessingAlgorithm(
      Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      CFA pInputCfa,
      Specification pSpecification)
      throws InvalidConfigurationException {

    // the options are not null when unit testing
    options = new MPOROptions(pConfiguration);
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    cfa = pInputCfa;
    config = pConfiguration;
    specification = pSpecification;
    sequentializationStatistics = new SequentializationStatistics();
  }

  public static boolean alreadySequentialized(CFA pCFA) {
    CfaTransformationMetadata transformationMetadata =
        pCFA.getMetadata().getTransformationMetadata();
    if (transformationMetadata == null) {
      return false;
    }

    ProgramTransformation transformation = transformationMetadata.transformation();
    return transformation.equals(ProgramTransformation.SEQUENTIALIZATION)
        || transformation.equals(ProgramTransformation.SEQUENTIALIZATION_FAILED);
  }

  private CFA preprocessCfaUsingSequentialization(CFA pOldCFA)
      throws UnrecognizedCodeException,
          InterruptedException,
          ParserException,
          InvalidConfigurationException {

    logger.log(Level.INFO, "Starting sequentialization of the program.");
    sequentializationStatistics.startSequentializationTimer();

    SequentializationUtils utils = SequentializationUtils.of(cfa, config, logger, shutdownNotifier);
    String sequentializedCode = Sequentialization.tryBuildProgramString(options, cfa, utils);
    // disable preprocessing in the updated config, since input cfa was preprocessed already
    Configuration configWithoutPreprocessor =
        Configuration.builder()
            .copyFrom(config)
            .setOption("parser.usePreprocessor", "false")
            .build();
    CFACreator cfaCreator = new CFACreator(configWithoutPreprocessor, logger, shutdownNotifier);
    ImmutableCFA newCFA = cfaCreator.parseSourceAndCreateCFA(sequentializedCode);

    newCFA =
        newCFA.copyWithMetadata(
            getNewMetadata(pOldCFA, newCFA, ProgramTransformation.SEQUENTIALIZATION));

    sequentializationStatistics.stopSequentializationTimer();
    logger.log(Level.INFO, "Finished sequentialization of the program.");

    return newCFA;
  }

  private CfaMetadata getNewMetadata(
      CFA pOldCFA, CFA pNewCfa, ProgramTransformation pSequentializationStatus) {

    return pOldCFA
        .getMetadata()
        .withTransformationMetadata(new CfaTransformationMetadata(cfa, pSequentializationStatus))
        // update main() FunctionEntryNode, since cfa.getMainFunction() returns it from metadata
        .withMainFunctionEntry(pNewCfa.getFunctionHead("main"));
  }

  @CanIgnoreReturnValue
  @Override
  public AlgorithmStatus run(@NonNull ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    // Only sequentialize if not already done and requested.
    // We replace the CFA for its sequentialized version.
    CFA newCfa = cfa;
    if (alreadySequentialized(cfa)) {
      logger.log(
          Level.INFO,
          "The CFA is already sequentialized. "
              + "The sequentialization will be ignored. "
              + "If this is part of a parallel algorithm, this may be expected.");
    } else {
      try {
        newCfa = preprocessCfaUsingSequentialization(cfa);
      } catch (UnrecognizedCodeException
          | ParserException
          | InvalidConfigurationException
          | IllegalArgumentException e) {
        logger.logUserException(
            Level.WARNING,
            e,
            "Sequentialization of the input program failed, falling back to using the original"
                + " program.");
        CfaMetadata newMetadata =
            getNewMetadata(cfa, newCfa, ProgramTransformation.SEQUENTIALIZATION_FAILED);
        // Mark the CFA as having failed sequentialization
        // TODO: Simplify with sealed classes
        if (cfa instanceof ImmutableCFA immutableCfa) {
          newCfa = immutableCfa.copyWithMetadata(newMetadata);
        } else {
          throw new AssertionError("Expected ImmutableCFA here.");
        }
      }
    }

    final CoreComponentsFactory coreComponents;
    final ConfigurableProgramAnalysis cpa;
    Algorithm innerAlgorithm;

    try {
      coreComponents =
          new CoreComponentsFactory(
              config, logger, shutdownNotifier, AggregatedReachedSets.empty(), newCfa);
      cpa = coreComponents.createCPA(specification);
      innerAlgorithm = coreComponents.createAlgorithm(cpa, specification);
    } catch (InvalidConfigurationException e) {
      throw new CPAException(
          "Building the algorithm which should be run on the sequentialized program failed", e);
    }

    // Prepare new reached set
    pReachedSet.clear();
    coreComponents.initializeReachedSet(pReachedSet, newCfa.getMainFunction(), cpa);

    if (innerAlgorithm instanceof StatisticsProvider statisticsProvider) {
      sequentializationStatistics.addInnerStatisticsProvider(statisticsProvider);
    }

    return innerAlgorithm.run(pReachedSet);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(sequentializationStatistics);
  }

  private static class SequentializationStatistics implements Statistics {

    private final ImmutableList.Builder<StatisticsProvider> innerStatisticsProviders =
        ImmutableList.builder();

    private final StatTimer sequentializationTime = new StatTimer("Sequentialization Time");

    private void addInnerStatisticsProvider(StatisticsProvider provider) {
      innerStatisticsProviders.add(provider);
    }

    private void startSequentializationTimer() {
      sequentializationTime.start();
    }

    private void stopSequentializationTimer() {
      sequentializationTime.stop();
    }

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      StatisticsWriter w0 = writingStatisticsTo(out);
      w0.put(sequentializationTime);
      List<Statistics> stats = new ArrayList<>();
      for (StatisticsProvider statisticsProvider : innerStatisticsProviders.build()) {
        statisticsProvider.collectStatistics(stats);
      }

      for (Statistics stat : stats) {
        stat.printStatistics(out, result, reached);
      }
    }

    @Override
    public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
      List<Statistics> stats = new ArrayList<>();
      for (StatisticsProvider statisticsProvider : innerStatisticsProviders.build()) {
        statisticsProvider.collectStatistics(stats);
      }

      for (Statistics stat : stats) {
        stat.writeOutputFiles(pResult, pReached);
      }
    }

    @Override
    public String getName() {
      return "Sequentialization Preprocessing Statistics";
    }
  }
}
