// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BackwardBMCAlgorithm implements Algorithm {

  private LogManager logger;
  private Algorithm algorithm;
  private ConfigurableProgramAnalysis cpa;

  public BackwardBMCAlgorithm(
      Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA, LogManager pLogger) {

    logger = pLogger;
    algorithm = pAlgorithm;
    cpa = pCPA;
  }

  @Override
  public AlgorithmStatus run(final ReachedSet reachedSet)
      throws CPAException, InterruptedException {

    AlgorithmStatus status;
    status = BMCHelper.unroll(logger, reachedSet, algorithm, cpa);
    return status;
  }
}
