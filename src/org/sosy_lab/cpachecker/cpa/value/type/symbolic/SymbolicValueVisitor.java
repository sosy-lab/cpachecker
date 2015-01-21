/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.value.type.symbolic;

import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.AdditionExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryAndExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryNotExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.BinaryOrExpression;
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
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ShiftLeftExpression;
import org.sosy_lab.cpachecker.cpa.value.type.symbolic.expressions.ShiftRightExpression;

/**
 * Visitor for {@link SymbolicValue}s.
 *
 * @param T return type of visit methods
 */
public interface SymbolicValueVisitor<T> {

  T visit(SymbolicIdentifier pValue);

  T visit(ConstantSymbolicExpression pExpression);

  T visit(AdditionExpression pExpression);

  T visit(MultiplicationExpression pExpression);

  T visit(DivisionExpression pExpression);

  T visit(ModuloExpression pExpression);

  T visit(BinaryAndExpression pExpression);

  T visit(BinaryNotExpression pExpression);

  T visit(BinaryOrExpression pExpression);

  T visit(BinaryXorExpression pExpression);

  T visit(ShiftRightExpression pExpression);

  T visit(ShiftLeftExpression pExpression);

  T visit(LogicalNotExpression pExpression);

  T visit(LessThanOrEqualExpression pExpression);

  T visit(LessThanExpression pExpression);

  T visit(EqualsExpression pExpression);

  T visit(LogicalOrExpression pExpression);

  T visit(LogicalAndExpression pExpression);

  T visit(CastExpression pExpression);

}
