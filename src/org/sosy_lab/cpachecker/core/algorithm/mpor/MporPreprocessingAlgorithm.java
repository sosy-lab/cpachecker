// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
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
@Options(prefix = "sequentialization")
public class MporPreprocessingAlgorithm implements Algorithm, StatisticsProvider {

  private final MPOROptions options;

  private final LogManager logger;

  private final ShutdownNotifier shutdownNotifier;

  private final Configuration config;
  private final Specification specification;

  private final CFA cfa;

  private final SequentializationStatistics sequentializationStatistics;

  @Option(
      secure = true,
      name = "outputFile",
      description = "export the sequentialized program into the given file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path sequentializedProgramPath = Path.of("sequentializedProgram.c");

  public MporPreprocessingAlgorithm(
      Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      CFA pInputCfa,
      Specification pSpecification)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    options = new MPOROptions(pConfiguration);
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    cfa = pInputCfa;
    config = pConfiguration;
    specification = pSpecification;
    sequentializationStatistics =
        new SequentializationStatistics(sequentializedProgramPath, logger);
  }

  public static boolean alreadySequentialized(CFA pCFA) {
    CfaTransformationMetadata transformationMetadata =
        pCFA.getMetadata().getTransformationMetadata();
    if (transformationMetadata == null) {
      return false;
    }

    ProgramTransformation transformation = transformationMetadata.transformation();
    return transformation.equals(ProgramTransformation.SEQUENTIALIZATION_ATTEMPTED);
  }

  private CFA preprocessCfaUsingSequentialization(CFA pOldCFA)
      throws UnrecognizedCodeException,
          InterruptedException,
          ParserException,
          InvalidConfigurationException {

    logger.log(Level.INFO, "Starting sequentialization of the program.");
    sequentializationStatistics.sequentializationTime.start();
    ImmutableCFA newCFA;
    try {
      SequentializationUtils utils =
          SequentializationUtils.of(cfa, config, logger, shutdownNotifier);
      String sequentializedCode = Sequentialization.tryBuildProgramString(options, cfa, utils);
      sequentializationStatistics.sequentializedProgramString = sequentializedCode;
      // disable preprocessing in the updated config, since input cfa was preprocessed already
      Configuration configWithoutPreprocessor =
          Configuration.builder()
              .copyFrom(config)
              .setOption("parser.usePreprocessor", "false")
              .build();
      CFACreator cfaCreator = new CFACreator(configWithoutPreprocessor, logger, shutdownNotifier);
      newCFA = cfaCreator.parseSourceAndCreateCFA(sequentializedCode);

      newCFA =
          newCFA.copyWithMetadata(
              getNewMetadata(pOldCFA, newCFA, ProgramTransformation.SEQUENTIALIZATION_ATTEMPTED));
    } finally {
      sequentializationStatistics.sequentializationTime.stop();
    }

    logger.log(Level.INFO, "Finished sequentialization of the program.");

    return newCFA;
  }

  private static CfaMetadata getNewMetadata(
      CFA pOldCFA, CFA pNewCfa, ProgramTransformation pSequentializationStatus) {

    return pNewCfa
        .getMetadata()
        .withTransformationMetadata(
            new CfaTransformationMetadata(pOldCFA, pSequentializationStatus));
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
      } catch (UnrecognizedCodeException | ParserException e) {
        logger.logUserException(
            Level.WARNING,
            e,
            "Sequentialization of the input program failed, falling back to using the original"
                + " program.");
        CfaMetadata newMetadata =
            getNewMetadata(cfa, newCfa, ProgramTransformation.SEQUENTIALIZATION_ATTEMPTED);
        // Mark the CFA as having failed sequentialization
        // TODO: Simplify with sealed classes
        if (cfa instanceof ImmutableCFA immutableCfa) {
          newCfa = immutableCfa.copyWithMetadata(newMetadata);
        } else {
          throw new AssertionError("Expected ImmutableCFA here.");
        }
      } catch (InvalidConfigurationException e) {
        throw new CPAException(
            "The configuration used to build the CFA from the sequentialized program is wrong.", e);
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
      if (cpa instanceof StatisticsProvider statisticsProvider) {
        statisticsProvider.collectStatistics(sequentializationStatistics.innerStatistics);
      }

      innerAlgorithm = coreComponents.createAlgorithm(cpa, specification);
      if (innerAlgorithm instanceof StatisticsProvider statisticsProvider) {
        statisticsProvider.collectStatistics(sequentializationStatistics.innerStatistics);
      }
    } catch (InvalidConfigurationException e) {
      throw new CPAException(
          "Building the algorithm which should be run on the sequentialized program failed", e);
    }

    // Prepare new reached set
    pReachedSet.clear();
    coreComponents.initializeReachedSet(pReachedSet, newCfa.getMainFunction(), cpa);

    return innerAlgorithm.run(pReachedSet);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(sequentializationStatistics);
  }

  private static class SequentializationStatistics implements Statistics {

    private final List<Statistics> innerStatistics = new ArrayList<>();

    private final StatTimer sequentializationTime = new StatTimer("Sequentialization Time");
    private final @Nullable Path programOutputPath;
    private final LogManager statisticsLogger;

    private @Nullable String sequentializedProgramString = null;

    private SequentializationStatistics(Path pProgramOutputPath, LogManager pLogger) {
      programOutputPath = pProgramOutputPath;
      statisticsLogger = pLogger;
    }

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      StatisticsWriter w0 = writingStatisticsTo(out);
      w0.put(sequentializationTime);
      for (Statistics stat : innerStatistics) {
        stat.printStatistics(out, result, reached);
      }
    }

    @Override
    public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
      for (Statistics stat : innerStatistics) {
        stat.writeOutputFiles(pResult, pReached);
      }

      if (sequentializedProgramString != null && programOutputPath != null) {
        try (Writer writer = IO.openOutputFile(programOutputPath, Charset.defaultCharset())) {
          writer.write(sequentializedProgramString);
        } catch (IOException e) {
          statisticsLogger.logUserException(
              Level.WARNING, e, "Failed to write sequentialized program.");
        }
      }
    }

    @Override
    public String getName() {
      return "Sequentialization Preprocessing Statistics";
    }
  }
}
