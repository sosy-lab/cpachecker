// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.input_checks;

import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class InputWarning {

  enum InputWarningMessage {
    NOT_CONCURRENT(
        "MPOR expects a concurrent C program with at least one pthread_create call."
            + " The program is sequentialized regardless.");

    final String message;

    InputWarningMessage(String pMessage) {
      message = pMessage;
    }
  }

  public static void handleWarnings(CFA pInputCfa, LogManager pLogger) {
    checkIsParallelProgram(pInputCfa, pLogger);
  }

  private static void checkIsParallelProgram(CFA pInputCfa, LogManager pLogger) {
    boolean isParallel = false;
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      Optional<CFunctionCall> functionCall = PthreadUtil.tryGetFunctionCallFromCfaEdge(cfaEdge);
      if (functionCall.isPresent()) {
        if (PthreadUtil.isCallToPthreadFunction(
            functionCall.orElseThrow(), PthreadFunctionType.PTHREAD_CREATE)) {
          isParallel = true;
          break;
        }
      }
    }
    if (!isParallel) {
      pLogger.log(Level.WARNING, InputWarningMessage.NOT_CONCURRENT);
    }
  }
}
