// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.sl;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;

public class SLExpressionToFormulaVisitor extends ExpressionToFormulaVisitor {

  SLCheckAllocationDelegate delegate;

  public SLExpressionToFormulaVisitor(
      CtoFormulaConverter pCtoFormulaConverter,
      FormulaManagerView pFmgr,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints) {
    super(pCtoFormulaConverter, pFmgr, pEdge, pFunction, pSsa, pConstraints);
    assert pPts instanceof SLCheckAllocationDelegate;
    delegate = (SLCheckAllocationDelegate) pPts;
  }

  @Override
  public Formula visit(CArraySubscriptExpression pE) throws UnrecognizedCodeException {
    // TODO Auto-generated method stub
    return super.visit(pE);
  }

  @Override
  public Formula visit(CPointerExpression pE) throws UnrecognizedCodeException {
    // TODO Auto-generated method stub
    Formula loc = pE.getOperand().accept(this);


    Formula loc1 = checkAllocation(loc);


    return heap.get(loc1);
  }

  @Override
  public Formula visit(CUnaryExpression pExp) throws UnrecognizedCodeException {
    if (pExp.getOperator() != UnaryOperator.AMPER) {
      return super.visit(pExp);
    }
    CExpression operand = pExp.getOperand();
    assert operand instanceof CIdExpression;
    CIdExpression idExp = (CIdExpression) operand;
    CType type = pExp.getExpressionType();
    String varName = idExp.getDeclaration().getQualifiedName();
    int ssaIndex = ssa.getIndex(varName);
    String varNameWithAmper = UnaryOperator.AMPER.getOperator() + varName;
    ssa.setIndex(varNameWithAmper, type, ssaIndex);
    return conv.makeVariable(varNameWithAmper, type, ssa);
  }
}
