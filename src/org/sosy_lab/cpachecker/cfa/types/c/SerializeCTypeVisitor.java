// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.cfa.types.c;

import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;

public class SerializeCTypeVisitor implements CTypeVisitor<String, RuntimeException> {

  @Override
  public String visit(CArrayType pArrayType) {
    return "ArrayType("
        + pArrayType.isConst()
        + ", "
        + pArrayType.isVolatile()
        + ", "
        + pArrayType.getType().accept(this)
        + ")";
  }

  @Override
  public String visit(CPointerType pPointerType) {
    return "PointerType("
        + pPointerType.isConst()
        + ", "
        + pPointerType.isVolatile()
        + ", "
        + pPointerType.getType().accept(this)
        + ")";
  }

  @Override
  public String visit(CFunctionType pFunctionType) {
    StringBuilder parameters = new StringBuilder();
    for (CType param : pFunctionType.getParameters()) {
      parameters.append(param.accept(this)).append(", ");
    }
    if (parameters.length() > 0) {
      parameters.setLength(parameters.length() - 2);
    }
    return "FunctionType("
        + pFunctionType.getReturnType().accept(this)
        + ", ["
        + parameters
        + "], "
        + pFunctionType.takesVarArgs()
        + ")";
  }

  @Override
  public String visit(CSimpleType pSimpleType) {
    return "SimpleType("
        + pSimpleType.isConst()
        + ", "
        + pSimpleType.isVolatile()
        + ", "
        + pSimpleType.getType()
        + ", "
        + pSimpleType.hasLongSpecifier()
        + ", "
        + pSimpleType.hasShortSpecifier()
        + ", "
        + pSimpleType.hasSignedSpecifier()
        + ", "
        + pSimpleType.hasUnsignedSpecifier()
        + ", "
        + pSimpleType.hasComplexSpecifier()
        + ", "
        + pSimpleType.hasImaginarySpecifier()
        + ", "
        + pSimpleType.hasLongLongSpecifier()
        + ")";
  }

  @Override
  public String visit(CCompositeType pCompositeType) {
    StringBuilder result = new StringBuilder();

    result
        .append("CompositeType(")
        .append(pCompositeType.isConst())
        .append(", ")
        .append(pCompositeType.isVolatile())
        .append(", ")
        .append(pCompositeType.getKind())
        .append(", ")
        .append(pCompositeType.getName())
        .append(", ")
        .append(pCompositeType.getOrigName());

    if (pCompositeType.getMembers() != null && !pCompositeType.getMembers().isEmpty()) {
      result.append(", [");
      for (CCompositeType.CCompositeTypeMemberDeclaration member : pCompositeType.getMembers()) {
        result
            .append(member.getType().accept(this))
            .append(":")
            .append(member.getName())
            .append(", ");
      }
      if (result.length() > 0) {
        result.setLength(result.length() - 2);
      }
      result.append("]");
    } else {
      result.append(", null");
    }

    result.append(")");

    return result.toString();
  }

  @Override
  public String visit(CProblemType pProblemType) {
    return "ProblemType(" + pProblemType.toString() + ")";
  }

  @Override
  public String visit(CTypedefType pTypedefType) {
    return "TypedefType("
        + pTypedefType.isConst()
        + ", "
        + pTypedefType.isVolatile()
        + ", "
        + pTypedefType.getName()
        + ", "
        + pTypedefType.getCanonicalType().accept(this)
        + ")";
  }

  @Override
  public String visit(CVoidType pVoidType) {
    String result = "VoidType(" + pVoidType.isConst() + ", " + pVoidType.isVolatile() + ")";
    return result;
  }

  @Override
  public String visit(CBitFieldType pBitFieldType) {
    return "BitFieldType("
        + pBitFieldType.getBitFieldSize()
        + ", "
        + pBitFieldType.getType().accept(this)
        + ")";
  }

  @Override
  public String visit(CElaboratedType pElaboratedType) {
    StringBuilder serializedElaboratedType = new StringBuilder();
    serializedElaboratedType
        .append("ElaboratedType(")
        .append(pElaboratedType.isConst())
        .append(", ")
        .append(pElaboratedType.isVolatile())
        .append(", ")
        .append(pElaboratedType.getKind())
        .append(", ")
        .append(pElaboratedType.getName())
        .append(", ")
        .append(pElaboratedType.getOrigName());

    CComplexType realType = pElaboratedType.getRealType();
    if (realType != null) {
      serializedElaboratedType.append(", ").append(realType.accept(this));
    } else {
      serializedElaboratedType.append(", unknownType");
    }

    serializedElaboratedType.append(")");
    return serializedElaboratedType.toString();
  }

  @Override
  public String visit(CEnumType pEnumType) {
    StringBuilder enumerators = new StringBuilder();
    for (CEnumerator enumerator : pEnumType.getEnumerators()) {
      enumerators.append(enumerator.toASTString()).append(", ");
    }
    String originName = pEnumType.getOrigName();
    if (originName == null || originName.isEmpty()) {
      originName = "null";
    }
    if (enumerators.length() > 0) {
      enumerators.setLength(enumerators.length() - 2);
    }
    return "EnumType("
        + pEnumType.isConst()
        + ","
        + pEnumType.isVolatile()
        + ","
        + pEnumType.getCompatibleType().accept(this)
        + ",["
        + enumerators
        + "]"
        + ","
        + pEnumType.getName()
        + ","
        + originName
        + ")";
  }
}
