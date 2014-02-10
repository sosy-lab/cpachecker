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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

class StatementToFormulaVisitor extends RightHandSideToFormulaVisitor implements CStatementVisitor<BooleanFormula, UnrecognizedCCodeException> {

  public StatementToFormulaVisitor(ExpressionToFormulaVisitor pDelegate) {
    super(pDelegate);
  }

  @Override
  public BooleanFormula visit(CExpressionStatement pIastExpressionStatement) {
    // side-effect free statement, ignore
    return conv.bfmgr.makeBoolean(true);
  }

  /**
   * Creates formulas for the given assignment (left and right side).
   * And returns all formulas to be able to create aliasing formulas.
   * @param assignment the assignment to process
   * @return a triple of right, left and assignment formula (in this order which is chronological)
   * @throws UnrecognizedCCodeException
   */
  public Triple<Formula, Formula, BooleanFormula> visitAssignment(CAssignment assignment) throws UnrecognizedCCodeException {
    CRightHandSide rhs = assignment.getRightHandSide();
    CLeftHandSide lhs = assignment.getLeftHandSide();

    if (rhs instanceof CExpression) {
      rhs = conv.makeCastFromArrayToPointerIfNecessary((CExpression)rhs, lhs.getExpressionType());
    }

    Formula r = rhs.accept(this);
    Formula l = conv.buildLvalueTerm(lhs, edge, function, ssa, constraints);
    r = conv.makeCast(
          rhs.getExpressionType(),
          lhs.getExpressionType(),
          r,
          edge);

    BooleanFormula a = conv.fmgr.assignment(l, r);
    return Triple.of(r, l, a);
  }

  public BooleanFormula visit(CAssignment assignment) throws UnrecognizedCCodeException {
    // No need to alias anything so just return the assignment
    return
          visitAssignment(assignment).getThird();
  }

  @Override
  public BooleanFormula visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) throws UnrecognizedCCodeException {
    return visit((CAssignment)pIastExpressionAssignmentStatement);
  }

  @Override
  public BooleanFormula visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) throws UnrecognizedCCodeException {
    return visit((CAssignment)pIastFunctionCallAssignmentStatement);
  }

  @Override
  public BooleanFormula visit(CFunctionCallStatement fexp) throws UnrecognizedCCodeException {
    // this is an external call
    // visit expression in order to print warnings if necessary
    visit(fexp.getFunctionCallExpression());
    return conv.bfmgr.makeBoolean(true);
  }
}