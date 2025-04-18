// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function_statements;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;

/**
 * A class to keep track of function return value assignments (e.g. {@code CPAchecker_TMP = retval;}
 * to the respective calling context.
 */
public class FunctionReturnValueAssignment {

  public final CExpressionAssignmentStatement statement;

  public FunctionReturnValueAssignment(CLeftHandSide pLeftHandSide, CExpression pReturnExpression) {

    statement =
        SeqStatementBuilder.buildExpressionAssignmentStatement(pLeftHandSide, pReturnExpression);
  }
}
