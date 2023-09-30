// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyupperText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rangedAnalysisSequence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopEqualsOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.cpa.rangedAnalysisSequence.SequenceBoundAnalysis.SequenceCPA;
import org.sosy_lab.cpachecker.cpa.rangedAnalysisSequence.SequenceBoundAnalysis.SequenceState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "cpa.rangedAnalysis")
public class RangedAnalysisCPA extends AbstractCPA implements ProofCheckerCPA {

  private final LogManager logger;

  private final SequenceCPA lower;
  private final SequenceCPA upper;

  private final boolean lowerBoundExists;
  private final boolean upperBoundExists;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(RangedAnalysisCPA.class);
  }

  protected String suffixForLowerInputFile = ".lower";
  protected String suffixForUpperInputFile = ".upper";

  @Option(
      secure = true,
      description = "If no path is given for the path2InputFile, try to load it automatically")
  protected boolean autoLoadPath2Input = false;

  @Option(
      secure = true,
      description =
          "The path for the input file to handle input/random values for the constant propagation"
              + " following the lower bound. If no path is given, there is no lower bound.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  protected Path path2LowerInputFile = null;

  @Option(
      secure = true,
      description =
          "The path for the input file to handle input/random values for the constant propagation"
              + " following the upper bound. If no path is given, there is no upper bound.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  protected Path path2UpperInputFile = null;

  @SuppressWarnings("unused")
  private RangedAnalysisCPA(
      Configuration config, LogManager pLogger, CFA pCfa, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(DelegateAbstractDomain.getInstance(), new RangedAnalysisTransferRelation(pLogger));
    config.inject(this);
    this.logger = pLogger;
    this.shutdownNotifier = pShutdownNotifier;
    this.cfa = pCfa;
    if (Objects.isNull(path2LowerInputFile) && autoLoadPath2Input) {
      Optional<Path> optPath = loadPath2InputFile(suffixForLowerInputFile, pCfa);
      if (optPath.isPresent()) {
        path2LowerInputFile = optPath.orElseThrow();
        logger.log(
            Level.INFO,
            Level.INFO,
            String.format("Using the file %s for the lower bound", path2LowerInputFile));
      }
    }

    if (Objects.isNull(path2UpperInputFile) && autoLoadPath2Input) {
      Optional<Path> optPath = loadPath2InputFile(suffixForUpperInputFile, pCfa);
      if (optPath.isPresent()) {
        path2UpperInputFile = optPath.orElseThrow();
        logger.log(
            Level.INFO,
            Level.INFO,
            String.format("Using the file %s for the upper bound", path2UpperInputFile));
      }
    }
    logger.logf(
        Level.INFO,
        "Using %s as lower bound and %s as upper bound",
        path2LowerInputFile,
        path2UpperInputFile);
    RangedAnalysisTransferRelation transferRelation =
        (RangedAnalysisTransferRelation) getTransferRelation();
    upper = getCPAForBound(path2UpperInputFile, config, path2UpperInputFile != null);
    lower = getCPAForBound(path2LowerInputFile, config, false);
    transferRelation.setCPTRs(lower.getTransferRelation(), upper.getTransferRelation());
    this.upperBoundExists = Objects.nonNull(path2UpperInputFile);
    this.lowerBoundExists = Objects.nonNull(path2LowerInputFile);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new RangedAnalysisState(
        lowerBoundExists ? (SequenceState) this.lower.getInitialState(node, partition) : null,
        upperBoundExists ? (SequenceState) this.upper.getInitialState(node, partition) : null);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return new MergeSepOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopEqualsOperator();
  }

  /**
   * Updates the option cpa.value.functionValuesForRandom in the config to pPath and builds a
   * ValueAnalysisCPA
   *
   * @param pPath the path to replace, may be null
   * @param config the original config
   * @return a ValueAnalysisCPA
   * @throws InvalidConfigurationException if an error occurs
   */
  private SequenceCPA getCPAForBound(Path pPath, Configuration config, boolean pIsUpperBound)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    ConfigurationBuilder builder = Configuration.builder().copyFrom(config);

    if (Objects.nonNull(pPath)) {
      builder.setOption(
          "cpa.sequenceCPA.stopIfUnderspecifiedTestcase", Boolean.toString(pIsUpperBound));
      builder.setOption("cpa.sequenceCPA.path2Bound", pPath.toAbsolutePath().toString());
    }
    CPAFactory factory =
        SequenceCPA.factory()
            .setConfiguration(builder.build())
            .setLogger(logger)
            .setShutdownNotifier(shutdownNotifier)
            .set(cfa, CFA.class);
    return (SequenceCPA) factory.createInstance();
  }

  /**
   * Check if there is a InputFile in xml format located in the same directory then the testcase,
   * with the same name (+ suffix). If yes, return the path to the file, otherwise retunr an empty
   * optional
   *
   * @param suffix optional parameter of the suffix, can be empty
   * @param pCfa the cfa to load the path of the original file
   * @return the path if it exists, otherwise an empty optional.
   */
  public static Optional<Path> loadPath2InputFile(String suffix, CFA pCfa) {

    List<Path> files = pCfa.getFileNames();
    if (files.size() == 1) {
      String filename = files.get(0).toAbsolutePath().toString();
      String testcasePath = filename.substring(0, filename.lastIndexOf("."));
      if (!suffix.isEmpty()) {
        testcasePath = testcasePath + suffix;
      }
      testcasePath = testcasePath + ".xml";
      Path targetPath = Path.of(testcasePath);
      if (Files.exists(targetPath)) {
        return Optional.of(targetPath);
      }
    }
    return Optional.empty();
  }
}
