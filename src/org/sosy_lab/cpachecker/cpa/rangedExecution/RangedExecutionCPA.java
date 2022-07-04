// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rangedExecution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "cpa.rangedExecution")
public class RangedExecutionCPA extends AbstractCPA implements ProofCheckerCPA {

  private final LogManager logger;

  private final ValueAnalysisCPA left;
  private final ValueAnalysisCPA right;

  private final boolean leftBoundExists;
  private final boolean rightBoundExists;
  private final ShutdownNotifier shutdownNotifier;
  private final CFA cfa;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(RangedExecutionCPA.class);
  }

  protected String suffixForLeftInputFile = ".left";
  protected String suffixForRightInputFile = ".right";

  @Option(
      secure = true,
      description = "If no path is given for the path2InputFile, try to load it automatically")
  protected boolean autoLoadPath2Input = false;

  @Option(
      secure = true,
      description =
          "The path for the input file to handle input/random values for the constant propagation"
              + " following the left bound. If no path is given, there is no left bound.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  protected Path path2LeftInputFile = null;

  @Option(
      secure = true,
      description =
          "The path for the input file to handle input/random values for the constant propagation"
              + " following the right bound. If no path is given, there is no right bound.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  protected Path path2RightInputFile = null;

  @SuppressWarnings("unused")
  private RangedExecutionCPA(
      Configuration config, LogManager pLogger, CFA pCfa, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(DelegateAbstractDomain.getInstance(), new RangedExecutionTransferRelation(pLogger));
    config.inject(this);
    this.logger = pLogger;
    this.shutdownNotifier = pShutdownNotifier;
    this.cfa = pCfa;
    if (Objects.isNull(path2LeftInputFile) && autoLoadPath2Input) {
      Optional<Path> optPath = loadPath2InputFile(suffixForLeftInputFile, pCfa);
      if (optPath.isPresent()) {
        path2LeftInputFile = optPath.get();
        logger.log(
            Level.INFO,
            Level.INFO,
            String.format("Using the file %s for the left bound", path2LeftInputFile));
      }
    }

    if (Objects.isNull(path2RightInputFile) && autoLoadPath2Input) {
      Optional<Path> optPath = loadPath2InputFile(suffixForRightInputFile, pCfa);
      if (optPath.isPresent()) {
        path2RightInputFile = optPath.get();
        logger.log(
            Level.INFO,
            Level.INFO,
            String.format("Using the file %s for the right bound", path2RightInputFile));
      }
    }
    logger.logf(
        Level.INFO,
        "Using %s as left bound and %s as right bound",
        path2LeftInputFile,
        path2RightInputFile);
    RangedExecutionTransferRelation transferRelation =
        (RangedExecutionTransferRelation) getTransferRelation();
    right = getCPAForBound(path2RightInputFile, config, true);
    left = getCPAForBound(path2LeftInputFile, config, false);
    transferRelation.setCPTRs(left.getTransferRelation(), right.getTransferRelation());
    this.rightBoundExists = Objects.nonNull(path2RightInputFile);
    this.leftBoundExists = Objects.nonNull(path2LeftInputFile);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new RangedExecutionState(
        leftBoundExists ? (ValueAnalysisState) this.left.getInitialState(node, partition) : null,
        rightBoundExists ? (ValueAnalysisState) this.right.getInitialState(node, partition) : null);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return new MergeSepOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopNeverOperator();
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
  private ValueAnalysisCPA getCPAForBound(Path pPath, Configuration config, boolean pIsRightBound)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    ConfigurationBuilder builder = Configuration.builder().copyFrom(config);

    if (Objects.nonNull(pPath)) {
      builder.setOption("cpa.value.ignoreFunctionValueExceptRandom", Boolean.toString(true));
      builder.setOption(
          "cpa.value.stopIfAllValuesForUnknownAreUsed", Boolean.toString(pIsRightBound));
      builder.setOption("cpa.value.functionValuesForRandom", pPath.toAbsolutePath().toString());
    }
    CPAFactory factory =
        ValueAnalysisCPA.factory()
            .setConfiguration(builder.build())
            .setLogger(logger)
            .setShutdownNotifier(shutdownNotifier)
            .set(cfa, CFA.class);
    return (ValueAnalysisCPA) factory.createInstance();
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
        testcasePath = testcasePath.concat(suffix);
      }
      testcasePath = testcasePath.concat(".xml");
      Path targetPath = Path.of(testcasePath);
      if (Files.exists(targetPath)) {
        return Optional.of(targetPath);
      }
    }
    return Optional.empty();
  }
}
