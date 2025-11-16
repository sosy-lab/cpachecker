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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

/**
 * The Modular Partial Order Reduction (MPOR) algorithm produces a sequentialization of a concurrent
 * C program. The algorithm contains options that allow both static and dynamic reductions in the
 * state space. Sequentializations can be given to any verifier capable of verifying sequential C
 * programs, hence modular.
 */
public class MPORAlgorithm implements Algorithm /* TODO statistics? */ {

  private final CFA cfa;

  private final MPOROptions options;

  private final SequentializationUtils utils;

  public MPORAlgorithm(
      Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      CFA pInputCfa,
      @Nullable MPOROptions pOptions)
      throws InvalidConfigurationException, UnsupportedCodeException {

    // the options are not null when unit testing
    options = pOptions != null ? pOptions : new MPOROptions(pConfiguration);
    cfa = pInputCfa;
    utils = SequentializationUtils.of(cfa, pConfiguration, pLogManager, pShutdownNotifier);
    InputRejection.handleRejections(cfa);
  }

  @CanIgnoreReturnValue
  @Override
  public AlgorithmStatus run(@Nullable ReachedSet pReachedSet)
      throws CPAException, InterruptedException {

    String sequentializedProgram = buildSequentializedProgram();
    MPORWriter.write(options, sequentializedProgram, cfa.getFileNames(), utils.logger());
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  public String buildSequentializedProgram()
      throws UnrecognizedCodeException, InterruptedException {

    // just use the first input file name for naming purposes
    Path firstInputFilePath = cfa.getFileNames().getFirst();
    String inputFileName = firstInputFilePath.toString();
    return Sequentialization.tryBuildProgramString(options, cfa, inputFileName, utils);
  }
}
