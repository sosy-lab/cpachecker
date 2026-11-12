// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl.antlr;

import com.google.common.base.Verify;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslCVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryTerm.AcslUnaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.acsl.antlr.generated.AcslGrammarBaseVisitor;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslCType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslLogicType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslPointerType;
import org.sosy_lab.cpachecker.cfa.types.acsl.AcslType;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

abstract class AbstractAntlrToAstConverter<T> extends AcslGrammarBaseVisitor<T> {

  private final CProgramScope cProgramScope;
  private final AcslScope acslScope;
  private final FileLocation initialFileLocation;

  protected AbstractAntlrToAstConverter(
      CProgramScope pCProgramScope, AcslScope pAcslScope, FileLocation pInitialFileLocation) {
    cProgramScope = pCProgramScope;
    acslScope = pAcslScope;
    initialFileLocation = pInitialFileLocation;
  }

  public AcslScope getAcslScope() {
    return acslScope;
  }

  protected FileLocation getInitialFileLocation() {
    return initialFileLocation;
  }

  public CProgramScope getCProgramScope() {
    return cProgramScope;
  }

  /**
   * Returns the location of the given context in the program. The parsed text must have the same
   * offsets as the comment that starts at the initial file location.
   */
  protected FileLocation fileLocationFromContext(ParserRuleContext pContext) {
    if (!initialFileLocation.isRealLocation()) {
      return initialFileLocation;
    }

    Token start = pContext.getStart();
    Token stop = pContext.getStop();
    int startOffset = start.getStartIndex();
    int endOffset = stop.getStopIndex() + 1;
    // Lines of tokens start at 1, columns at 0, and only the first line of the comment is indented
    int startLine = start.getLine() - 1;
    int endLine = stop.getLine() - 1;
    int startColumn =
        (startLine == 0 ? initialFileLocation.getStartColumnInLine() : 1)
            + start.getCharPositionInLine();
    int endColumn =
        (endLine == 0 ? initialFileLocation.getStartColumnInLine() : 1)
            + stop.getCharPositionInLine()
            + stop.getStopIndex()
            - stop.getStartIndex()
            + 1;

    return new FileLocation(
        initialFileLocation.getFileName(),
        initialFileLocation.getNiceFileName(),
        initialFileLocation.getNodeOffset() + startOffset,
        endOffset - startOffset,
        initialFileLocation.getStartingLineNumber() + startLine,
        initialFileLocation.getStartingLineNumber() + endLine,
        startColumn,
        endColumn,
        initialFileLocation.getStartingLineInOrigin() + startLine,
        initialFileLocation.getStartingLineInOrigin() + endLine,
        initialFileLocation.isOffsetRelatedToOrigin());
  }

  AcslSimpleDeclaration getVariableDeclarationForName(String pName) {
    @Nullable CSimpleDeclaration cVariableDeclaration = cProgramScope.lookupVariable(pName);
    @Nullable AcslSimpleDeclaration acslVariableDeclaration = acslScope.lookupVariable(pName);
    if (cVariableDeclaration != null && acslVariableDeclaration == null) {
      if (cVariableDeclaration instanceof CVariableDeclaration var) {
        return new AcslCVariableDeclaration(var);
      } else if (cVariableDeclaration instanceof CParameterDeclaration var) {
        return new AcslCVariableDeclaration(var.asVariableDeclaration());
      } else {
        throw new AcslParseRuntimeException(
            "Expected a C variable declaration, but got: " + cVariableDeclaration);
      }
    } else if (acslVariableDeclaration != null && cVariableDeclaration == null) {
      return acslVariableDeclaration;
    } else if (cVariableDeclaration != null && acslVariableDeclaration != null) {
      throw new AcslParseRuntimeException(
          "Variable " + pName + " is declared in both the Acsl and the C scope");
    }

    throw new AcslParseRuntimeException(
        "Variable " + pName + " is not declared in neither the C program nor the ACSL scope.");
  }

  AcslFunctionDeclaration getFunctionDeclarationForName(String pName) {
    AcslFunctionDeclaration declaration = acslScope.lookupFunction(pName);
    if (declaration == null) {
      throw new AcslParseRuntimeException("Logic function " + pName + " is not declared");
    }
    return declaration;
  }

  AcslPredicateDeclaration getPredicateDeclarationForName(String pName) {
    AcslPredicateDeclaration declaration = acslScope.lookupPredicate(pName);
    if (declaration == null) {
      throw new AcslParseRuntimeException("Predicate " + pName + " is not declared");
    }
    return declaration;
  }

  AcslType getTypeForName(String pName) {
    AcslType type = acslScope.lookupType(pName);
    if (type == null) {
      throw new AcslParseRuntimeException("Type " + pName + " is not declared");
    }
    return type;
  }

  AcslType findResultTypeAfterUnaryOperation(
      AcslUnaryTermOperator operator, AcslType expressionType) {
    // TODO: Look at this function `private CAstNode convert(final IASTUnaryExpression e) {` in the
    //  class ASTConverter to see if this code can be improved/stuff from there can be reused
    if (expressionType instanceof AcslCType pCType) {
      CType cType = pCType.getType();
      if (operator == AcslUnaryTermOperator.ADDRESS_OF) {
        return new AcslCType(new CPointerType(cType.getQualifiers(), cType));
      } else if (cType instanceof CArrayType pArrayType
          && operator == AcslUnaryTermOperator.POINTER_DEREFERENCE) {
        return new AcslCType(pArrayType.getType());
      } else if (cType instanceof CPointerType pPointerType
          && operator == AcslUnaryTermOperator.POINTER_DEREFERENCE) {
        return new AcslCType(pPointerType.getType());
      } else if (cType instanceof CSimpleType pSimpleType
          && (operator == AcslUnaryTermOperator.MINUS || operator == AcslUnaryTermOperator.PLUS)) {
        return new AcslCType(pSimpleType);
      }
    } else if (expressionType instanceof AcslLogicType) {
      Verify.verify(
          operator == AcslUnaryTermOperator.MINUS
              || operator == AcslUnaryTermOperator.PLUS
              || operator == AcslUnaryTermOperator.NEGATION);
      return expressionType;
    } else if (expressionType instanceof AcslPointerType pPointerType) {
      Verify.verify(operator == AcslUnaryTermOperator.POINTER_DEREFERENCE);
      return pPointerType.getType();
    }
    throw new AcslParseRuntimeException(
        "Unary operator " + operator + " is not supported for type " + expressionType);
  }
}
