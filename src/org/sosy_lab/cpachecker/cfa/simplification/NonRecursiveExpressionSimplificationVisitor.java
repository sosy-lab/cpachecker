/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.simplification;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

/**
 * This visitor visits an expression and evaluates it.
 * It tries to evaluate only the outermost operator,
 * i.e., it evaluates 1+2 to 3, but not (1+1)+1.
 * If evaluation is successful, it returns a CIntegerLiteralExpression with the new value,
 * otherwise it returns the original expression.
 */
public class NonRecursiveExpressionSimplificationVisitor extends ExpressionSimplificationVisitor {

  public NonRecursiveExpressionSimplificationVisitor(MachineModel mm, LogManagerWithoutDuplicates pLogger) {
    super(mm, pLogger);
  }

  /** return a simplified version of the expression.
   * --> disabled, because this is the "non-recursive-visitor". */
  protected CExpression recursive(CExpression expr) {
    return expr;
  }
}
