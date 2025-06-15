// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;

public class SerializeCTypeVisitor implements CTypeVisitor<String, RuntimeException> {

  private static final String ARRAY_TYPE_PREFIX = "ArrayType";
  private static final String POINTER_TYPE_PREFIX = "PointerType";
  private static final String FUNCTION_TYPE_PREFIX = "FunctionType";
  private static final String SIMPLE_TYPE_PREFIX = "SimpleType";
  private static final String COMPOSITE_TYPE_PREFIX = "CompositeType";
  private static final String PROBLEM_TYPE_PREFIX = "ProblemType";
  private static final String TYPEDEF_TYPE_PREFIX = "TypedefType";
  private static final String VOID_TYPE_PREFIX = "VoidType";
  private static final String BITFIELD_TYPE_PREFIX = "BitFieldType";
  private static final String ELABORATED_TYPE_PREFIX = "ElaboratedType";
  private static final String ENUM_TYPE_PREFIX = "EnumType";

  @Override
  public String visit(CArrayType pArrayType) {
    String result =
        ARRAY_TYPE_PREFIX
            + "("
            + pArrayType.isConst()
            + ", "
            + pArrayType.isVolatile()
            + ", "
            + pArrayType.getType().accept(this)
            + ")";
    return result;
  }

  @Override
  public String visit(CPointerType pPointerType) {
    return POINTER_TYPE_PREFIX
        + "("
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

    return FUNCTION_TYPE_PREFIX
        + "("
        + pFunctionType.getReturnType().accept(this)
        + ", ["
        + parameters
        + "], "
        + pFunctionType.takesVarArgs()
        + ")";
  }

  @Override
  public String visit(CSimpleType pSimpleType) {
    return SIMPLE_TYPE_PREFIX
        + "("
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
        .append(COMPOSITE_TYPE_PREFIX)
        .append("(")
        .append(pCompositeType.isConst())
        .append(", ")
        .append(pCompositeType.isVolatile())
        .append(", ")
        .append(pCompositeType.getKind())
        .append(", ")
        .append(pCompositeType.getName())
        .append(", ")
        .append(pCompositeType.getOrigName())
        .append(")");

    return result.toString();
  }

  @Override
  public String visit(CProblemType pProblemType) {
    Objects.requireNonNull(pProblemType, "ProblemType must not be null");
    return PROBLEM_TYPE_PREFIX + "(" + pProblemType + ")";
  }

  @Override
  public String visit(CTypedefType pTypedefType) {
    return TYPEDEF_TYPE_PREFIX
        + "("
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
    String result =
        VOID_TYPE_PREFIX + "(" + pVoidType.isConst() + ", " + pVoidType.isVolatile() + ")";
    return result;
  }

  @Override
  public String visit(CBitFieldType pBitFieldType) {
    return BITFIELD_TYPE_PREFIX
        + "("
        + pBitFieldType.getBitFieldSize()
        + ", "
        + pBitFieldType.getType().accept(this)
        + ")";
  }

  @Override
  public String visit(CElaboratedType pElaboratedType) {
    return ELABORATED_TYPE_PREFIX
        + "("
        + pElaboratedType.isConst()
        + ", "
        + pElaboratedType.isVolatile()
        + ", "
        + pElaboratedType.getKind()
        + ", "
        + pElaboratedType.getName()
        + ", "
        + pElaboratedType.getOrigName()
        + ")";
  }

  @Override
  public String visit(CEnumType pEnumType) {
    StringBuilder enumerators = new StringBuilder();
    for (CEnumerator enumerator : pEnumType.getEnumerators()) {
      enumerators.append(enumerator.toASTString()).append(", ");
    }
    String originName = pEnumType.getOrigName();
    if (isNullOrEmpty(originName)) {
      originName = "null";
    }
    if (enumerators.length() > 0) {
      enumerators.setLength(enumerators.length() - 2);
    }

    String result =
        ENUM_TYPE_PREFIX
            + "("
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

    return result;
  }
}
