// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.sl;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;

public class SLFieldToOffsetVisitor implements CTypeVisitor<Void, UnrecognizedCodeException> {

  private final CtoFormulaConverter delegate;
  private String fieldName;
  private BigInteger offset;
  private CFAEdge edge;

  public SLFieldToOffsetVisitor(CtoFormulaConverter pDelegate) {
    delegate = pDelegate;
  }

  public BigInteger getOffset(CFieldReference pField, CFAEdge pEdge)
      throws UnrecognizedCodeException {
    fieldName = pField.getFieldName();
    offset = BigInteger.ZERO;
    edge = pEdge;

    CExpression owner = pField.getFieldOwner();
    CType type = owner.getExpressionType();
    if (pField.isPointerDereference()) {
      if(type instanceof CTypedefType) {
        type = ((CTypedefType) type).getRealType();
      }
      if (type instanceof CPointerType) {
        type = ((CPointerType) type).getType();
      }
    }
    type.accept(this);
    return offset;
  }

  @Override
  public Void visit(CArrayType pArrayType) throws UnrecognizedCodeException {
    offset = offset.add(BigInteger.valueOf(delegate.getSizeof(pArrayType)));
    return null;
  }

  @Override
  public Void visit(CCompositeType pCompositeType) throws UnrecognizedCodeException {
    for (CCompositeTypeMemberDeclaration type : pCompositeType.getMembers()) {
      if (type.getName().equals(fieldName)) {
        return null;
      }
      type.getType().accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CElaboratedType pElaboratedType) throws UnrecognizedCodeException {
    return pElaboratedType.getRealType().accept(this);
  }

  @Override
  public Void visit(CEnumType pEnumType) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("CEnumTypes are not implemented yet.", edge);
  }

  @Override
  public Void visit(CFunctionType pFunctionType) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("FunctionTypes are not implemented yet.", edge);
  }

  @Override
  public Void visit(CPointerType pPointerType) throws UnrecognizedCodeException {
    offset = offset.add(BigInteger.valueOf(delegate.getSizeof(pPointerType)));
    return null;
  }

  @Override
  public Void visit(CProblemType pProblemType) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("CProblemTypes are not implemented yet.", edge);
  }

  @Override
  public Void visit(CSimpleType pSimpleType) throws UnrecognizedCodeException {
    offset = offset.add(BigInteger.valueOf(delegate.getSizeof(pSimpleType)));
    return null;
  }

  @Override
  public Void visit(CTypedefType pTypedefType) throws UnrecognizedCodeException {
    pTypedefType.getRealType().accept(this);
    return null;
  }

  @Override
  public Void visit(CVoidType pVoidType) throws UnrecognizedCodeException {
    return null;
  }

  @Override
  public Void visit(CBitFieldType pCBitFieldType) throws UnrecognizedCodeException {
    throw new UnrecognizedCodeException("CBitFieldTypes are not implemented yet.", edge);
  }


}