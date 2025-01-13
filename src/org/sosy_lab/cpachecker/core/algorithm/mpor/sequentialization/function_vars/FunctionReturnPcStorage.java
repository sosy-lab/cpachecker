// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

public class FunctionReturnPcStorage {

  public final CIdExpression returnPcVar;

  public final int value;

  public FunctionReturnPcStorage(CIdExpression pReturnPc, int pValue) {
    returnPcVar = pReturnPc;
    value = pValue;
  }
}
