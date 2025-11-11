// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.output.MPORWriter;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

/**
 * The Modular Partial Order Reduction (MPOR) algorithm produces a sequentialization of a concurrent
 * C program. The algorithm contains options that allow both static and dynamic reductions in the
 * state space. Sequentializations can be given to any verifier capable of verifying sequential C
 * programs, hence modular.
 */
@SuppressWarnings("unused") // this is necessary because we don't use the cpa and config
public class MPORAlgorithm implements Algorithm /* TODO statistics? */ {

  private final MPOROptions options;

  @CanIgnoreReturnValue
  @Override
  public AlgorithmStatus run(@Nullable ReachedSet pReachedSet) throws CPAException {
    String sequentializedProgram = buildSequentializedProgram();
    MPORWriter.write(options, sequentializedProgram, cfa.getFileNames(), logger);
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  public String buildSequentializedProgram() {
    // just use the first input file name for naming purposes
    Path firstInputFilePath = cfa.getFileNames().getFirst();
    String inputFileName = firstInputFilePath.toString();
    return Sequentialization.tryBuildProgramString(
        options, cfa, inputFileName, logger, shutdownNotifier);
  }

  private final ConfigurableProgramAnalysis cpa;

  private final LogManager logger;

  private final Configuration config;

  private final ShutdownNotifier shutdownNotifier;

  private final CFA cfa;

  public MPORAlgorithm(
      @Nullable ConfigurableProgramAnalysis pCpa,
      Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      CFA pInputCfa,
      @Nullable MPOROptions pOptions)
      throws InvalidConfigurationException {

    // the options are not null when unit testing
    options = pOptions != null ? pOptions : new MPOROptions(pConfiguration, pLogManager);

    cpa = pCpa;
    config = pConfiguration;
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    cfa = pInputCfa;

    InputRejection.handleRejections(logger, cfa);
  }

  public static MPORAlgorithm testInstance(
      LogManager pLogManager, CFA pInputCfa, MPOROptions pOptions)
      throws InvalidConfigurationException {

    return new MPORAlgorithm(
        null,
        TestDataTools.configurationForTest().build(),
        pLogManager,
        ShutdownNotifier.createDummy(),
        pInputCfa,
        pOptions);
  }

  public static MPORAlgorithm testInstanceWithConfig(
      Configuration pConfig, LogManager pLogManager, CFA pInputCfa, MPOROptions pOptions)
      throws InvalidConfigurationException {

    return new MPORAlgorithm(
        null, pConfig, pLogManager, ShutdownNotifier.createDummy(), pInputCfa, pOptions);
  }
}
