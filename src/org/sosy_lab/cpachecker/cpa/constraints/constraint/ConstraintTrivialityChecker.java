// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressOfExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.BinaryXorExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.CastExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.DivisionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.EqualsExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LessThanOrEqualExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalAndExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.LogicalOrExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ModuloExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.MultiplicationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.NegationExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.PointerExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftLeftExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ShiftRightExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SubtractionExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.UnarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * Checks whether a {@link Constraint} is trivial, that means that it does not contain any variables
 * with unknown concrete values.
 */
public class ConstraintTrivialityChecker implements SymbolicValueVisitor<Boolean> {

  @Override
  public Boolean visit(SymbolicIdentifier pValue) {
    return false;
  }

  @Override
  public Boolean visit(ConstantSymbolicExpression pExpression) {
    final Value value = pExpression.getValue();

    if (value instanceof SymbolicValue) {
      return ((SymbolicValue) value).accept(this);

    } else {
      assert value.isExplicitlyKnown();
      return true;
    }
  }

  private boolean isTrivialExpression(BinarySymbolicExpression pExpression) {
    return pExpression.getOperand1().accept(this) && pExpression.getOperand2().accept(this);
  }

  private boolean isTrivialExpression(UnarySymbolicExpression pExpression) {
    return pExpression.getOperand().accept(this);
  }

  @Override
  public Boolean visit(EqualsExpression pConstraint) {
    return isTrivialExpression(pConstraint);
  }

  @Override
  public Boolean visit(LogicalOrExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(LogicalAndExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(CastExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(PointerExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(AddressOfExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(LessThanExpression pConstraint) {
    return isTrivialExpression(pConstraint);
  }

  @Override
  public Boolean visit(LessThanOrEqualExpression pConstraint) {
    return isTrivialExpression(pConstraint);
  }

  @Override
  public Boolean visit(AdditionExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(SubtractionExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(MultiplicationExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(DivisionExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(ModuloExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(BinaryAndExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(BinaryNotExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(BinaryOrExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(BinaryXorExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(ShiftRightExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(ShiftLeftExpression pExpression) {
    return isTrivialExpression(pExpression);
  }

  @Override
  public Boolean visit(LogicalNotExpression pConstraint) {
    return isTrivialExpression(pConstraint);
  }

  @Override
  public Boolean visit(NegationExpression pExpression) {
    return isTrivialExpression(pExpression);
  }
}
