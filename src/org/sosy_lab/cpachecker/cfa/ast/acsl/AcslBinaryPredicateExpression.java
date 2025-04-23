// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslBinaryPredicateExpression extends ABinaryExpression
    implements AcslExpression {

  @Serial private static final long serialVersionUID = 7019956121956900L;

  public AcslBinaryPredicateExpression(
      FileLocation pFileLocation,
      AcslType pType,
      AcslExpression pOperand1,
      AcslExpression pOperand2,
      AcslBinaryPredicateExpressionOperator pOperator) {
    super(pFileLocation, pType, pOperand1, pOperand2, pOperator);
  }

  @Override
  public <R, X extends Exception> R accept(AcslExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  public enum AcslBinaryPredicateExpressionOperator implements ABinaryOperator, AcslAstNode {
    IMPLICATION("==>"),
    EQUIVALENT("<==>"),
    AND("&&"),
    OR("||"),
    EQUALS("=="),
    NOT_EQUALS("!="),
    LESS_EQUAL("<="),
    GREATER_EQUAL(">="),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    ;

    @Serial private static final long serialVersionUID = 701123361956900L;

    private final String operator;
    private final FileLocation fileLocation;

    AcslBinaryPredicateExpressionOperator(String pOperator) {
      operator = pOperator;
      fileLocation = FileLocation.DUMMY;
    }

    public static AcslBinaryPredicateExpressionOperator of(String pOperator) {
      for (AcslBinaryPredicateExpressionOperator op : values()) {
        if (op.operator.equals(pOperator)) {
          return op;
        }
      }
      throw new IllegalArgumentException("Unknown operator: " + pOperator);
    }

    @Override
    public String toString() {
      return operator;
    }

    /** Returns the string representation of this operator (e.g. "*", "+"). */
    @Override
    public String getOperator() {
      return operator;
    }

    @Override
    public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
      return v.visit(this);
    }

    @Override
    public FileLocation getFileLocation() {
      return fileLocation;
    }

    @Override
    public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
      return toString();
    }

    @Override
    public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
      return toString();
    }
  }
}
