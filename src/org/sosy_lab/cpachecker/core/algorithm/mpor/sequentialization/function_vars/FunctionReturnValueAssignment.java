// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;

public class FunctionReturnValueAssignment {

  public final FunctionReturnPcStorage returnPcStorage;

  public final CExpressionAssignmentStatement statement;

  public FunctionReturnValueAssignment(
      FunctionReturnPcStorage pReturnPcStorage,
      CLeftHandSide pLeftHandSide,
      CExpression pReturnExpression) {

    returnPcStorage = pReturnPcStorage;
    statement = SeqExpressionAssignmentStatement.build(pLeftHandSide, pReturnExpression);
  }
}
