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
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
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
    CExpression arrayExp = pIastArraySubscriptExpression.getArrayExpression();
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
    CType type = arrayExp.getExpressionType();
    int size;
    Formula loc;
    if (type instanceof CArrayType) {
      CType tmp = ((CArrayType) type).asPointerType().getType();
      size = converter.getSizeof(tmp);
      type = ((CArrayType) type).asPointerType();
      loc = arrayExp.accept(this);
    } else {
      size = converter.getSizeof(((CPointerType) type).getType());
      loc =
          converter
              .buildTerm(arrayExp, edge, function, ssa, delegate, constraints, errorConditions);
    }
    subscript =
        converter.makeCast(subscriptExp.getExpressionType(), type, subscript, constraints, edge);
    // Formula loc = arrayExp.accept(this);
    // int size = converter.getSizeof(arrayExp.getExpressionType());
    Optional<Formula> allocated = delegate.checkAllocation(loc, subscript, size);
    if (allocated.isPresent()) {
      return allocated.get();
    } else {
      delegate.addError(SLStateError.INVALID_WRITE);
      return null;
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
    Formula loc =
        converter.buildTerm(e, edge, function, ssa, delegate, constraints, errorConditions);
    int size = converter.getSizeof(pPointerExpression.getExpressionType());
    Optional<Formula> allocated = delegate.checkAllocation(loc, size);
    if (allocated.isPresent()) {
      return allocated.get();
    } else {
      delegate.addError(SLStateError.INVALID_WRITE);
      return null;
    }
  }

  @Override
  public Formula visit(CIdExpression pIdExp) {
    CType type = pIdExp.getExpressionType();
    if (type instanceof CArrayType) {
      type = ((CArrayType) type).asPointerType();
    }
    String varName = UnaryOperator.AMPER.getOperator() + pIdExp.getDeclaration().getQualifiedName();
    CType t = delegate.makeLocationTypeForVariableType(type);
    return converter.makeVariable(varName, t, ssa);
  }
}
