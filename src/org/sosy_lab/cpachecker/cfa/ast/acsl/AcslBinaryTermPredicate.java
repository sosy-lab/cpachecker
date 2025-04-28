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
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression.ABinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class AcslBinaryTermPredicate implements AcslPredicate {
  private AcslTerm operand1;
  private AcslTerm operand2;
  private AcslBinaryTermExpressionOperator operator;
  @Serial private static final long serialVersionUID = 8144911237675011353L;
  private final AcslType type;
  private final FileLocation location;

  public AcslBinaryTermPredicate(
      FileLocation pFileLocation,
      AcslType pType,
      AcslTerm pOperand1,
      AcslTerm pOperand2,
      AcslBinaryTermExpressionOperator pOperator) {
    checkNotNull(pFileLocation);
    checkNotNull(pType);
    checkNotNull(pOperand1);
    checkNotNull(pOperand2);
    checkNotNull(pOperator);
    operand1 = pOperand1;
    operand2 = pOperand2;
    operator = pOperator;
    type = pType;
    location = pFileLocation;
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
  public Type getExpressionType() {
    return type;
  }

  @Override
  public FileLocation getFileLocation() {
    return location;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toParenthesizedASTString(pAAstNodeRepresentation);
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "(("
        + operand1.toASTString(pAAstNodeRepresentation)
        + ")"
        + " "
        + operator.toString()
        + " "
        + "("
        + operand2.toASTString(pAAstNodeRepresentation)
        + "))";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 8;
    result = prime * result + Objects.hashCode(operand1);
    result = prime * result + Objects.hashCode(operand2);
    result = prime * result + Objects.hashCode(operator);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslBinaryTermPredicate other
        && Objects.equals(other.location, location)
        && Objects.equals(other.type, type)
        && Objects.equals(other.operand1, operand1)
        && Objects.equals(other.operand2, operand2)
        && Objects.equals(other.operator, operator);
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
