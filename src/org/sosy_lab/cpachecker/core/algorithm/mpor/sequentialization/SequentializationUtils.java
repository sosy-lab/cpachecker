// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.util.cwriter.ClangFormatter;

public record SequentializationUtils(
    CBinaryExpressionBuilder binaryExpressionBuilder,
    ClangFormatter clangFormatter,
    LogManager logger,
    ShutdownNotifier shutdownNotifier) {

  public static SequentializationUtils of(
      CFA pCfa,
      Configuration pConfiguration,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    return new SequentializationUtils(
        new CBinaryExpressionBuilder(pCfa.getMachineModel(), pLogger),
        new ClangFormatter(pConfiguration, pLogger),
        pLogger,
        pShutdownNotifier);
  }
}
