// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.sl;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.sl.SLState.SLStateError;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.LvalueVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.java_smt.api.Formula;

public class SLLhsToFormulaVisitor extends LvalueVisitor {

  private CToFormulaConverterWithSL converter;
  private SLMemoryDelegate delegate;

  public SLLhsToFormulaVisitor(
      CToFormulaConverterWithSL pConverter,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints,
      ErrorConditions pErrorConditions) {
    super(pConverter, pEdge, pFunction, pSsa, pPts, pConstraints, pErrorConditions);
    converter = pConverter;
    assert pPts instanceof SLMemoryDelegate;
    delegate = (SLMemoryDelegate) pPts;
  }

  @Override
  public Formula visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws UnrecognizedCodeException {
    CExpression subscriptExp = pIastArraySubscriptExpression.getSubscriptExpression();
    Formula subscript =
        converter.buildTerm(
           subscriptExp,
            edge,
            function,
            ssa,
            delegate,
            constraints,
            errorConditions);

    CExpression arrayExp = pIastArraySubscriptExpression.getArrayExpression();
    Formula loc = converter.buildTerm(arrayExp, edge, function, ssa, delegate, constraints, errorConditions);
    int size = converter.getSizeof(arrayExp.getExpressionType());
    Optional<Formula> allocated = delegate.checkAllocation(loc, subscript, size);
    if (allocated.isPresent()) {
      return allocated.get();
    } else {
      delegate.addError(SLStateError.INVALID_WRITE);
      return super.visit(pIastArraySubscriptExpression);
    }
  }

  @Override
  public Formula visit(CFieldReference pIastFieldReference) throws UnrecognizedCodeException {
    // TODO Auto-generated method stub
    return super.visit(pIastFieldReference);
  }


  @Override
  public Formula visit(CPointerExpression pPointerExpression) throws UnrecognizedCodeException {
    CExpression e = pPointerExpression.getOperand();
    int size = converter.getSizeof(pPointerExpression.getExpressionType());
    Optional<Formula> allocated = delegate.checkAllocation(e.accept(this), size);
    if (allocated.isPresent()) {
      return allocated.get();
    } else {
      delegate.addError(SLStateError.INVALID_WRITE);
      return super.visit(pPointerExpression);
    }
  }

  @Override
  public Formula visit(CIdExpression pIdExp) {
    String varName = UnaryOperator.AMPER.getOperator() + pIdExp.getDeclaration().getQualifiedName();
    return converter.makeVariable(varName, pIdExp.getExpressionType(), ssa);
  }
}
