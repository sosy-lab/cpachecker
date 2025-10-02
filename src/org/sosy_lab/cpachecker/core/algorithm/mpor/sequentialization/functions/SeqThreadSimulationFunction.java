// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;

public class SeqThreadSimulationFunction extends SeqFunction {

  private final ImmutableList<String> functionBody;

  private final CIdExpression functionName;

  public SeqThreadSimulationFunction(ImmutableList<String> pFunctionBody, int pThreadId) {
    functionBody = pFunctionBody;
    functionName = SeqExpressionBuilder.buildThreadSimulationFunctionIdExpression(pThreadId);
  }

  @Override
  ImmutableList<String> buildBody() {
    return functionBody;
  }

  @Override
  CType getReturnType() {
    return CVoidType.VOID;
  }

  @Override
  CIdExpression getFunctionName() {
    return functionName;
  }

  @Override
  ImmutableList<CParameterDeclaration> getParameters() {
    return ImmutableList.of();
  }
}
