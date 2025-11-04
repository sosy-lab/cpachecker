// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

public record SequentializationUtils(
    CBinaryExpressionBuilder binaryExpressionBuilder,
    LogManager logger,
    ShutdownNotifier shutdownNotifier) {

  public static SequentializationUtils of(
      MachineModel pMachineModel, LogManager pLogger, ShutdownNotifier pShutdownNotifier) {

    return new SequentializationUtils(
        new CBinaryExpressionBuilder(pMachineModel, pLogger), pLogger, pShutdownNotifier);
  }
}
