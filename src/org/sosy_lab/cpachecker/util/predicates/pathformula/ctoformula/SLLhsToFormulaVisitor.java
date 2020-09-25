// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import java.math.BigInteger;
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
import org.sosy_lab.cpachecker.cpa.sl.SLMemoryDelegate;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder.DummyPointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryError;
import org.sosy_lab.java_smt.api.Formula;

public class SLLhsToFormulaVisitor extends LvalueVisitor {

  private SLMemoryDelegate delegate;
  private FormulaManagerView fm;

  public SLLhsToFormulaVisitor(
      CtoFormulaConverter pConverter,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      Constraints pConstraints,
      ErrorConditions pErrorConditions,
      FormulaManagerView pFm,
      SLMemoryDelegate pDelegate) {
    super(
        pConverter,
        pEdge,
        pFunction,
        pSsa,
        DummyPointerTargetSetBuilder.INSTANCE,
        pConstraints,
        pErrorConditions);
    fm = pFm;
    delegate = pDelegate;

  }

  @Override
  public Formula visit(CArraySubscriptExpression pIastArraySubscriptExpression)
      throws UnrecognizedCodeException {
    CExpression arrayExp = pIastArraySubscriptExpression.getArrayExpression();
    CExpression subscriptExp = pIastArraySubscriptExpression.getSubscriptExpression();
    Formula subscript =
        conv
            .buildTerm(subscriptExp, edge, function, ssa, pts, constraints, errorConditions);
    CType type = arrayExp.getExpressionType();
    int size;
    Formula loc;
    if (type instanceof CArrayType) {
      CType tmp = ((CArrayType) type).asPointerType().getType();
      size = conv.getSizeof(tmp);
      type = ((CArrayType) type).asPointerType();
      loc = arrayExp.accept(this);
    } else {
      size = conv.getSizeof(((CPointerType) type).getType());
      loc =
          conv
              .buildTerm(arrayExp, edge, function, ssa, pts, constraints, errorConditions);
    }
    subscript =
        conv.makeCast(subscriptExp.getExpressionType(), type, subscript, constraints, edge);
    // Formula loc = arrayExp.accept(this);
    // int size = converter.getSizeof(arrayExp.getExpressionType());
    Optional<Formula> allocated = delegate.checkAllocation(loc, subscript, size);
    if (allocated.isPresent()) {
      return allocated.orElseThrow();
    } else {
      delegate.addError(MemoryError.INVALID_WRITE);
      return null;
    }
  }

  @Override
  public Formula visit(CFieldReference pIastFieldReference) throws UnrecognizedCodeException {
    CExpression owner = pIastFieldReference.getFieldOwner();
    Formula loc =
        conv.buildTerm(owner, edge, function, ssa, pts, constraints, errorConditions);
    SLFieldToOffsetVisitor v = new SLFieldToOffsetVisitor(conv);
    BigInteger offset = v.getOffset(pIastFieldReference, edge);

    Formula off = fm.makeNumber(fm.getFormulaType(loc), offset.longValueExact());

    return fm.makePlus(loc, off);
  }

  @Override
  public Formula visit(CPointerExpression pPointerExpression) throws UnrecognizedCodeException {
    CExpression e = pPointerExpression.getOperand();
    Formula loc =
        conv.buildTerm(e, edge, function, ssa, pts, constraints, errorConditions);
    Optional<Formula> allocated = delegate.checkAllocation(loc);
    if (allocated.isPresent()) {
      return allocated.orElseThrow();
    } else {
      delegate.addError(MemoryError.INVALID_WRITE);
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
    return conv.makeVariable(varName, t, ssa);
  }
}
