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
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class SeqThreadSimulationFunction extends SeqFunction {

  public final MPORThread thread;

  private final ImmutableList<String> functionBody;

  private final CIdExpression functionName;

  private final String functionCall;

  public SeqThreadSimulationFunction(ImmutableList<String> pFunctionBody, MPORThread pThread) {
    functionBody = pFunctionBody;
    functionName = SeqExpressionBuilder.buildThreadSimulationFunctionIdExpression(pThread.getId());
    thread = pThread;
    functionCall = buildFunctionCallStatement(ImmutableList.of()).toASTString();
  }

  public String getFunctionCall() {
    return functionCall;
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
  ImmutableList<CParameterDeclaration> getParameterDeclarations() {
    return ImmutableList.of();
  }
}
