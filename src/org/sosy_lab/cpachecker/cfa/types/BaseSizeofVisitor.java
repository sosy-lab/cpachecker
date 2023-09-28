// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
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

public class BaseSizeofVisitor implements CTypeVisitor<BigInteger, IllegalArgumentException> {
  private final MachineModel model;

  protected BaseSizeofVisitor(MachineModel model) {
    this.model = model;
  }

  @Override
  public BigInteger visit(CArrayType pArrayType) throws IllegalArgumentException {
    // TODO: Take possible padding into account

    CExpression arrayLength = pArrayType.getLength();

    if (arrayLength instanceof CIntegerLiteralExpression) {
      BigInteger length = ((CIntegerLiteralExpression) arrayLength).getValue();

      BigInteger sizeOfType = model.getSizeof(pArrayType.getType());
      return length.multiply(sizeOfType);
    }

    // Treat arrays with variable length as pointer.
    return BigInteger.valueOf(model.getSizeofPtr());
  }

  @Override
  public BigInteger visit(CCompositeType pCompositeType) throws IllegalArgumentException {

    switch (pCompositeType.getKind()) {
      case STRUCT:
        return handleSizeOfStruct(pCompositeType);
      case UNION:
        return handleSizeOfUnion(pCompositeType);
      case ENUM: // There is no such kind of Composit Type.
      default:
        throw new AssertionError();
    }
  }

  BigInteger calculateByteSize(BigInteger pBitFieldsSize) {
    if (pBitFieldsSize.compareTo(BigInteger.ZERO) == 0) {
      return BigInteger.ZERO;
    }

    BigInteger charSizeInBits = BigInteger.valueOf(model.getSizeofCharInBits());
    BigInteger result = pBitFieldsSize.divide(charSizeInBits);
    if (pBitFieldsSize.mod(charSizeInBits).compareTo(BigInteger.ZERO) > 0) {
      result = result.add(BigInteger.ONE);
    }
    return result;
  }

  private BigInteger handleSizeOfStruct(CCompositeType pCompositeType) {
    return model.getFieldOffsetOrSizeOrFieldOffsetsMappedInBits(pCompositeType, null, null);
  }

  private BigInteger handleSizeOfUnion(CCompositeType pCompositeType) {
    BigInteger size = BigInteger.ZERO;
    BigInteger sizeOfType = BigInteger.ZERO;
    // TODO: Take possible padding into account
    for (CCompositeTypeMemberDeclaration decl : pCompositeType.getMembers()) {
      sizeOfType = decl.getType().accept(this);
      size = size.max(sizeOfType);
    }
    return size;
  }

  @Override
  public BigInteger visit(CElaboratedType pElaboratedType) throws IllegalArgumentException {
    CType def = pElaboratedType.getRealType();
    if (def != null) {
      return def.accept(this);
    }

    if (pElaboratedType.getKind() == ComplexTypeKind.ENUM) {
      return BigInteger.valueOf(model.getSizeofInt());
    }

    throw new IllegalArgumentException("Cannot compute size of incomplete type " + pElaboratedType);
  }

  @Override
  public BigInteger visit(CEnumType pEnumType) throws IllegalArgumentException {
    return BigInteger.valueOf(model.getSizeof(pEnumType.getCompatibleType()));
  }

  @Override
  public BigInteger visit(CFunctionType pFunctionType) throws IllegalArgumentException {
    // A function does not really have a size,
    // but references to functions can be used as pointers.
    return BigInteger.valueOf(model.getSizeofPtr());
  }

  @Override
  public BigInteger visit(CPointerType pPointerType) throws IllegalArgumentException {
    return BigInteger.valueOf(model.getSizeofPtr());
  }

  @Override
  public BigInteger visit(CProblemType pProblemType) throws IllegalArgumentException {
    throw new IllegalArgumentException("Unknown C-Type: " + pProblemType.getClass());
  }

  @Override
  public BigInteger visit(CSimpleType pSimpleType) throws IllegalArgumentException {
    return BigInteger.valueOf(model.getSizeof(pSimpleType));
  }

  @Override
  public BigInteger visit(CTypedefType pTypedefType) throws IllegalArgumentException {
    return pTypedefType.getRealType().accept(this);
  }

  @Override
  public BigInteger visit(CVoidType pVoidType) throws IllegalArgumentException {
    return BigInteger.valueOf(model.getSizeofVoid());
  }

  @Override
  public BigInteger visit(CBitFieldType pCBitFieldType) throws IllegalArgumentException {
    return calculateByteSize(BigInteger.valueOf(pCBitFieldType.getBitFieldSize()));
  }
}
