// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.FunctionCallExpr;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class FunctionSignature implements SeqElement {

  public final String returnType;

  public final FunctionCallExpr functionCallExpr;

  public FunctionSignature(String pReturnType, FunctionCallExpr pFunctionCallExpr) {
    returnType = pReturnType;
    functionCallExpr = pFunctionCallExpr;
  }

  @Override
  public String toString() {
    return returnType + SeqSyntax.SPACE + functionCallExpr.toString();
  }
}
