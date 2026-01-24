// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serial;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
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
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
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
@Options(prefix = "analysis.preprocessing.MPOR")
public class MporPreprocessingAlgorithm implements Algorithm, StatisticsProvider {

  @Option(
      secure = true,
      description =
          "The file name for the exported sequentialization metadata that contains e.g. the input"
              + " file name(s).")
  @FileOption(Type.OUTPUT_FILE)
  private @Nullable Path metadataPath = Path.of("sequentializedProgramMetadata.yml");

  @Option(secure = true, description = "The file name for the exported sequentialized program.")
  @FileOption(Type.OUTPUT_FILE)
  private @Nullable Path programPath = Path.of("sequentializedProgram.c");

  @Option(
      secure = true,
      description =
          "If the sequentialization should be performed as a preprocessing step before the main"
              + " analysis begins, set to true. The sequentialized program is then analyzed by the"
              + " CPAchecker analysis given. Note that the CFA is transformed at the beginning of"
              + " the analysis, so all (sub-)analyses will also operate on the sequentialized CFA."
              + " In particular this means that if you use a parallel or sequential composition of"
              + " analyses, all of them will analyze the sequentialized CFA.\n"
              + "If the sequentialization should just be exported and analyzed externally, set to"
              + " false.")
  private boolean runAnalysis = true;

  private final Configuration config;

  private final LogManager logger;

  private final ShutdownNotifier shutdownNotifier;

  private final CFA cfa;

  private final Specification specification;

  private final MPOROptions options;

  private final SequentializationUtils utils;

  private final SequentializationStatistics sequentializationStatistics;

  public MporPreprocessingAlgorithm(
      Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Specification pSpecification)
      throws InvalidConfigurationException {

    pConfiguration.inject(this);

    config = pConfiguration;
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;
    specification = pSpecification;
    options = new MPOROptions(pConfiguration);
    utils = SequentializationUtils.of(cfa, config, logger, shutdownNotifier);
    sequentializationStatistics = new SequentializationStatistics(programPath, logger);
  }

  private static boolean isAlreadySequentialized(CFA pCFA) {
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
          InvalidConfigurationException,
          UnsupportedSequentializationException {

    logger.log(Level.INFO, "Starting sequentialization of the program.");
    sequentializationStatistics.sequentializationTime.start();
    ImmutableCFA newCFA;
    try {
      String sequentializedCode = sequentializeAndExportProgram();
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

    logger.log(Level.INFO, "Finished sequentialization of the input program.");

    return newCFA;
  }

  private static CfaMetadata getNewMetadata(
      CFA pOldCFA, CFA pNewCfa, ProgramTransformation pSequentializationStatus)
      throws UnsupportedSequentializationException {
    if (!pOldCFA
        .getMainFunction()
        .getFunctionName()
        .equals(pNewCfa.getMainFunction().getFunctionName())) {
      throw new UnsupportedSequentializationException(
          "We can only sequentialize programs without changing the main function name.");
    }

    return pNewCfa
        .getMetadata()
        .withTransformationMetadata(
            new CfaTransformationMetadata(pOldCFA, pSequentializationStatus));
  }

  @CanIgnoreReturnValue
  @Override
  public AlgorithmStatus run(@NonNull ReachedSet pReachedSet)
      throws CPAException, InterruptedException {

    // if this instance is not for the internal analysis, export sequentialization and return
    if (!runAnalysis) {
      sequentializeAndExportProgram();
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }

    // Only sequentialize if not already done and requested.
    // We replace the CFA for its sequentialized version.
    CFA newCfa = cfa;

    Verify.verify(
        !isAlreadySequentialized(cfa),
        "The given CFA is already sequentialized, cannot perform sequentialization.");

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

    final CoreComponentsFactory coreComponents;
    final ConfigurableProgramAnalysis cpa;
    Algorithm innerAlgorithm;

    try {
      // set useMporPreprocessing=false so that CoreComponentsFactory does not sequentialize again
      Configuration newConfig =
          Configuration.builder()
              .copyFrom(config)
              .setOption("analysis.preprocessing.MPOR", "false")
              .build();
      coreComponents =
          new CoreComponentsFactory(
              newConfig, logger, shutdownNotifier, AggregatedReachedSets.empty(), newCfa);
      cpa = coreComponents.createCPA(specification);
      if (cpa instanceof StatisticsProvider statisticsProvider) {
        statisticsProvider.collectStatistics(sequentializationStatistics.innerStatistics);
      }

      innerAlgorithm = coreComponents.createAlgorithm(cpa, specification);
      if (innerAlgorithm instanceof StatisticsProvider statisticsProvider) {
        statisticsProvider.collectStatistics(sequentializationStatistics.innerStatistics);
      }
    } catch (InvalidConfigurationException e) {
      throw new UnsupportedSequentializationException(
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

    /** The output program path can be null, when the option to output no files is specified. */
    private final @Nullable Path programOutputPath;

    private final LogManager statisticsLogger;

    private @Nullable String sequentializedProgramString = null;

    private SequentializationStatistics(@Nullable Path pProgramOutputPath, LogManager pLogger) {
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

  public static class UnsupportedSequentializationException extends CPAException {
    @Serial private static final long serialVersionUID = 7637130884987670847L;

    public UnsupportedSequentializationException(String msg) {
      super(msg);
    }

    public UnsupportedSequentializationException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }

  /**
   * Sequentializes the input {@link CFA} based on the fields in this instance and exports the
   * resulting program. The return value can be ignored, if the program is not analyzed inside
   * CPAchecker directly.
   */
  @CanIgnoreReturnValue
  private String sequentializeAndExportProgram()
      throws UnrecognizedCodeException, InterruptedException {

    InputRejection.handleRejections(cfa);
    String rProgram = Sequentialization.tryBuildProgramString(options, cfa, utils);
    handleExport(rProgram, cfa.getFileNames());
    return rProgram;
  }

  private static final String PROGRAM_NOT_EXPORTED_MESSAGE =
      "Sequentialized program was not exported.";

  private static final String METADATA_NOT_EXPORTED_MESSAGE =
      "Sequentialization metadata was not exported.";

  public void handleExport(String pOutputProgram, List<Path> pInputFilePaths) {

    // write output program, if the path is successfully determined
    if (programPath != null) {
      try {
        try (Writer writer = IO.openOutputFile(programPath, Charset.defaultCharset())) {
          writer.write(pOutputProgram);
          logger.log(Level.INFO, "Sequentialized program exported to: ", programPath.toString());
        }
      } catch (IOException e) {
        logger.logUserException(
            Level.WARNING,
            e,
            "An IO error occurred while writing the output program. "
                + PROGRAM_NOT_EXPORTED_MESSAGE);
      }
    } else {
      logger.log(
          Level.WARNING,
          "Could not determine path for sequentialization. " + PROGRAM_NOT_EXPORTED_MESSAGE);
    }

    // write metadata, if the path is successfully determined
    if (metadataPath != null) {
      YAMLMapper yamlMapper = new YAMLMapper();
      MetadataRecord metadataRecord = buildMetadataRecord(pInputFilePaths);
      try {
        yamlMapper.writeValue(metadataPath.toFile(), metadataRecord);
        logger.log(Level.INFO, "Sequentialization metadata exported to: ", metadataPath.toString());
      } catch (IOException e) {
        logger.logUserException(
            Level.WARNING,
            e,
            "An error occurred while writing metadata. " + METADATA_NOT_EXPORTED_MESSAGE);
      }
    } else {
      logger.log(
          Level.WARNING,
          "Could not determine path for sequentialization metadata. "
              + METADATA_NOT_EXPORTED_MESSAGE);
    }
  }

  private record InputFileRecord(
      @JsonProperty("name") String name, @JsonProperty("path") String path) {}

  private record MetadataRecord(
      @JsonProperty("cpachecker_version") String cpaCheckerVersion,
      @JsonProperty("utc_creation_time") String utcCreationTime,
      @JsonProperty("input_files") List<InputFileRecord> inputFiles) {}

  private MetadataRecord buildMetadataRecord(List<Path> pInputFilePaths) {
    String utcCreationTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    ImmutableList<InputFileRecord> inputFiles =
        transformedImmutableListCopy(
            pInputFilePaths,
            path ->
                new InputFileRecord(
                    Objects.requireNonNull(path).getFileName().toString(), path.toString()));
    return new MetadataRecord(CPAchecker.getPlainVersion(), utcCreationTime, inputFiles);
  }
}
