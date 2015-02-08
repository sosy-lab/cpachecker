/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import org.sosy_lab.cpachecker.cpa.constraints.ConstraintVisitor;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.SymbolicValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryOrExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinarySymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryXorExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.CastExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.DivisionExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.EqualsExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LessThanExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LessThanOrEqualExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LogicalAndExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LogicalNotExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.LogicalOrExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ModuloExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.MultiplicationExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.PointerExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ShiftLeftExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ShiftRightExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.UnarySymbolicExpression;

/**
 * Checks whether a {@link Constraint} is trivial, that means that it does not contain any
 * variables with unknown concrete values.
 */
public class ConstraintTrivialityChecker implements ConstraintVisitor<Boolean>, SymbolicValueVisitor<Boolean> {

  private final ValueAnalysisState valueState;

  public ConstraintTrivialityChecker(ValueAnalysisState pValueState) {
    valueState = pValueState;
  }

  @Override
  public Boolean visit(SymbolicIdentifier pValue) {
    return valueState.hasKnownValue(pValue);
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
    return pExpression.getOperand1().accept(this)
      && pExpression.getOperand2().accept(this);
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
}
