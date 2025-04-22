// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression.AUnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class AcslUnaryTerm extends AcslTerm {

  @Serial private static final long serialVersionUID = 8458828043123590886L;
  private final AcslTerm operand;
  private final AcslUnaryTermOperator operator;

  public AcslUnaryTerm(
      FileLocation pFileLocation,
      AcslType pType,
      AcslTerm pOperand,
      AcslUnaryTermOperator pOperator) {
    super(pFileLocation, pType);
    operand = pOperand;
    operator = pOperator;
  }

  public AcslTerm getOperand() {
    return operand;
  }

  public AcslUnaryTermOperator getOperator() {
    return operator;
  }

  @Override
  public <R, X extends Exception> R accept(AcslTermVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(AcslAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return toParenthesizedASTString(pAAstNodeRepresentation);
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "("
        + operand.toParenthesizedASTString(pAAstNodeRepresentation)
        + operand.toParenthesizedASTString(pAAstNodeRepresentation)
        + ")";
  }

  @Override
  public int hashCode() {
    final int prime = 37;
    int result = 11;
    result = prime * result + super.hashCode();
    result = prime * result + Objects.hashCode(operand);
    result = prime * result + Objects.hashCode(operator);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslUnaryTerm other
        && super.equals(obj)
        && Objects.equals(other.operator, operator)
        && Objects.equals(other.operand, operand);
  }

  public enum AcslUnaryTermOperator implements AUnaryOperator, AcslAstNode {
    SIZEOF("sizeof"),
    PLUS("+"),
    MINUS("-"),
    POINTER_DEREFERENCE("*"),
    ADDRESS_OF("&"),
    NEGATION("!"),
    ;

    private final String operator;
    private final FileLocation fileLocation;

    AcslUnaryTermOperator(String pOperator) {
      operator = pOperator;
      fileLocation = FileLocation.DUMMY;
    }

    public static AcslUnaryTermOperator of(String pOperator) {
      for (AcslUnaryTermOperator op : values()) {
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
