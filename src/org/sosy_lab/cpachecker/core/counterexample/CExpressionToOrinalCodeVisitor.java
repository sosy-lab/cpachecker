/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.counterexample;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CExpressionToOrinalCodeVisitor
    implements CExpressionVisitor<String, RuntimeException> {

  public static final CExpressionToOrinalCodeVisitor BASIC_TRANSFORMER =
      new CExpressionToOrinalCodeVisitor();

  private final Map<CExpression, String> subsitution;

  private CExpressionToOrinalCodeVisitor() {
    this(Collections.emptyMap());
  }

  private CExpressionToOrinalCodeVisitor(Map<CExpression, String> pSubsitution) {
    subsitution = ImmutableMap.copyOf(pSubsitution);
  }

  public CExpressionToOrinalCodeVisitor substitute(CExpression pExpression, String pSubsitute) {
    if (pSubsitute.equals(subsitution.get(Objects.requireNonNull(pExpression)))) {
      return this;
    }
    ImmutableMap.Builder<CExpression, String> builder = ImmutableMap.builder();
    for (Map.Entry<CExpression, String> entry : subsitution.entrySet()) {
      if (!entry.getKey().equals(pExpression)) {
        builder.put(entry.getKey(), entry.getValue());
      }
    }
    builder.put(pExpression, pSubsitute);
    return new CExpressionToOrinalCodeVisitor(builder.build());
  }

  @Override
  public String visit(CArraySubscriptExpression pArraySubscriptExpression) {
    String substitute = subsitution.get(pArraySubscriptExpression);
    if (substitute != null) {
      return substitute;
    }
    CExpression arrayExpression = pArraySubscriptExpression.getArrayExpression();
    final String left;
    if (arrayExpression instanceof AArraySubscriptExpression) {
      left = arrayExpression.accept(this);
    } else {
      left = parenthesize(arrayExpression);
    }
    CExpression subscriptExpression = pArraySubscriptExpression.getSubscriptExpression();
    return left + "[" + subscriptExpression.accept(this) + "]";
  }

  @Override
  public String visit(CFieldReference pFieldReference) {
    String substitute = subsitution.get(pFieldReference);
    if (substitute != null) {
      return substitute;
    }
    final String left;
    if (pFieldReference.getFieldOwner() instanceof CFieldReference) {
      left = pFieldReference.getFieldOwner().accept(this);
    } else {
      left = parenthesize(pFieldReference.getFieldOwner());
    }
    String op = pFieldReference.isPointerDereference() ? "->" : ".";
    return left + op + pFieldReference.getFieldName();
  }

  @Override
  public String visit(CIdExpression pIdExpression) {
    String substitute = subsitution.get(pIdExpression);
    if (substitute != null) {
      return substitute;
    }
    return pIdExpression.getDeclaration().getOrigName();
  }

  @Override
  public String visit(CPointerExpression pPointerExpression) {
    String substitute = subsitution.get(pPointerExpression);
    if (substitute != null) {
      return substitute;
    }
    return "*" + parenthesize(pPointerExpression.getOperand().accept(this));
  }

  @Override
  public String visit(CComplexCastExpression pComplexCastExpression) {
    String substitute = subsitution.get(pComplexCastExpression);
    if (substitute != null) {
      return substitute;
    }
    String operand = pComplexCastExpression.getOperand().accept(this);
    if (pComplexCastExpression.isRealCast()) {
      return "__real__ " + operand;
    }
    return "__imag__ " + operand;
  }

  @Override
  public String visit(CBinaryExpression pIastBinaryExpression) {
    String substitute = subsitution.get(pIastBinaryExpression);
    if (substitute != null) {
      return substitute;
    }
    return parenthesize(pIastBinaryExpression.getOperand1())
        + " "
        + pIastBinaryExpression.getOperator().getOperator()
        + " "
        + parenthesize(pIastBinaryExpression.getOperand2());
  }

  @Override
  public String visit(CCastExpression pCastExpression) {
    String substitute = subsitution.get(pCastExpression);
    if (substitute != null) {
      return substitute;
    }
    CType type = pCastExpression.getExpressionType();
    final String typeCode = type.toASTString("");
    return parenthesize(typeCode) + parenthesize(pCastExpression.getOperand());
  }

  @Override
  public String visit(CCharLiteralExpression pCharLiteralExpression) {
    String substitute = subsitution.get(pCharLiteralExpression);
    if (substitute != null) {
      return substitute;
    }
    char c = pCharLiteralExpression.getCharacter();
    if (c >= ' ' && c < 128) {
      return "'" + c + "'";
    }
    return "'\\x" + Integer.toHexString(c) + "'";
  }

  @Override
  public String visit(CFloatLiteralExpression pFloatLiteralExpression) {
    String substitute = subsitution.get(pFloatLiteralExpression);
    if (substitute != null) {
      return substitute;
    }
    return pFloatLiteralExpression.getValue().toString();
  }

  @Override
  public String visit(CIntegerLiteralExpression pIntegerLiteralExpression) {
    String substitute = subsitution.get(pIntegerLiteralExpression);
    if (substitute != null) {
      return substitute;
    }

    String suffix = "";

    CType cType = pIntegerLiteralExpression.getExpressionType();
    if (cType instanceof CSimpleType) {
      CSimpleType type = (CSimpleType) cType;
      if (type.isUnsigned()) {
        suffix += "U";
      }
      if (type.isLong()) {
        suffix += "L";
      } else if (type.isLongLong()) {
        suffix += "LL";
      }
    }

    return pIntegerLiteralExpression.getValue().toString() + suffix;
  }

  @Override
  public String visit(CStringLiteralExpression pStringLiteralExpression) {
    String substitute = subsitution.get(pStringLiteralExpression);
    if (substitute != null) {
      return substitute;
    }
    // Includes quotation marks
    return pStringLiteralExpression.getValue();
  }

  @Override
  public String visit(CTypeIdExpression pTypeIdExpression) {
    String substitute = subsitution.get(pTypeIdExpression);
    if (substitute != null) {
      return substitute;
    }
    return pTypeIdExpression.getOperator().getOperator()
        + parenthesize(pTypeIdExpression.getType().getCanonicalType().toASTString(""));
  }

  @Override
  public String visit(CUnaryExpression pUnaryExpression) {
    String substitute = subsitution.get(pUnaryExpression);
    if (substitute != null) {
      return substitute;
    }
    UnaryOperator operator = pUnaryExpression.getOperator();
    if (operator == UnaryOperator.SIZEOF) {
      return operator.getOperator() + parenthesize(pUnaryExpression.getOperand().accept(this));
    }
    return operator.getOperator() + parenthesize(pUnaryExpression.getOperand());
  }

  @Override
  public String visit(CImaginaryLiteralExpression pLiteralExpression) {
    String substitute = subsitution.get(pLiteralExpression);
    if (substitute != null) {
      return substitute;
    }
    return pLiteralExpression.getValue().toString() + "i";
  }

  @Override
  public String visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
    String substitute = subsitution.get(pAddressOfLabelExpression);
    if (substitute != null) {
      return substitute;
    }
    return pAddressOfLabelExpression.toASTString();
  }

  public String parenthesize(String pInput) {
    return "(" + pInput + ")";
  }

  public String parenthesize(CExpression pInput) {
    String result = pInput.accept(this);
    if (pInput instanceof CIdExpression) {
      return result;
    }
    return parenthesize(result);
  }
}