// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types;

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

class BaseAlignofVisitor implements CTypeVisitor<Integer, IllegalArgumentException> {
  private final MachineModel model;

  BaseAlignofVisitor(MachineModel model) {
    this.model = model;
  }

  @Override
  public Integer visit(CArrayType pArrayType) throws IllegalArgumentException {
    // the alignment of an array is the same as the alignment of an member of the array
    return pArrayType.getType().accept(this);
  }

  @Override
  public Integer visit(CCompositeType pCompositeType) throws IllegalArgumentException {

    switch (pCompositeType.getKind()) {
      case STRUCT:
      case UNION:
        int alignof = 1;
        // TODO: Take possible padding into account
        for (CCompositeTypeMemberDeclaration decl : pCompositeType.getMembers()) {
          int alignOfType = decl.getType().accept(this);
          alignof = Math.max(alignof, alignOfType);
        }
        return alignof;

      case ENUM: // There is no such kind of Composite Type.
      default:
        throw new AssertionError();
    }
  }

  @Override
  public Integer visit(CElaboratedType pElaboratedType) throws IllegalArgumentException {
    CType def = pElaboratedType.getRealType();
    if (def != null) {
      return def.accept(this);
    }

    if (pElaboratedType.getKind() == ComplexTypeKind.ENUM) {
      return model.getAlignofInt();
    }

    throw new IllegalArgumentException(
        "Cannot compute alignment of incomplete type " + pElaboratedType);
  }

  @Override
  public Integer visit(CEnumType pEnumType) throws IllegalArgumentException {
    // enums are always ints
    return model.getAlignofInt();
  }

  @Override
  public Integer visit(CFunctionType pFunctionType) throws IllegalArgumentException {
    // function types have per definition the value 1 if compiled with gcc
    return 1;
  }

  @Override
  public Integer visit(CPointerType pPointerType) throws IllegalArgumentException {
    return model.getAlignofPtr();
  }

  @Override
  public Integer visit(CProblemType pProblemType) throws IllegalArgumentException {
    throw new IllegalArgumentException("Unknown C-Type: " + pProblemType.getClass());
  }

  @Override
  public Integer visit(CSimpleType pSimpleType) throws IllegalArgumentException {
    switch (pSimpleType.getType()) {
      case BOOL:
        return model.getAlignofBool();
      case CHAR:
        return model.getAlignofChar();
      case FLOAT:
        return model.getAlignofFloat();
      case UNSPECIFIED: // unspecified is the same as int
      case INT:
        if (pSimpleType.hasLongLongSpecifier()) {
          return model.getAlignofLongLongInt();
        } else if (pSimpleType.hasLongSpecifier()) {
          return model.getAlignofLongInt();
        } else if (pSimpleType.hasShortSpecifier()) {
          return model.getAlignofShortInt();
        } else {
          return model.getAlignofInt();
        }
      case INT128:
        return model.getAlignofInt128();
      case DOUBLE:
        if (pSimpleType.hasLongSpecifier()) {
          return model.getAlignofLongDouble();
        } else {
          return model.getAlignofDouble();
        }
      case FLOAT128:
        return model.getAlignofFloat128();
      default:
        throw new AssertionError("Unrecognized CBasicType " + pSimpleType.getType());
    }
  }

  @Override
  public Integer visit(CTypedefType pTypedefType) throws IllegalArgumentException {
    return pTypedefType.getRealType().accept(this);
  }

  @Override
  public Integer visit(CVoidType pVoidType) throws IllegalArgumentException {
    return model.getAlignofVoid();
  }

  @Override
  public Integer visit(CBitFieldType pCBitFieldType) throws IllegalArgumentException {
    return pCBitFieldType.getType().accept(this);
  }
}
