// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Representation of the C function expression for builtin function popcount() (known as bitcount in
 * Java). Example: <code>
 * int a = nondet_uint(); int b = __builtin_popcount(a)</code>
 */
public final class PopcountFunctionExpression extends FunctionCallExpression {

  @Serial private static final long serialVersionUID = 1586324721114146414L;

  public PopcountFunctionExpression(
      final SymbolicExpression pArgument, final AFunctionCallExpression pFunctionCallExpr) {
    super(ImmutableList.of(pArgument), pFunctionCallExpr);
  }

  PopcountFunctionExpression(
      final SymbolicExpression pArgument,
      final AFunctionCallExpression pFunctionCallExpr,
      final MemoryLocation pRepresentedLocation) {
    super(ImmutableList.of(pArgument), pFunctionCallExpr, pRepresentedLocation);
  }

  PopcountFunctionExpression(
      final SymbolicExpression pArgument,
      final AFunctionCallExpression pFunctionCallExpr,
      final AbstractState pAbstractState) {
    super(ImmutableList.of(pArgument), pFunctionCallExpr, pAbstractState);
  }

  @Override
  public PopcountFunctionExpression copyForLocation(MemoryLocation pRepresentedLocation) {
    return new PopcountFunctionExpression(
        getArguments().getFirst(), getFunctionCallExpression(), pRepresentedLocation);
  }

  @Override
  public SymbolicExpression copyForState(AbstractState pCurrentState) {
    return new PopcountFunctionExpression(
        getArguments().getFirst(), getFunctionCallExpression(), pCurrentState);
  }

  @Override
  public <VisitorReturnT> VisitorReturnT accept(SymbolicValueVisitor<VisitorReturnT> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String getOperationString() {
    String suffix = "";
    CSimpleType argumentType = (CSimpleType) getArgumentTypes().getFirst();
    if (argumentType.hasLongLongSpecifier()) {
      suffix = "ll";
    } else if (argumentType.hasLongSpecifier()) {
      suffix = "l";
    } else {
      checkState(
          argumentType.getType().isIntegerType(),
          "C builtin function __builtin_popcount, __builtin_popcountl, and __builtin_popcountll may"
              + " only have input types unsigned integer, unsigned long int, and unsigned long long"
              + " int respectively. ");
    }
    return "__builtin_popcount" + suffix;
  }
}
