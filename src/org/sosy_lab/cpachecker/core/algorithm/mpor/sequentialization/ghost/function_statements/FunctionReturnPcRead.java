// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.function_statements;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

public class FunctionReturnPcRead {

  public final int threadId;

  public final CIdExpression returnPcVar;

  public FunctionReturnPcRead(int pThreadId, CIdExpression pReturnPcVar) {
    threadId = pThreadId;
    returnPcVar = pReturnPcVar;
  }
}
