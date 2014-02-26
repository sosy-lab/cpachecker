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
package org.sosy_lab.cpachecker.util.predicates.pathformula.withUF;

import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;


class StatementToFormulaWithUFVisitor extends RightHandSideToFormulaWithUFVisitor
                                      implements CStatementVisitor<BooleanFormula, UnrecognizedCCodeException> {

  public StatementToFormulaWithUFVisitor(final CToFormulaWithUFConverter cToFormulaConverter,
                                         final CFAEdge cfaEdge,
                                         final String function,
                                         final SSAMapBuilder ssa,
                                         final Constraints constraints,
                                         final ErrorConditions errorConditions,
                                         final PointerTargetSetBuilder pts) {
    super(cToFormulaConverter, cfaEdge, function, ssa, constraints, errorConditions, pts);
  }

  @Override
  public BooleanFormula visit(final CExpressionAssignmentStatement e) throws UnrecognizedCCodeException {
    return visit((CAssignment) e);
  }

  @Override
  public BooleanFormula visit(final CFunctionCallAssignmentStatement e) throws UnrecognizedCCodeException {
    return visit((CAssignment) e);
  }

  @Override
  public BooleanFormula visit(CExpressionStatement s) {
    return conv.bfmgr.makeBoolean(true);
  }

  @Override
  public BooleanFormula visit(CFunctionCallStatement exp) throws UnrecognizedCCodeException {
    // this is an external call
    // visit expression in order to print warnings if necessary
    visit(exp.getFunctionCallExpression());
    return conv.bfmgr.makeBoolean(true);
  }

  private BooleanFormula visit(final CAssignment e) throws UnrecognizedCCodeException {
    AssignmentHandler assignmentHandler = new AssignmentHandler(conv, edge, function, ssa, pts, constraints, errorConditions);
    return assignmentHandler.handleAssignment(e.getLeftHandSide(), e.getRightHandSide(), false, null);
  }
}
