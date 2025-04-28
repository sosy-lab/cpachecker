// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression.ABinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslBinaryTermPredicate extends ABinaryExpression implements AcslPredicate {

  @Serial private static final long serialVersionUID = 8144911237675011353L;

  public AcslBinaryTermPredicate(
      FileLocation pFileLocation,
      AcslType pType,
      AcslTerm pOperand1,
      AcslTerm pOperand2,
      AcslBinaryTermExpressionOperator pOperator) {
    super(pFileLocation, pType, pOperand1, pOperand2, pOperator);
    checkNotNull(pFileLocation);
    checkNotNull(pType);
    checkNotNull(pOperand1);
    checkNotNull(pOperand2);
    checkNotNull(pOperator);
  }

  @Override
  public <R, X extends Exception> R accept(AcslPredicateVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public AcslTerm getOperand1() {
    return (AcslTerm) super.getOperand1();
  }

  @Override
  public AcslTerm getOperand2() {
    return (AcslTerm) super.getOperand2();
  }

  @Override
  public AcslBinaryTermExpressionOperator getOperator() {
    return (AcslBinaryTermExpressionOperator) super.getOperator();
  }

  @Override
  public AcslType getExpressionType() {
    return (AcslType) super.getExpressionType();
  }

  public enum AcslBinaryTermExpressionOperator implements ABinaryOperator, AcslAstNode {
    EQUALS("=="),
    NOT_EQUALS("!="),
    LESS_EQUAL("<="),
    GREATER_EQUAL(">="),
    LESS_THAN("<"),
    GREATER_THAN(">"),
    ;

    @Serial private static final long serialVersionUID = 70136361956900L;

    private final String operator;
    private final FileLocation fileLocation;

    AcslBinaryTermExpressionOperator(String pOperator) {
      operator = pOperator;
      fileLocation = FileLocation.DUMMY;
    }

    public static AcslBinaryTermExpressionOperator of(String pOperator) {
      for (AcslBinaryTermExpressionOperator op : values()) {
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
