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

import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSetBuilder;

class StatementToFormulaVisitor implements CStatementVisitor<BooleanFormula, UnrecognizedCCodeException> {

  private final CtoFormulaConverter conv;
  private final CFAEdge       edge;
  private final String        function;
  private final SSAMapBuilder ssa;
  private final PointerTargetSetBuilder pts;
  private final Constraints   constraints;
  private final ErrorConditions errorConditions;

  StatementToFormulaVisitor( CtoFormulaConverter pConv,
      CFAEdge pEdge, String pFunction,
      SSAMapBuilder pSsa, PointerTargetSetBuilder pPts,
      Constraints pConstraints, ErrorConditions pErrorConditions) {
    conv = pConv;
    edge = pEdge;
    function = pFunction;
    ssa = pSsa;
    pts = pPts;
    constraints = pConstraints;
    errorConditions = pErrorConditions;
  }

  @Override
  public BooleanFormula visit(CExpressionStatement pIastExpressionStatement) {
    // side-effect free statement, ignore
    return conv.bfmgr.makeBoolean(true);
  }

  @Override
  public BooleanFormula visit(CExpressionAssignmentStatement assignment) throws UnrecognizedCCodeException {
    return conv.makeAssignment(assignment.getLeftHandSide(), assignment.getRightHandSide(), edge, function, ssa, pts, constraints, errorConditions);
  }

  @Override
  public BooleanFormula visit(CFunctionCallAssignmentStatement assignment) throws UnrecognizedCCodeException {
    return conv.makeAssignment(assignment.getLeftHandSide(), assignment.getRightHandSide(), edge, function, ssa, pts, constraints, errorConditions);
  }

  @Override
  public BooleanFormula visit(CFunctionCallStatement fexp) throws UnrecognizedCCodeException {
    // this is an external call
    // visit expression in order to print warnings if necessary
    CRightHandSideVisitor<Formula, UnrecognizedCCodeException> ev = conv.createCRightHandSideVisitor(edge, function, ssa, pts, constraints, errorConditions);
    fexp.getFunctionCallExpression().accept(ev);
    return conv.bfmgr.makeBoolean(true);
  }
}