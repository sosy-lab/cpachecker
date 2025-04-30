// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import com.google.common.base.Verify;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBuiltinLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCharLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPointerType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslRealLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.generated.AcslGrammarBaseVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

abstract class AntlrToInternalAbstractConverter<T> extends AcslGrammarBaseVisitor<T> {

  private final CProgramScope cProgramScope;
  private final AcslScope acslScope;

  protected AntlrToInternalAbstractConverter(CProgramScope pCProgramScope, AcslScope pAcslScope) {
    cProgramScope = pCProgramScope;
    acslScope = pAcslScope;
  }

  public AcslScope getAcslScope() {
    return acslScope;
  }

  public CProgramScope getCProgramScope() {
    return cProgramScope;
  }

  AcslSimpleDeclaration getVariableDeclarationForName(String pName) {
    @Nullable CSimpleDeclaration cVariableDeclaration = cProgramScope.lookupVariable(pName);
    @Nullable AcslSimpleDeclaration acslVariableDeclaration = acslScope.lookupVariable(pName);
    if (cVariableDeclaration != null && acslVariableDeclaration == null) {
      if (cVariableDeclaration instanceof CVariableDeclaration var) {
        return new AcslCVariableDeclaration(var);
      } else {
        throw new RuntimeException(
            "Expected a C variable declaration, but got: " + cVariableDeclaration);
      }
    } else if (acslVariableDeclaration != null && cVariableDeclaration == null) {
      return acslVariableDeclaration;
    } else if (cVariableDeclaration != null && acslVariableDeclaration != null) {
      throw new RuntimeException(
          "Variable " + pName + " is declared in both the Acsl and the C scope");
    }

    throw new RuntimeException(
        "Variable " + pName + " is not declared in neither the C program nor the ACSL scope.");
  }

  AcslTerm getConstantForString(String pStringValue) {
    // This is a hack around the problem that I cannot get ANTLR to correctly
    // give me the type of the C constant it parsed
    try {
      return new AcslIntegerLiteralTerm(
          FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, new BigInteger(pStringValue));
    } catch (NumberFormatException e) {
      try {
        // It is not an integer, so we try a float
        return new AcslRealLiteralTerm(
            FileLocation.DUMMY, AcslBuiltinLogicType.REAL, new BigDecimal(pStringValue));

      } catch (NumberFormatException e2) {
        // This is a character constant
        if (pStringValue.length() != 1) {
          throw new RuntimeException(
              "Character constant should be of length 1, but was: " + pStringValue);
        }
        return new AcslCharLiteralTerm(
            FileLocation.DUMMY, AcslBuiltinLogicType.INTEGER, pStringValue.charAt(0));
      }
    }
  }

  AcslType findResultTypeAfterUnaryOperation(
      AcslUnaryTermOperator operator, AcslType expressionType) {
    // Find out the correct type after the operation
    // TODO: Look at this function `private CAstNode convert(final IASTUnaryExpression e) {` in the
    //  class ASTConverter to see if this code can be improved/stuff from there can be reused
    AcslType resultType;
    if (expressionType instanceof AcslCType pCType) {
      CType cType = pCType.getType();
      if (operator == AcslUnaryTermOperator.ADDRESS_OF) {
        resultType = new AcslCType(new CPointerType(cType.isConst(), cType.isVolatile(), cType));
      } else if (cType instanceof CArrayType pArrayType) {
        if (operator == AcslUnaryTermOperator.POINTER_DEREFERENCE) {
          // We need to get the type of the element
          resultType = new AcslCType(pArrayType.getType());
        } else {
          throw new RuntimeException(
              "Expected a pointer dereference operator, but got: "
                  + operator
                  + " for type: "
                  + cType);
        }
      } else if (cType instanceof CPointerType pPointerType) {
        if (operator == AcslUnaryTermOperator.POINTER_DEREFERENCE) {
          // We need to get the type of the element
          resultType = new AcslCType(pPointerType.getType());
        } else {
          throw new RuntimeException(
              "Expected a pointer dereference operator, but got: "
                  + operator
                  + " for type: "
                  + cType);
        }
      } else if (cType instanceof CSimpleType pSimpleType) {
        if (operator == AcslUnaryTermOperator.MINUS || operator == AcslUnaryTermOperator.PLUS) {
          // We need to get the type of the element
          resultType = new AcslCType(pSimpleType);
        } else {
          throw new RuntimeException(
              "Expected a unary operator, but got: " + operator + " for type: " + cType);
        }
      } else {
        throw new RuntimeException(
            "Expected a unary operator, but got: " + operator + " for type: " + cType);
      }
    } else if (expressionType instanceof AcslLogicType) {
      // We are dealing with a logic type, so we just return the same type
      Verify.verify(
          operator == AcslUnaryTermOperator.MINUS
              || operator == AcslUnaryTermOperator.PLUS
              || operator == AcslUnaryTermOperator.NEGATION);
      resultType = expressionType;
    } else if (expressionType instanceof AcslPointerType pPointerType) {
      Verify.verify(operator == AcslUnaryTermOperator.POINTER_DEREFERENCE);
      resultType = pPointerType.getType();
    } else {
      throw new RuntimeException(
          "Expected a unary operator, but got: " + operator + " for type: " + expressionType);
    }
    return resultType;
  }
}
