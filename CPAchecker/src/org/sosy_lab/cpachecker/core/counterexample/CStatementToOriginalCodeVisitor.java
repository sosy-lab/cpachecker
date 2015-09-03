/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.transform;

import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatorVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

/**
 * Like toASTString, but with original names.
 *
 * NOT necessarily equivalent to specific parts of the original code file.
 */
public enum CStatementToOriginalCodeVisitor implements CStatementVisitor<String, RuntimeException> {

  INSTANCE;

  @Override
  public String visit(CExpressionStatement pIastExpressionStatement) {
    return pIastExpressionStatement.getExpression().accept(ExpressionToOrinalCodeVisitor.VISITOR_INSTANCE) + ";";
  }

  @Override
  public String visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) {

    ExpressionToOrinalCodeVisitor expressionToOrinalCodeVisitor = ExpressionToOrinalCodeVisitor.VISITOR_INSTANCE;

    String leftHandSide = pIastExpressionAssignmentStatement.getLeftHandSide().accept(expressionToOrinalCodeVisitor);
    String rightHandSide = pIastExpressionAssignmentStatement.getRightHandSide().accept(expressionToOrinalCodeVisitor);

    return leftHandSide + " = " + rightHandSide + "; ";
  }

  @Override
  public String visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) {

    ExpressionToOrinalCodeVisitor expressionToOrinalCodeVisitor = ExpressionToOrinalCodeVisitor.VISITOR_INSTANCE;

    String leftHandSide = pIastFunctionCallAssignmentStatement.getLeftHandSide().accept(expressionToOrinalCodeVisitor);
    String rightHandSide = handleFunctionCallExpression(
        pIastFunctionCallAssignmentStatement.getFunctionCallExpression());

    return leftHandSide
        + " = "
        + rightHandSide
        + "; ";
  }

  @Override
  public String visit(CFunctionCallStatement pIastFunctionCallStatement) {
    return handleFunctionCallExpression(pIastFunctionCallStatement.getFunctionCallExpression()) + ";";
  }

  private static enum ExpressionToOrinalCodeVisitor implements CExpressionVisitor<String, RuntimeException> {

    VISITOR_INSTANCE;

    @Override
    public String visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
      CExpression arrayExpression = pIastArraySubscriptExpression.getArrayExpression();
      final String left;
      if (arrayExpression instanceof AArraySubscriptExpression) {
        left = arrayExpression.accept(this);
      } else {
        left = parenthesize(arrayExpression);
      }
      CExpression subscriptExpression = pIastArraySubscriptExpression.getSubscriptExpression();
      return left + "[" + subscriptExpression.accept(this) + "]";
    }

    @Override
    public String visit(CFieldReference pIastFieldReference) {
      final String left;
      if (pIastFieldReference.getFieldOwner() instanceof CFieldReference) {
        left = pIastFieldReference.getFieldOwner().accept(this);
      } else {
        left = parenthesize(pIastFieldReference.getFieldOwner());
      }
      String op = pIastFieldReference.isPointerDereference() ? "->" : ".";
      return left + op  + pIastFieldReference.getFieldName();
    }

    @Override
    public String visit(CIdExpression pIastIdExpression) {
      return pIastIdExpression.getDeclaration().getOrigName();
    }

    @Override
    public String visit(CPointerExpression pPointerExpression) {
      return "*" + parenthesize(pPointerExpression.getOperand().accept(this));
    }

    @Override
    public String visit(CComplexCastExpression pComplexCastExpression) {
      String operand = pComplexCastExpression.getOperand().accept(this);
      if (pComplexCastExpression.isRealCast()) {
        return "__real__ " + operand;
      }
      return "__imag__ " + operand;
    }

    @Override
    public String visit(CBinaryExpression pIastBinaryExpression) {
      return parenthesize(pIastBinaryExpression.getOperand1())
          + " "
          + pIastBinaryExpression.getOperator().getOperator()
          + " "
          + parenthesize(pIastBinaryExpression.getOperand2());
    }

    @Override
    public String visit(CCastExpression pIastCastExpression) {
      CType type = pIastCastExpression.getExpressionType();
      final String typeCode = type.toASTString("");
      return parenthesize(typeCode)
          + parenthesize(pIastCastExpression.getOperand());
    }

    @Override
    public String visit(CCharLiteralExpression pIastCharLiteralExpression) {
      char c = pIastCharLiteralExpression.getCharacter();
      if (c >= ' ' && c < 128) {
        return "'" + c + "'";
      }
      return "'\\x" + Integer.toHexString(c) + "'";
    }

    @Override
    public String visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
      return pIastFloatLiteralExpression.getValue().toString();
    }

    @Override
    public String visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
      String suffix = "";

      CType cType = pIastIntegerLiteralExpression.getExpressionType();
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

      return pIastIntegerLiteralExpression.getValue().toString() + suffix;
    }

    @Override
    public String visit(CStringLiteralExpression pIastStringLiteralExpression) {
      // Includes quotation marks
      return pIastStringLiteralExpression.getValue();
    }

    @Override
    public String visit(CTypeIdExpression pIastTypeIdExpression) {
      return pIastTypeIdExpression.getOperator().getOperator()
          + parenthesize(pIastTypeIdExpression.getType().getCanonicalType().toASTString(""));
    }

    @Override
    public String visit(CUnaryExpression pIastUnaryExpression) {
      UnaryOperator operator = pIastUnaryExpression.getOperator();
      if (operator == UnaryOperator.SIZEOF) {
        return operator.getOperator() + parenthesize(pIastUnaryExpression.getOperand().accept(this));
      }
      return operator.getOperator() + parenthesize(pIastUnaryExpression.getOperand());
    }

    @Override
    public String visit(CImaginaryLiteralExpression pIastLiteralExpression) {
      return pIastLiteralExpression.getValue().toString() + "i";
    }

    @Override
    public String visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
      return pAddressOfLabelExpression.toASTString();
    }
  }

  @SuppressWarnings("unused")
  private static enum CInitializerToOriginalCodeVisitor implements CInitializerVisitor<String, RuntimeException> {

    VISITOR_INSTANCE;

    public static final Function<CInitializer, String> TO_CODE = new Function<CInitializer, String>() {

      @Override
      public String apply(CInitializer pArg0) {
        return pArg0.accept(CInitializerToOriginalCodeVisitor.VISITOR_INSTANCE);
      }

    };

    @Override
    public String visit(CInitializerExpression pInitializerExpression) {
      return pInitializerExpression.getExpression().accept(ExpressionToOrinalCodeVisitor.VISITOR_INSTANCE);
    }

    @Override
    public String visit(CInitializerList pInitializerList) {
      StringBuilder code = new StringBuilder();

      code.append("{ ");
      Joiner.on(", ").appendTo(code, transform(pInitializerList.getInitializers(), TO_CODE));
      code.append(" }");

      return code.toString();
    }

    @Override
    public String visit(CDesignatedInitializer pCStructInitializerPart) {
      return from(pCStructInitializerPart.getDesignators()).transform(DesignatorToOriginalCodeVisitor.TO_CODE).join(Joiner.on(""))
          + " = " + pCStructInitializerPart.getRightHandSide().accept(this);
    }

  }

  private static enum DesignatorToOriginalCodeVisitor implements CDesignatorVisitor<String, RuntimeException> {

    VISITOR_INSTANCE;

    public static final Function<CDesignator, String> TO_CODE = new Function<CDesignator, String>() {

      @Override
      public String apply(CDesignator pArg0) {
        return pArg0.accept(DesignatorToOriginalCodeVisitor.VISITOR_INSTANCE);
      }

    };

    @Override
    public String visit(CArrayDesignator pArrayDesignator) {
      return "["
          + pArrayDesignator.getSubscriptExpression().accept(ExpressionToOrinalCodeVisitor.VISITOR_INSTANCE)
          + "]";
    }

    @Override
    public String visit(CArrayRangeDesignator pArrayRangeDesignator) {
      return "["
          + pArrayRangeDesignator.getFloorExpression().accept(ExpressionToOrinalCodeVisitor.VISITOR_INSTANCE)
          + " ... "
              + pArrayRangeDesignator.getCeilExpression().accept(ExpressionToOrinalCodeVisitor.VISITOR_INSTANCE)
          + "]";
    }

    @Override
    public String visit(CFieldDesignator pFieldDesignator) {
      return "."  + pFieldDesignator.getFieldName();
    }

  }

  private static String handleFunctionCallExpression(
      CFunctionCallExpression pFunctionCallExpression) {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(parenthesize(pFunctionCallExpression.getFunctionNameExpression()));
    lASTString.append("(");
    Joiner.on(", ").appendTo(lASTString, transform(pFunctionCallExpression.getParameterExpressions(), new Function<CExpression, String>() {

      @Override
      public String apply(CExpression pInput) {
        return pInput.accept(ExpressionToOrinalCodeVisitor.VISITOR_INSTANCE);
      }
    }));
    lASTString.append(")");

    return lASTString.toString();
  }

  private static String parenthesize(String pInput) {
    return "(" + pInput + ")";
  }

  private static String parenthesize(CExpression pInput) {
    String result = pInput.accept(ExpressionToOrinalCodeVisitor.VISITOR_INSTANCE);
    if (pInput instanceof CIdExpression) {
      return result;
    }
    return parenthesize(result);
  }

}
